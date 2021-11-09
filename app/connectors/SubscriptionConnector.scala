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

import audit.SubscriptionAuditService
import com.google.inject.Inject
import config.AppConfig
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import transformations.toUserAnswers.PspDetailsTransformer
import uk.gov.hmrc.http.{HttpClient, _}
import utils.{HttpResponseHelper, InvalidPayloadHandler}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject()(
                                       http: HttpClient,
                                       config: AppConfig,
                                       headerUtils: HeaderUtils,
                                       subscriptionAuditService: SubscriptionAuditService,
                                       pspDetailsTransformer: PspDetailsTransformer,
                                       invalidPayloadHandler: InvalidPayloadHandler
                                     ) extends HttpResponseHelper {

  private val logger = Logger(classOf[SubscriptionConnector])

  def pspSubscription(externalId: String, data: JsValue)
                     (implicit hc: HeaderCarrier,
                      ec: ExecutionContext,
                      request: RequestHeader): Future[HttpResponse] = {
    val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    http.POST[JsValue, HttpResponse](config.pspSubscriptionUrl, data)(implicitly, implicitly, headerCarrier, implicitly) andThen
      subscriptionAuditService.sendSubscribeAuditEvent(externalId, data) andThen
      logFailures("PSP Subscription", data, "/resources/schemas/pspCreateAmend.json", config.pspSubscriptionUrl)
  }

  def getSubscriptionDetails(pspId: String)
                            (implicit
                             headerCarrier: HeaderCarrier,
                             ec: ExecutionContext,
                             request: RequestHeader): Future[Either[HttpResponse, JsValue]] = {

    val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    val url = config.subscriptionDetailsUrl.format(pspId)

    http.GET[HttpResponse](url)(implicitly, hc, implicitly) map { response =>
      response.status match {
        case OK =>
          logger.debug(s"[Get-psp-details-untransformed]${response.json}")
          Right(validateGetJson(response.json))
        case _ => Left(response)
      }
    }
  }

  def pspDeregistration(pspId: String, data: JsValue)
                       (implicit
                        headerCarrier: HeaderCarrier,
                        ec: ExecutionContext,
                        request: RequestHeader): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    val url = config.pspDeregistrationUrl.format(pspId)
    http.POST[JsValue, HttpResponse](url, data)(implicitly, implicitly, hc, implicitly) andThen
      logFailures("Deregister PSP", data, "/resources/schemas/deregister1469.json", url)
  }

  private def validateGetJson(json: JsValue): JsValue =
    json.transform(pspDetailsTransformer.transformToUserAnswers) match {
      case JsSuccess(value, _) => value
      case JsError(errors) => throw JsResultException(errors)
    }

  private def logFailures(endpoint: String, data: JsValue, schemaPath: String, args: String*): PartialFunction[Try[HttpResponse], Unit] = {
    case Success(response) if response.status == BAD_REQUEST && response.body.contains("INVALID_PAYLOAD") =>
      invalidPayloadHandler.logFailures(schemaPath, args.headOption.getOrElse(""))(data)
    case Failure(e: HttpResponse) => logger.warn(s"$endpoint received error response from API", e)
  }


}
