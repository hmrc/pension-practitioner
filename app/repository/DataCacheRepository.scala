/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repository

import com.google.inject.Inject
import com.mongodb.client.model.FindOneAndUpdateOptions
import crypto.DataEncryptor
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.crypto.EncryptedValue
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneId}
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataCacheRepository @Inject()(
                                     mongoComponent: MongoComponent,
                                     config: Configuration,
                                     cipher: DataEncryptor
                                   )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[JsValue](
    collectionName = config.get[String]("mongodb.psp-cache.name"),
    mongoComponent = mongoComponent,
    domainFormat = implicitly,
    indexes = Seq(
      IndexModel(
        keys = Indexes.ascending("expireAt"),
        indexOptions = IndexOptions()
          .name("dataExpiry")
          .expireAfter(0, TimeUnit.SECONDS)
          .background(true)
      ),
      IndexModel(
        keys = Indexes.ascending("id"),
        indexOptions = IndexOptions()
          .name("id")
          .background(true)
          .unique(true)
      )

    )
  ) with Logging {

  private def selector(id: String) = Filters.equal("id", id)

  private def getExpireAt: Instant = {
    LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant
      .plus(config.get[Int]("mongodb.psp-cache.timeToLiveInDays") + 1, ChronoUnit.DAYS)
  }

  def save(id: String, userData: JsValue)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.debug("Calling save in PSP Cache")
    implicit val encryptedValueFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

    val modifier = Updates.combine(
      Updates.set("id", Codecs.toBson(id)),
      Updates.set("data", Codecs.toBson(cipher.encrypt(id, userData))),
      Updates.set("expireAt", getExpireAt)
    )

    collection.findOneAndUpdate(
      filter = selector(id),
      update = modifier,
      new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())
  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    logger.debug("Calling get in PSP Cache")

    collection.find(filter = selector(id))
      .toFuture().map(_.headOption).map { optJsVal =>
      optJsVal.flatMap { jsVal =>
       (jsVal \ "data").asOpt[JsValue].map { cipher.decrypt(id, _) }
      }
    }
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.warn(s"Removing row from collection DataCacheRepository at id:$id")

    collection.deleteOne(filter = selector(id))
      .toFuture().map(_ => ())
  }
}

object DataCacheRepository {
  implicit val dateFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
}
