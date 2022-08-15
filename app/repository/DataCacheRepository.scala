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
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

class DataCacheRepository @Inject()(
                                     mongoComponent: MongoComponent,
                                     config: Configuration
                                   )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[JsValue](
    collectionName = config.get[String]("mongodb.psp-cache.name"),
    mongoComponent = mongoComponent,
    domainFormat = implicitly,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("expireAt"),
        IndexOptions().name("dataExpiry").expireAfter(0, TimeUnit.SECONDS).background(true)
      ),
      IndexModel(
        Indexes.ascending("id"),
        IndexOptions().name("id").unique(true).background(true)
      )
    )
  ) with Logging {

  import DataCacheRepository._

  private def selector(id: String) = Filters.equal("id", id)

  private def getExpireAt: DateTime = {
    DateTime
      .now(DateTimeZone.UTC)
      .toLocalDate
      .plusDays(config.get[Int]("mongodb.psp-cache.timeToLiveInDays") + 1)
      .toDateTimeAtStartOfDay()
  }

  def save(id: String, userData: JsValue)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.debug("Calling save in PSP Cache")

    val modifier = Updates.combine(
      Updates.set("id", Codecs.toBson(id)),
      Updates.set("data", Codecs.toBson(userData)),
      Updates.set("expireAt", Codecs.toBson(getExpireAt))
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
        (jsVal \ "data").asOpt[JsValue]
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
  implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
}
