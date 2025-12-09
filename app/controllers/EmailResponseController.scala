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

package controllers

import audit.*
import com.google.inject.Inject
import models.enumeration.JourneyType
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.*
import services.JsonCryptoService
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class EmailResponseController @Inject()(
                                         val auditService: AuditService,
                                         cc: ControllerComponents,
                                         jsonCrypto: JsonCryptoService,
                                         parser: PlayBodyParsers,
                                         val authConnector: AuthConnector
                                       )
                                       (implicit val ec: ExecutionContext)
  extends BackendController(cc) with AuditEmailStatus {

  override protected val logger: Logger = Logger(classOf[EmailResponseController])
  override protected val crypto: Encrypter & Decrypter = jsonCrypto.jsonCrypto

  def retrieveStatus(journeyType: JourneyType.Name,
                     requestId: String,
                     email: String,
                     encryptedPspId: String): Action[JsValue] =
    Action(parser.tolerantJson) {
      implicit request =>
        logger.warn("json encrypted psp & emailAddress email status parameters")
        auditEmailStatus(encryptedPspId, email, Some(journeyType), Some(requestId))
    }

  def retrieveStatusForPSPDeregistration(
                                          encryptedPspId: String,
                                          encryptedEmail: String
                                        ): Action[JsValue] =
    Action(parser.tolerantJson) {
      implicit request =>
        logger.warn("json encrypted PSPDeregistration psp & emailAddress email status parameters")
        auditEmailStatus(encryptedPspId, encryptedEmail)
    }
}
