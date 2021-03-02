/*
 * Copyright 2021 HM Revenue & Customs
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

package audit

import com.google.inject.Inject
import models.registerWithoutId.RegisterWithoutIdResponse
import models.registerWithId.{RegisterWithIdResponse, UkAddress}
import play.api.http.Status
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpException, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class RegistrationAuditService @Inject()(auditService: AuditService) {

  def withIdIsUk(response: RegisterWithIdResponse): Option[Boolean] = {
    response.address match {
      case _: UkAddress => Some(true)
      case _ => Some(false)
    }
  }

  def sendRegisterWithIdAuditEvent(withId: Boolean, externalId: String, psaType: String, requestJson: JsValue)
                                  (implicit ec: ExecutionContext, request: RequestHeader): PartialFunction[Try[RegisterWithIdResponse], Unit] = {
    case Success(registerWithIdResponse) =>
      auditService.sendEvent(PSPRegistration(withId, externalId, psaType, found = true,
        withIdIsUk(registerWithIdResponse), Status.OK, requestJson, Some(Json.toJson(registerWithIdResponse))))

    case Failure(error: UpstreamErrorResponse) =>
      auditService.sendEvent(PSPRegistration(withId, externalId, psaType, found = true,
        None, error.statusCode, requestJson, None))

    case Failure(error: HttpException) =>
      auditService.sendEvent(PSPRegistration(withId, externalId, psaType, found = true,
        None, error.responseCode, requestJson, None))

  }

  def sendRegisterWithoutIdAuditEvent(withId: Boolean, externalId: String, psaType: String, requestJson: JsValue)
                                  (implicit ec: ExecutionContext, request: RequestHeader): PartialFunction[Try[RegisterWithoutIdResponse], Unit] = {
    case Success(registerWithoutIdResponse) =>
      auditService.sendEvent(PSPRegistration(withId, externalId, psaType, found = true,
        Some(false), Status.OK, requestJson, Some(Json.toJson(registerWithoutIdResponse))))

    case Failure(error: UpstreamErrorResponse) =>
      auditService.sendEvent(PSPRegistration(withId, externalId, psaType, found = true,
        None, error.statusCode, requestJson, None))

    case Failure(error: HttpException) =>
      auditService.sendEvent(PSPRegistration(withId, externalId, psaType, found = true,
        None, error.responseCode, requestJson, None))

  }
}

case class PSPRegistration(
                            withId: Boolean,
                            externalId: String,
                            psaType: String,
                            found: Boolean,
                            isUk: Option[Boolean],
                            status: Int,
                            request: JsValue,
                            response: Option[JsValue]
                          ) extends AuditEvent {

  override def auditType: String = if (withId) "PensionSchemePractitionerRegistration" else "PensionSchemePractitionerRegWithoutId"

  override def details: Map[String, String] =
    Map(
      "withId" -> withId.toString,
      "externalId" -> externalId,
      "pensionSchemeAdministratorType" -> psaType,
      "found" -> found.toString,
      "isUk" -> isUk.map(_.toString).getOrElse(""),
      "status" -> status.toString,
      "request" -> Json.prettyPrint(request),
      "response" -> {
        response match {
          case Some(json) => Json.prettyPrint(json)
          case _ => ""
        }
      }
    )
}

object PSPRegistration {
  implicit val formatsPSPRegistration: Format[PSPRegistration] = Json.format[PSPRegistration]
}
