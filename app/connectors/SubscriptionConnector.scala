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
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.{HttpResponseHelper, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject()(http: HttpClient,
                                      config: AppConfig,
                                      headerUtils: HeaderUtils
                                   //   invalidPayloadHandler: InvalidPayloadHandler
                                     ) extends HttpResponseHelper {

  def pspSubscription(data: JsValue)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    val headerCarrier: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader)
    http.POST[JsValue, HttpResponse](config.pspSubscriptionUrl, data)(implicitly, implicitly, headerCarrier, implicitly).map {
      response =>
        response.status match {
          case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
   //         invalidPayloadHandler.logFailures("/resources/schemas/pspSubscriptionRequest.json", config.pspSubscriptionUrl)(response.json)
            response
          case _ => response
        }
    }
  }
}
