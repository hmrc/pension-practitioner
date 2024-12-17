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

package service

import com.google.inject.Inject
import crypto.DataEncryptorImpl
import org.mongodb.scala.MongoCollection
import play.api.libs.json.{JsObject, JsValue, Json, OFormat}
import play.api.{Configuration, Logging}
import repository.{DataCacheRepository, MinimalDetailsCacheRepository}
import uk.gov.hmrc.crypto.EncryptedValue
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class MigrationService @Inject()(mongoLockRepository: MongoLockRepository,
                                 dataCacheRepository: DataCacheRepository,
                                 minimalDetailsCacheRepository: MinimalDetailsCacheRepository,
                                 configuration: Configuration)(implicit ec: ExecutionContext) extends Logging {
  private val lock = LockService(mongoLockRepository, "pension_practitioner_mongodb_migration_lock", Duration(10, TimeUnit.MINUTES))
  private val cipher = new DataEncryptorImpl(Some(configuration.get[String]("mongodb.migration.encryptionKey")))
  private def encryptCollections() = {
    implicit val encryptedValueFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

    logger.warn("[PODS-9952] Started encrypting collection")
    def encryptCollection(collection: MongoCollection[JsValue], collectionName: String, idAndDataToSave: (String, JsValue) => Future[Unit]) = {
      collection.find().toFuture().map(seqJsValue => {
        logger.warn(s"[PODS-9952] Number of documents found for $collection: ${seqJsValue.length}")

        val newEncryptedValues = seqJsValue.flatMap { jsValue =>
          val data = jsValue \ "data"
          val alreadyEncrypted = data.validate[EncryptedValue].fold(_ => false, _ => true)
          if(alreadyEncrypted) {
            None
          } else {
            val encryptedData = Json.toJson(cipher.encrypt((jsValue \ "id").as[String], data.as[JsValue]))
            val encryptedJsValue = (jsValue.as[JsObject] - "data") + ("data" -> encryptedData)
            Some(encryptedJsValue)
          }
        }

        val numberOfNewEncryptedValues = newEncryptedValues.length

        logger.warn(s"[PODS-9952] Number of documents encrypted for $collectionName: $numberOfNewEncryptedValues")

        val successfulInserts = newEncryptedValues.map { jsValue =>
          val id = (jsValue \ "id").as[String]
          val data = (jsValue \ "data").as[JsValue]
          Try(Await.result(idAndDataToSave(id, data), 40.seconds)) match {
            case Failure(exception) =>
              logger.error(s"[PODS-9952] upsert failed", exception)
              false
            case Success(value) =>
              true
          }
        }.count(_ == true)

        logger.warn(s"[PODS-9952] Number of documents upserted for $collectionName: $successfulInserts")

        numberOfNewEncryptedValues -> successfulInserts
      })
    }

    Future.sequence(Seq(
      encryptCollection(dataCacheRepository.collection, "Data Cache", dataCacheRepository.save ),
      encryptCollection(minimalDetailsCacheRepository.collection, "Minimal Details", minimalDetailsCacheRepository.upsert)
    ))

  }

  private def decryptCollections() = {
    implicit val encryptedValueFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

    logger.warn("[PODS-9952] Started decrypting collection")
    def decryptCollection(collection: MongoCollection[JsValue], collectionName: String, idAndDataToSave: (String, JsValue) => Future[Unit]) = {
      logger.warn(s"[PODS-9952] Started decryption of collection: $collectionName")
      collection.find().toFuture().map(seqJsValue => {
        logger.warn(s"[PODS-9952] Number of documents found for $collectionName: ${seqJsValue.length}")
        val newDecryptedValues = seqJsValue.flatMap { jsValue =>
          val data = jsValue \ "data"
          val alreadyEncrypted = data.validate[EncryptedValue].fold(_ => false, _ => true)
          if(alreadyEncrypted) {
            val decryptedData = cipher.decrypt((jsValue \ "id").as[String], Json.toJson(data.as[EncryptedValue]))
            val decryptedJsValue = (jsValue.as[JsObject] - "data") + ("data" -> decryptedData)
            Some(decryptedJsValue)
          } else {
            None
          }
        }

        val numberOfNewDecryptedValues = newDecryptedValues.length

        logger.warn(s"[PODS-9952] Number of documents decrypted for $collectionName: $numberOfNewDecryptedValues")

        val successfulInserts = newDecryptedValues.map { jsValue =>
          val id = (jsValue \ "id").as[String]
          val data = (jsValue \ "data").as[JsValue]
          println(data)
          Try(Await.result(idAndDataToSave(id, data), 40.seconds)) match {
            case Failure(exception) =>
              logger.error(s"[PODS-9952] upsert failed", exception)
              false
            case Success(value) =>
              true
          }
        }.count(_ == true)

        logger.warn(s"[PODS-9952] Number of documents upserted for $collectionName: $successfulInserts")

        numberOfNewDecryptedValues -> successfulInserts
      })
    }

    Future.sequence(Seq(
      decryptCollection(dataCacheRepository.collection, "Data Cache", dataCacheRepository.save ),
      decryptCollection(minimalDetailsCacheRepository.collection, "Minimal Details", minimalDetailsCacheRepository.upsert)
    ))

  }


  lock withLock {
    for {
      res <- if(configuration.get[Boolean]("mongodb.migration.encrypt")) encryptCollections() else decryptCollections()
    } yield res
  } map {
    case Some(seq) =>
      logger.warn(s"[PODS-9952] collection modified successfully. Total documents: ${seq.map(_._1).sum}. Documents updated: ${seq.map(_._2).sum}")
    case None => logger.warn(s"[PODS-9952] locked by other instance")
  } recover {
    case e => logger.error("Locking finished with error", e)
  }
}
