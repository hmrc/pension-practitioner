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

import com.google.inject.Inject
import config.AppConfig
import play.Logger
import play.api.http.Status.OK
import play.api.libs.json._
import play.api.mvc.RequestHeader
import transformations.toUserAnswers.PspDetailsTransformer
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject()(http: HttpClient,
                                      config: AppConfig,
                                      headerUtils: HeaderUtils,
                                      pspDetailsTransformer: PspDetailsTransformer
                                     ) extends HttpResponseHelper {

  def pspSubscription(data: JsValue)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    http.POST[JsValue, HttpResponse](config.pspSubscriptionUrl, data)(implicitly, implicitly, headerCarrier, implicitly)
  }

  def getSubscriptionDetails(pspId: String)(implicit
                                                        headerCarrier: HeaderCarrier,
                                                        ec: ExecutionContext,
                                                        request: RequestHeader): Future[Either[HttpResponse, JsValue]] = {

    val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    val url = config.subscriptionDetailsUrl.format(pspId)

    http.GET[HttpResponse](url)(implicitly, hc, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger.debug(s"[Get-psp-details-untransformed]${response.json}")
          Right(validateGetJson(response.json))
        case _ => Left(response)
      }
    }
  }

  case class FailedMapToUserAnswersException() extends Exception
  private def validateGetJson(json: JsValue): JsValue =
    json.transform(pspDetailsTransformer.transformToUserAnswers).getOrElse(throw new FailedMapToUserAnswersException)

}
