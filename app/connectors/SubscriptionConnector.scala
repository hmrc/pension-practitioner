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

package connectors

import audit.SubscriptionAuditService
import com.google.inject.Inject
import config.AppConfig
import play.api.Logger
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import transformations.toUserAnswers.PspDetailsTransformer
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{HttpResponseHelper, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject()(
    httpClientV2: HttpClientV2,
    config: AppConfig,
    headerUtils: HeaderUtils,
    subscriptionAuditService: SubscriptionAuditService,
    pspDetailsTransformer: PspDetailsTransformer,
    invalidPayloadHandler: InvalidPayloadHandler
) extends HttpResponseHelper {

  private val logger = Logger(classOf[SubscriptionConnector])

  def pspSubscription(
                       externalId: String, data: JsValue
                     )(implicit ec: ExecutionContext,
                      request: RequestHeader
                     ): Future[Either[HttpException, HttpResponse]] = {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader())
    val url = url"${config.pspSubscriptionUrl}"

    httpClientV2.post(url)
      .withBody(data)
      .setHeader(headerCarrier.extraHeaders*)
      .execute[HttpResponse] map { response =>
      responseToEither(response = response, url = url.toString)
    } andThen subscriptionAuditService.sendSubscribeAuditEvent(externalId, data) andThen
      logFailures("PSP Subscription", data, "/resources/schemas/pspCreateAmend.json", url.toString)

  }

  private def responseToEither(response: HttpResponse, url: String): Either[HttpException, HttpResponse] = {
    response.status match {
      case OK =>
        Right(response)
      case FORBIDDEN if response.body.contains("ACTIVE_PSPID") =>
        Right(response)
      case _ =>
        Left(handleErrorResponse("POST", url)(response))
    }
  }

  def getSubscriptionDetails(pspId: String
                            )(implicit ec: ExecutionContext): Future[Either[HttpResponse, JsValue]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader())
    val url = url"${config.subscriptionDetailsUrl.format(pspId)}"

    httpClientV2.get(url)
      .setHeader(hc.extraHeaders*)
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          logger.debug(s"[Get-psp-details-untransformed]${response.json}")
          Right(validateGetJson(response.json))
        case _ => Left(response)
      }
    }
  }

  def pspDeregistration(pspId: String,
                        data: JsValue
                       )(implicit ec: ExecutionContext): Future[Either[HttpException, HttpResponse]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader())
    val url = url"${config.pspDeregistrationUrl.format(pspId)}"

    httpClientV2.post(url)
      .withBody(data)
      .setHeader(hc.extraHeaders*)
      .execute[HttpResponse] map { response =>
        responseToEither(response = response, url = url.toString)
      } andThen
      logFailures("Deregister PSP", data, "/resources/schemas/deregister1469.json", url.toString)

  }

  private def validateGetJson(json: JsValue): JsValue =
    json.transform(pspDetailsTransformer.transformToUserAnswers) match {
      case JsSuccess(value, _) => value
      case JsError(errors) => throw JsResultException(errors)
    }

  private def logFailures(endpoint: String,
                          data: JsValue,
                          schemaPath: String,
                          args: String*
                         ): PartialFunction[Try[Either[HttpException, HttpResponse]], Unit] = {
    case Success(Right(response)) if response.status == BAD_REQUEST && response.body.contains("INVALID_PAYLOAD") =>
      invalidPayloadHandler.logFailures(schemaPath, args.headOption.getOrElse(""))(data)
    case Success(Left(error)) => logger.warn(s"$endpoint received error response from API", error)
    case Failure(e: HttpResponse) => logger.warn(s"$endpoint received error response from API", e)
  }

}
