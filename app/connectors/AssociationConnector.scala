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

import com.google.inject.Inject
import config.AppConfig
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse}
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

class AssociationConnector @Inject()(
                                      httpClient: HttpClient,
                                      appConfig: AppConfig,
                                      headerUtils: HeaderUtils
                                    )
  extends HttpResponseHelper {

  private val logger = Logger(classOf[AssociationConnector])


  def authorisePsp(json: JsValue, pstr: String)
                  (implicit ec: ExecutionContext): Future[Either[HttpException, HttpResponse]] = {

    val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader())
    val url = appConfig.pspAuthorisationUrl.format(pstr)
    logger.debug(s"[Psp-Association-Outgoing-Payload] - ${json.toString()}")
    httpClient.POST[JsValue, HttpResponse](url, json)(implicitly, implicitly, headerCarrier, implicitly) map (
      response => responseToEither(response = response, url = url))
  }

  def deAuthorisePsp(json: JsValue, pstr: String)
                    (implicit ec: ExecutionContext): Future[Either[HttpException, HttpResponse]] = {

    val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader())
    val url = appConfig.pspDeAuthorisationUrl.format(pstr)
    logger.debug(s"[Psp-DeAuthorisation-Outgoing-Payload] - ${json.toString()}")
    httpClient.POST[JsValue, HttpResponse](url, json)(implicitly, implicitly, headerCarrier, implicitly) map (
      response => responseToEither(response = response, url = url))
  }

  private def responseToEither(response: HttpResponse, url: String): Either[HttpException, HttpResponse] = {
    response.status match {
      case OK => Right(response)
      case _ =>
        Left(handleErrorResponse("POST", url)(response))
    }
  }
}
