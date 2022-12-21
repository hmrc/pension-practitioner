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
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MinimalDetailsCacheRepository @Inject()(
                                               mongoComponent: MongoComponent,
                                               config: Configuration
                                             )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[JsValue](
    collectionName = config.get[String]("mongodb.minimal-detail.name"),
    mongoComponent = mongoComponent,
    domainFormat = implicitly,
    indexes = Seq(
      IndexModel(
        keys = Indexes.ascending("lastUpdated"),
        indexOptions = IndexOptions()
          .name("dataExpiry")
          .expireAfter(config.get[Int](path = "mongodb.minimal-detail.timeToLiveInSeconds"), TimeUnit.SECONDS)
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

  import MinimalDetailsCacheRepository._

  private def selector(id: String) = Filters.equal("id", id)

  def upsert(id: String, data: JsValue)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.debug("Calling save in PSP Minimal Details Cache")

    val modifier = Updates.combine(
      Updates.set("id", Codecs.toBson(id)),
      Updates.set("data", Codecs.toBson(data)),
      Updates.set("lastUpdated", Codecs.toBson(DateTime.now(DateTimeZone.UTC)))
    )

    collection.findOneAndUpdate(
      filter = selector(id),
      update = modifier,
      new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())
  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    logger.debug("Calling get in PSP Minimal Details Cache")

    collection.find(filter = selector(id))
      .toFuture().map(_.headOption).map { optJsVal =>
      optJsVal.flatMap { jsVal =>
        (jsVal \ "data").asOpt[JsValue]
      }
    }
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.warn(s"Removing row from collection MinimalDetailsCacheRepository at id:$id")

    collection.deleteOne(filter = selector(id))
      .toFuture().map(_ => ())
  }
}

object MinimalDetailsCacheRepository {
  implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
}
