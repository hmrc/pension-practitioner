/*
 * Copyright 2025 HM Revenue & Customs
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

import audit.{AuditService, EmailAuditEvent, PSPDeregistrationEmailAuditEvent}
import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*
import models.{EmailEvents, EmailIdentifierErrors, EmailIdentifiers, Opened}
import models.EmailIdentifierErrors.{EmailMalformed, PspIdMalformed}
import models.enumeration.JourneyType
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Results.{BadRequest, Forbidden, Ok}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter}
import uk.gov.hmrc.domain.PspId

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait AuditEmailStatus {

  implicit val ec: ExecutionContext
  protected val logger: Logger
  protected val auditService: AuditService
  protected val crypto: Encrypter & Decrypter

  private type ValidationResult[A] = ValidatedNec[EmailIdentifierErrors, A]

  private val emailRegex: String = "^(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"" +
    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")" +
    "@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|" +
    "\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-zA-Z0-9-]*[a-zA-Z0-9]:" +
    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$"

  private def validatePsaId(encryptedPspId: String): ValidationResult[PspId] =
    Try(PspId(crypto.decrypt(Crypted(encryptedPspId)).value)) match {
      case Failure(_) => PspIdMalformed.invalidNec
      case Success(pspId) => pspId.validNec
    }

  private def validateEmail(encryptedEmail: String): ValidationResult[String] = {
    val decryptedEmail = crypto.decrypt(Crypted(encryptedEmail)).value
    if (decryptedEmail.matches(emailRegex)) {
      decryptedEmail.validNec
    } else {
      EmailMalformed.invalidNec
    }
  }

  private def validateParameters(encryptedPsaId: String, encryptedEmail: String): ValidationResult[EmailIdentifiers] =
    (
      validatePsaId(encryptedPsaId),
      validateEmail(encryptedEmail)
    )
      .mapN(EmailIdentifiers.apply)

  protected def auditEmailStatus(encryptedPspId: String,
                        encryptedEmail: String,
                        maybeJourneyType: Option[JourneyType.Name] = None,
                        maybeRequestId: Option[String] = None
                       )(implicit request: Request[JsValue]): Result =
    validateParameters(encryptedPspId, encryptedEmail) match {
      case Validated.Valid(emailIdentifiers) =>
        request.body.validate[EmailEvents].fold(
          _ => BadRequest("Bad request received for email call back event"),
          valid => {
            valid.events.filterNot(
              _.event == Opened
            ).foreach { event =>
              (maybeJourneyType, maybeRequestId) match {
                case (Some(journeyType), Some(requestId)) =>
                  logger.debug(s"Email Audit event coming from $journeyType is $event")
                  auditService.sendEvent(EmailAuditEvent(emailIdentifiers.pspId.id, emailIdentifiers.emailAddress, event.event, journeyType, requestId))
                case _ =>
                  logger.debug(s"Email Audit event is $event")
                  auditService.sendEvent(PSPDeregistrationEmailAuditEvent(emailIdentifiers.pspId.id, emailIdentifiers.emailAddress, event.event))
              }
            }
            Ok
          }
        )
      case Validated.Invalid(e) =>
        Forbidden(e.toList.map(_.value).mkString(" & "))
    }
    
}
