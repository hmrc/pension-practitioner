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

package connectors

import audit.SubscriptionAuditService
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.FORBIDDEN
import com.google.inject.Inject
import config.AppConfig
import play.api.http.Status._
import play.api.libs.json._
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SubscriptionConnector @Inject()(http: HttpClient,
                                      config: AppConfig,
                                      headerUtils: HeaderUtils,
                                      subscriptionAuditService: SubscriptionAuditService
                                     ) extends HttpResponseHelper {

  def pspSubscription(externalId: String, data: JsValue)
                              (implicit hc: HeaderCarrier,
                                ec: ExecutionContext,
                                request: RequestHeader): Future[Either[HttpException, String]] = {
    val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    val futureHttpResponse = http.POST[JsValue, HttpResponse](
      config.pspSubscriptionUrl, data)(implicitly, implicitly, headerCarrier, implicitly) andThen
      subscriptionAuditService.sendSubscribeAuditEvent(externalId, data)
    futureHttpResponse.map(httpResponse => processResponse(httpResponse, config.pspSubscriptionUrl))
  }

  private def processResponse(response: HttpResponse, url: String)(
    implicit request: RequestHeader, ec: ExecutionContext) : Either[HttpException, String] = {
    if (response.status == OK) {
      Logger.info(s"POST of $url returned successfully")
      Right(response.body)
    } else {
      processFailureResponse(response, url)
    }
  }

  private def processFailureResponse(response: HttpResponse, url: String): Either[HttpException, String] = {
    Logger.warn(s"POST or $url returned ${response.status} with body ${response.body}")
    response.status match {
      case FORBIDDEN if response.body.contains("ACTIVE_PSPID_ALREADY_EXISTS") =>
        Left(new ConflictException("ACTIVE_PSPID_ALREADY_EXISTS"))
      case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
        Left(new BadRequestException("INVALID PAYLOAD"))
      case _ => Left(handleErrorResponse("POST", url)(response))
    }
  }
}
