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

import com.google.inject.Inject
import config.AppConfig
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

class AssociationConnector  @Inject()(httpClient: HttpClient,
                                      appConfig: AppConfig,
                                      headerUtils: HeaderUtils
                                     )
  extends HttpResponseHelper {

  def authorisePsp(json: JsValue, pstr: String)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    val url = appConfig.pspAuthorisationUrl.format(pstr)
    Logger.debug(s"[Psp-Association-Outgoing-Payload] - ${json.toString()}")
    httpClient.POST[JsValue, HttpResponse](url, json)(implicitly, implicitly, headerCarrier, implicitly)
  }

  def deAuthorisePsp(json: JsValue, pstr: String)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    val url = appConfig.pspDeAuthorisationUrl.format(pstr)
    Logger.debug(s"[Psp-DeAuthorisation-Outgoing-Payload] - ${json.toString()}")
    httpClient.POST[JsValue, HttpResponse](url, json)(implicitly, implicitly, headerCarrier, implicitly)
  }
}
