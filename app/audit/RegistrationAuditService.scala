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
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{Format, Json, JsValue}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{UpstreamErrorResponse, HttpException}

import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

class RegistrationAuditService @Inject()(auditService: AuditService) {

  private val logger = Logger(classOf[RegistrationAuditService])

  def withIdIsUk(response: RegisterWithIdResponse): Option[Boolean] = {
    response.address match {
      case _: UkAddress => Some(true)
      case _ => Some(false)
    }
  }

  // scalastyle:off method.length
  def sendRegisterWithoutIdAuditEvent(
                                    externalId: String,
                                    psaType: String,
                                    requestJson: JsValue
                                  )(
                                    implicit ec: ExecutionContext,
                                    request: RequestHeader
                                  ): PartialFunction[Try[Either[HttpException, RegisterWithoutIdResponse]], Unit] = {
    case Success(Right(registerWithoutIdResponse)) =>
      auditService.sendEvent(
        PSPRegistration(
          withId = false,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = Some(false),
          status = Status.OK,
          request = requestJson,
          response = Some(Json.toJson(registerWithoutIdResponse))
        )
      )
    case Success(Left(error)) =>
      auditService.sendEvent(
        PSPRegistration(
          withId = false,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = None,
          status = error.responseCode,
          request = requestJson,
          response = None
        )
      )
    case Failure(error: UpstreamErrorResponse) =>
      auditService.sendEvent(
        PSPRegistration(
          withId = false,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = None,
          status = error.statusCode,
          request = requestJson,
          response = None
        )
      )

    case Failure(error: HttpException) =>
      auditService.sendEvent(
        PSPRegistration(
          withId = false,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = None,
          status = error.responseCode,
          request = requestJson,
          response = None
        )
      )
  }

  // scalastyle:off method.length
  def sendRegisterWithIdAuditEvent(
    externalId: String,
    psaType: String,
    requestJson: JsValue
  )(
    implicit ec: ExecutionContext,
    request: RequestHeader
  ): PartialFunction[Try[Either[HttpException, RegisterWithIdResponse]], Unit] = {
    case Success(Right(registerWithIdResponse)) =>
      auditService.sendEvent(
        PSPRegistration(
          withId = true,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = withIdIsUk(registerWithIdResponse),
          status = Status.OK,
          request = requestJson,
          response = Some(Json.toJson(registerWithIdResponse))
        )
      )
    case Success(Left(error)) =>
      auditService.sendEvent(
        PSPRegistration(
          withId = true,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = None,
          status = error.responseCode,
          request = requestJson,
          response = None
        )
      )

    case Failure(error: UpstreamErrorResponse) =>
      auditService.sendEvent(
        PSPRegistration(
          withId = true,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = None,
          status = error.statusCode,
          request = requestJson,
          response = None
        )
      )

    case Failure(error: HttpException) =>
      auditService.sendEvent(
        PSPRegistration(
          withId = true,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = None,
          status = error.responseCode,
          request = requestJson,
          response = None
        )
      )
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
