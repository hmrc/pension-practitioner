/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.{AuditService, EmailAuditEvent}
import com.google.inject.Inject
import models.enumeration.JourneyType
import models.{EmailEvents, Opened}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

class EmailResponseController @Inject()(
                                         auditService: AuditService,
                                         cc: ControllerComponents,
                                         crypto: ApplicationCrypto,
                                         parser: PlayBodyParsers,
                                         val authConnector: AuthConnector
                                       ) extends BackendController(cc) with AuthorisedFunctions {

  def retrieveStatus(journeyType: JourneyType.Name, requestId: String, email: String, encryptedPsaId: String): Action[JsValue] = Action(parser.tolerantJson) {
    implicit request =>
      validatePsaIdEmail(encryptedPsaId, email) match {
        case Right(Tuple2(psaId, emailAddress)) =>
          request.body.validate[EmailEvents].fold(
            _ => BadRequest("Bad request received for email call back event"),
            valid => {
              valid.events.filterNot(
                _.event == Opened
              ).foreach { event =>
                Logger.debug(s"Email Audit event coming from $journeyType is $event")
                auditService.sendEvent(EmailAuditEvent(psaId, emailAddress, event.event, journeyType, requestId))
              }
              Ok
            }
          )

        case Left(result) => result
      }
  }

  private def validatePsaIdEmail(encryptedPsaId: String, email: String): Either[Result, (PsaId, String)] = {
    val psaId = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPsaId)).value
    val emailAddress = crypto.QueryParameterCrypto.decrypt(Crypted(email)).value
    val emailRegex: String = "^(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"" +
      "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")" +
      "@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|" +
      "\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-zA-Z0-9-]*[a-zA-Z0-9]:" +
      "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$"

    try {
      require(emailAddress.matches(emailRegex))
      Right(Tuple2(PsaId(psaId), emailAddress))
    } catch {
      case _: IllegalArgumentException => Left(Forbidden(s"Malformed PSAID : $psaId or Email : $emailAddress"))
    }
  }
}
