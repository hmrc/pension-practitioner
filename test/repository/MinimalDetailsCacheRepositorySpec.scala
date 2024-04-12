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

package repository

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.{Format, JsString, JsValue, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class MinimalDetailsCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with EmbeddedMongoDBSupport with BeforeAndAfter with
  BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import MinimalDetailsCacheRepositorySpec._

  var minimalDetailsCacheRepository: MinimalDetailsCacheRepository = _

  override def beforeAll(): Unit = {
    when(mockConfig.get[String](ArgumentMatchers.eq("mongodb.minimal-detail.name"))(ArgumentMatchers.any()))
      .thenReturn("minimal-detail")
    when(mockConfig.get[Int](ArgumentMatchers.eq("mongodb.minimal-detail.timeToLiveInSeconds"))(ArgumentMatchers.any()))
      .thenReturn(3600)
    initMongoDExecutable()
    startMongoD()
    minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

    super.beforeAll()
  }

  override def afterAll(): Unit =
    stopMongoD()

  "upsert" must {
    "save new data into the cache" in {

      val filters = Filters.eq(idField, id)

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(id, userData)
        documentsInDB <- minimalDetailsCacheRepository.collection.find(filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }

    "update data when already exists in the cache and then get the value" in {

      val result = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(id, userData)
        _ <- minimalDetailsCacheRepository.upsert(id, dummyData)
        dataRetrieved <- minimalDetailsCacheRepository.get(id = id)
      } yield dataRetrieved

      whenReady(result) {
        _ mustBe Some(dummyData)
      }
    }
  }

  "get" must {
    "get no data if value does not exist" in {

      val result = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        resultOfGet <- minimalDetailsCacheRepository.get(id = id)
      } yield resultOfGet

      whenReady(result) {
        _ mustBe None
      }
    }
  }

  "remove" must {
    "remove record at given id" in {

      val saveAndRemoveData = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(id, dummyData)
        _ <- minimalDetailsCacheRepository.remove(id)
        dataRetrieved <- minimalDetailsCacheRepository.get(id = id)
      } yield dataRetrieved

      whenReady(saveAndRemoveData) {
        _ mustBe None
      }
    }
  }
}

object MinimalDetailsCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  implicit val dateFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private val id: String = "testId"
  private val idField: String = "id"
  private val userData: JsValue = Json.obj("testing" -> "123")

  private val mockConfig = mock[Configuration]
  private val dummyData = JsString("dummy data")

  private def buildFormRepository(mongoHost: String, mongoPort: Int) = {
    val databaseName = "pension-administrator"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new MinimalDetailsCacheRepository(MongoComponent(mongoUri), mockConfig)
  }
}


