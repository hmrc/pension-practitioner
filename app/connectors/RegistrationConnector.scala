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
import models.registerWithoutId.{RegisterWithoutIdResponse, RegisterWithoutIdIndividualRequest, OrganisationRegistrant}
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpClient, _}
import utils.HttpResponseHelper
import uk.gov.hmrc.http.HttpReads.Implicits._

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
                              ): Future[Either[HttpException, RegisterWithIdResponse]] = {
    val url = config.registerWithIdIndividualUrl.format(nino)
    http.POST[JsValue, HttpResponse](
      url = url,
      body = registerData
    )(
      wts = implicitly,
      rds = implicitly,
      hc = desHeaderCarrier,
      ec = implicitly
    ) map(response => handleHttpResponseWithIdForErrors(response= response, url = url)) andThen
      registrationAuditService.sendRegisterWithIdAuditEvent(
        externalId = externalId,
        psaType = "Individual",
        requestJson = registerData
      )
  }

  def registerWithIdOrganisation(
                                  externalId: String,
                                  utr: String,
                                  registerData: JsValue
                                )(
                                  implicit headerCarrier: HeaderCarrier,
                                  ec: ExecutionContext,
                                  request: RequestHeader
                                ): Future[Either[HttpException, RegisterWithIdResponse]] = {
    val url = config.registerWithIdOrganisationUrl.format(utr)

    val organisationPsaType: String =
      (registerData \ "organisation" \ "organisationType")
        .validate[String]
        .fold(_ => "Unknown", organisationType => organisationType)

    http.POST[JsValue, HttpResponse](
      url = url,
      body = registerData
    )(
      wts = implicitly,
      rds = implicitly[HttpReads[HttpResponse]],
      hc = desHeaderCarrier,
      ec = implicitly
    )  map(response => handleHttpResponseWithIdForErrors(response= response, url = url)) andThen
      registrationAuditService.sendRegisterWithIdAuditEvent(
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
                                ): Future[Either[HttpException, RegisterWithoutIdResponse]] = {
    val url = config.registerWithoutIdIndividualUrl
    val correlationId = headerUtils.getCorrelationId
    val registerWithNoIdData =
      Json.toJson(registerData)(
        RegisterWithoutIdIndividualRequest.writesRegistrationNoIdIndividualRequest(correlationId)
      )

    http.POST[JsValue, HttpResponse](
      url = url,
      body = registerWithNoIdData
    )(
      wts = implicitly,
      rds = implicitly,
      hc = desHeaderCarrier,
      ec = implicitly
    ) map(response => handleHttpResponseWithoutIdForErrors(response= response, url = url)) andThen
      registrationAuditService.sendRegisterWithoutIdAuditEvent(
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
                                  ): Future[Either[HttpException, RegisterWithoutIdResponse]] = {
    val url = config.registerWithoutIdOrganisationUrl
    val correlationId = headerUtils.getCorrelationId
    val registerWithNoIdData =
      Json.toJson(registerData)(
        OrganisationRegistrant.writesOrganisationRegistrantRequest(correlationId)
      )

    http.POST[JsValue, HttpResponse](
      url = url,
      body = registerWithNoIdData
    )(
      wts = implicitly,
      rds = implicitly,
      hc = desHeaderCarrier,
      ec = implicitly
    ) map(response => handleHttpResponseWithoutIdForErrors(response= response, url = url)) andThen
      registrationAuditService.sendRegisterWithoutIdAuditEvent(
        externalId = externalId,
        psaType = "Organisation",
        requestJson = registerWithNoIdData
      )
  }

  private def handleHttpResponseWithIdForErrors(response: HttpResponse, url:String):Either[HttpException, RegisterWithIdResponse] = {
    response.status match {
      case OK =>
        Json.parse(response.body).validate[RegisterWithIdResponse] match {
          case JsSuccess(value, _) => Right(value)
          case JsError(errors) => throw JsResultException(errors)
        }
      case _ =>
        Left(handleErrorResponse("POST", url)(response))
    }
  }

  private def handleHttpResponseWithoutIdForErrors(response: HttpResponse, url:String):Either[HttpException, RegisterWithoutIdResponse] = {
    response.status match {
      case OK =>
        Json.parse(response.body).validate[RegisterWithoutIdResponse] match {
          case JsSuccess(value, _) => Right(value)
          case JsError(errors) => throw JsResultException(errors)
        }
      case _ =>
        Left(handleErrorResponse("POST", url)(response))
    }
  }
}
