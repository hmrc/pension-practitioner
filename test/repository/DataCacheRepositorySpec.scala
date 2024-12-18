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

import crypto.DataEncryptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.libs.json.{Format, JsString, JsValue, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.LocalMongoDB

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class DataCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with LocalMongoDB with BeforeAndAfter with
  BeforeAndAfterAll with ScalaFutures with GuiceOneAppPerSuite { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  var dataCacheRepository: DataCacheRepository = _

  implicit val dateFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private val id: String = "testId"
  private val idField: String = "id"
  private val userData: JsValue = Json.obj("testing" -> "123")

  private val mockConfig = mock[Configuration]
  private val dummyData = JsString("dummy data")

  private def buildFormRepository(mongoHost: String, mongoPort: Int) = {
    val databaseName = "pension-practitioner"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new DataCacheRepository(MongoComponent(mongoUri), mockConfig, app.injector.instanceOf[DataEncryptor])
  }

  override def beforeAll(): Unit = {
    when(mockConfig.get[String](ArgumentMatchers.eq("mongodb.psp-cache.name"))(ArgumentMatchers.any()))
      .thenReturn("psp-journey")
    dataCacheRepository = buildFormRepository(mongoHost, mongoPort)
    super.beforeAll()
  }

  "save" must {
    "save new data into the cache" in {

      val filters = Filters.eq(idField, id)

      val documentsInDB = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        _ <- dataCacheRepository.save(id, userData)
        documentsInDB <- dataCacheRepository.collection.find(filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }

    "update data when already exists in the cache and then get the value" in {

      val result = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        _ <- dataCacheRepository.save(id, userData)
        _ <- dataCacheRepository.save(id, dummyData)
        dataRetrieved <- dataCacheRepository.get(id = id)
      } yield dataRetrieved

      whenReady(result) {
        _ mustBe Some(dummyData)
      }
    }
  }

  "get" must {
    "get no data if value does not exist" in {

      val result = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        resultOfGet <- dataCacheRepository.get(id = id)
      } yield resultOfGet

      whenReady(result) {
        _ mustBe None
      }
    }
  }

  "remove" must {
    "remove record at given id" in {

      val saveAndRemoveData = for {
        _ <- dataCacheRepository.collection.drop().toFuture()
        _ <- dataCacheRepository.save(id, dummyData)
        _ <- dataCacheRepository.remove(id)
        dataRetrieved <- dataCacheRepository.get(id = id)
      } yield dataRetrieved

      whenReady(saveAndRemoveData) {
        _ mustBe None
      }
    }
  }
}
