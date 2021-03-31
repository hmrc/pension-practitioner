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

package connectors

import audit.RegistrationAuditService
import com.google.inject.Inject
import config.AppConfig
import models.registerWithId.RegisterWithIdResponse
import models.registerWithoutId.{OrganisationRegistrant, RegisterWithoutIdIndividualRequest, RegisterWithoutIdResponse}
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpClient, _}
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

class RegistrationConnector @Inject()(
                                       http: HttpClient,
                                       config: AppConfig,
                                       headerUtils: HeaderUtils,
                                       registrationAuditService: RegistrationAuditService
                                     )
  extends HttpResponseHelper {

  private def desHeaderCarrier: HeaderCarrier =
    HeaderCarrier(extraHeaders = headerUtils.desHeaderWithoutCorrelationId)

  def registerWithIdIndividual(
                                externalId: String,
                                nino: String,
                                registerData: JsValue
                              )(
                                implicit headerCarrier: HeaderCarrier,
                                ec: ExecutionContext,
                                request: RequestHeader
                              ): Future[RegisterWithIdResponse] =
    http.POST[JsValue, RegisterWithIdResponse](
      url = config.registerWithIdIndividualUrl.format(nino),
      body = registerData
    )(
      wts = implicitly,
      rds = implicitly,
      hc = desHeaderCarrier,
      ec = implicitly
    ) andThen
      registrationAuditService.sendRegisterWithIdAuditEvent(
        withId = true,
        externalId,
        psaType = "Individual",
        requestJson = registerData
      )

  def registerWithIdOrganisation(
                                  externalId: String,
                                  utr: String,
                                  registerData: JsValue
                                )(
                                  implicit headerCarrier: HeaderCarrier,
                                  ec: ExecutionContext,
                                  request: RequestHeader
                                ): Future[Either[HttpException, RegisterWithIdResponse]] = {
    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)

    val organisationPsaType: String =
      (registerData \ "organisation" \ "organisationType")
        .validate[String]
        .fold(_ => "Unknown", organisationType => organisationType)

    http.POST[JsValue, HttpResponse](
      url = registerWithIdUrl,
      body = registerData
    )(
      wts = implicitly,
      rds = implicitly[HttpReads[HttpResponse]],
      hc = desHeaderCarrier,
      ec = implicitly
    ) map {
      response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[RegisterWithIdResponse] match {
              case JsSuccess(value, _) => Right(value)
              case JsError(errors) => throw JsResultException(errors)
            }
          case _ =>
            Left(handleErrorResponse("POST", registerWithIdUrl)(response))
        }
    } andThen
      registrationAuditService.sendRegisterWithIdOrgAuditEvent(
        withId = true,
        externalId = externalId,
        psaType = organisationPsaType,
        requestJson = registerData
      )
  }

  def registrationNoIdIndividual(
                                  externalId: String,
                                  registerData: RegisterWithoutIdIndividualRequest
                                )(
                                  implicit headerCarrier: HeaderCarrier,
                                  ec: ExecutionContext,
                                  request: RequestHeader
                                ): Future[RegisterWithoutIdResponse] = {
    val url = config.registerWithoutIdIndividualUrl
    val correlationId = headerUtils.getCorrelationId(headerCarrier.requestId.map(_.value))
    val registerWithNoIdData =
      Json.toJson(registerData)(
        RegisterWithoutIdIndividualRequest.writesRegistrationNoIdIndividualRequest(correlationId)
      )

    http.POST[JsValue, RegisterWithoutIdResponse](
      url = url,
      body = registerWithNoIdData
    )(
      wts = implicitly,
      rds = implicitly,
      hc = desHeaderCarrier,
      ec = implicitly
    ) andThen
      registrationAuditService.sendRegisterWithoutIdAuditEvent(
        withId = false,
        externalId = externalId,
        psaType = "Individual",
        requestJson = registerWithNoIdData
      )
  }

  def registrationNoIdOrganisation(
                                    externalId: String,
                                    registerData: OrganisationRegistrant
                                  )(
                                    implicit headerCarrier: HeaderCarrier,
                                    ec: ExecutionContext,
                                    request: RequestHeader
                                  ): Future[RegisterWithoutIdResponse] = {
    val url = config.registerWithoutIdOrganisationUrl
    val correlationId = headerUtils.getCorrelationId(headerCarrier.requestId.map(_.value))
    val registerWithNoIdData =
      Json.toJson(registerData)(
        OrganisationRegistrant.writesOrganisationRegistrantRequest(correlationId)
      )

    http.POST[JsValue, RegisterWithoutIdResponse](
      url = url,
      body = registerWithNoIdData
    )(
      wts = implicitly,
      rds = implicitly,
      hc = desHeaderCarrier,
      ec = implicitly
    ) andThen
      registrationAuditService.sendRegisterWithoutIdAuditEvent(
        withId = false,
        externalId = externalId,
        psaType = "Organisation",
        requestJson = registerWithNoIdData
      )
  }
}
