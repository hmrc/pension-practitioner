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

import com.github.simplyscala.MongoEmbedDatabase
import org.joda.time.DateTime
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.{Format, JsString, JsValue, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class DataCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  import DataCacheRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockConfig.get[String](ArgumentMatchers.eq("mongodb.psp-cache.name"))(ArgumentMatchers.any()))
      .thenReturn("psp-journey")
  }

  withEmbedMongoFixture(port = 24680) { _ =>
    "save" must {
      "save new data into the cache" in {
        mongoCollectionDrop()

        val filters = Filters.eq(idField, id)

        val documentsInDB = for {
          _ <- dataCacheRepository.save(id, userData)
          documentsInDB <- dataCacheRepository.collection.find(filters).toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.size mustBe 1
        }
      }

      "update data when already exists in the cache and then get the value" in {
        mongoCollectionDrop()

        val result = for {
          _ <- dataCacheRepository.save(id, userData)
          _ <- dataCacheRepository.save(id, dummyData)
          dataRetrieved <- dataCacheRepository.get(id = id)
        } yield dataRetrieved

        whenReady(result) {
          _ mustBe Some(dummyData)
        }
      }
    }
  }

  "get" must {
    "get no data if value does not exist" in {

      mongoCollectionDrop()

      val result = for {
        resultOfGet <- dataCacheRepository.get(id = id)
      } yield resultOfGet

      whenReady(result) {
        _ mustBe None
      }

    }
    //
    //    "remove" must {
    //      "delete an existing DataEntryWithoutEncryption session data cache record by id in Mongo collection when encrypted false" in {
    //
    //        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
    //        mongoCollectionDrop()
    //
    //        val record = ("id-1", Json.parse("""{"data":"1"}"""))
    //
    //        val documentsInDB = for {
    //          _ <- dataCacheRepository.upsert(record._1, record._2)
    //          documentsInDB <- dataCacheRepository.get(record._1)
    //        } yield documentsInDB
    //
    //        whenReady(documentsInDB) { documentsInDB =>
    //          documentsInDB.isDefined mustBe true
    //        }
    //
    //        val documentsInDB2 = for {
    //          _ <- dataCacheRepository.remove(record._1)
    //          documentsInDB2 <- dataCacheRepository.get(record._1)
    //        } yield documentsInDB2
    //
    //        whenReady(documentsInDB2) { documentsInDB2 =>
    //          documentsInDB2.isDefined mustBe false
    //        }
    //      }
    //
    //      "not delete an existing DataEntryWithoutEncryption session data cache record by id in Mongo collection when encrypted false and id incorrect" in {
    //
    //        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
    //        mongoCollectionDrop()
    //
    //        val record = ("id-1", Json.parse("""{"data":"1"}"""))
    //        val documentsInDB = for {
    //          _ <- dataCacheRepository.upsert(record._1, record._2)
    //          documentsInDB <- dataCacheRepository.get(record._1)
    //        } yield documentsInDB
    //
    //        whenReady(documentsInDB) { documentsInDB =>
    //          documentsInDB.isDefined mustBe true
    //        }
    //
    //        val documentsInDB2 = for {
    //          _ <- dataCacheRepository.remove("2")
    //          documentsInDB2 <- dataCacheRepository.get(record._1)
    //        } yield documentsInDB2
    //
    //        whenReady(documentsInDB2) { documentsInDB2 =>
    //          documentsInDB2.isDefined mustBe true
    //        }
    //      }
    //
    //      "delete an existing EncryptedDataEntry session data cache record by id in Mongo collection when encrypted true" in {
    //
    //        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
    //        mongoCollectionDrop()
    //
    //        val record = ("id-1", Json.parse("""{"data":"1"}"""))
    //        val documentsInDB = for {
    //          _ <- dataCacheRepository.upsert(record._1, record._2)
    //          documentsInDB <- dataCacheRepository.get(record._1)
    //        } yield documentsInDB
    //
    //        whenReady(documentsInDB) { documentsInDB =>
    //          documentsInDB.isDefined mustBe true
    //        }
    //
    //        val documentsInDB2 = for {
    //          _ <- dataCacheRepository.remove(record._1)
    //          documentsInDB2 <- dataCacheRepository.get(record._1)
    //        } yield documentsInDB2
    //
    //
    //        whenReady(documentsInDB2) { documentsInDB2 =>
    //          documentsInDB2.isDefined mustBe false
    //        }
    //      }
    //
    //      "not delete an existing EncryptedDataEntry session data cache record by id in Mongo collection when encrypted true" in {
    //
    //        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
    //        mongoCollectionDrop()
    //
    //        val record = ("id-1", Json.parse("""{"data":"1"}"""))
    //        val documentsInDB = for {
    //          _ <- dataCacheRepository.upsert(record._1, record._2)
    //          documentsInDB <- dataCacheRepository.get(record._1)
    //        } yield documentsInDB
    //
    //        whenReady(documentsInDB) { documentsInDB =>
    //          documentsInDB.isDefined mustBe true
    //        }
    //
    //        val documentsInDB2 = for {
    //          _ <- dataCacheRepository.remove("2")
    //          documentsInDB2 <- dataCacheRepository.get(record._1)
    //        } yield documentsInDB2
    //
    //        whenReady(documentsInDB2) { documentsInDB2 =>
    //          documentsInDB2.isDefined mustBe true
    //        }
    //      }
    //    }
    //  }
  }

}

object DataCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat

  private val id: String = "testId"
  private val idField: String = "id"
  private val userData: JsValue = Json.obj("testing" -> "123")

  private val mockConfig = mock[Configuration]
  private val databaseName = "pension-administrator"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)
  private val dummyData = JsString("dummy data")

  private def mongoCollectionDrop(): Void = Await
    .result(dataCacheRepository.collection.drop().toFuture(), Duration.Inf)

  def dataCacheRepository: DataCacheRepository = new DataCacheRepository(mongoComponent, mockConfig)
}
