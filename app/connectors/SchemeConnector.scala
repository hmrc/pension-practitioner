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

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import utils.{ErrorHandler, HttpResponseHelper}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {

  def listOfSchemes(pspId: String
                   )(implicit headerCarrier: HeaderCarrier,
                    ec: ExecutionContext,
                    request: RequestHeader
                   ): Future[Either[HttpResponse, JsValue]]
}

class SchemeConnectorImpl @Inject()(
  httpClientV2: HttpClientV2,
  config: AppConfig
) extends SchemeConnector with HttpResponseHelper with ErrorHandler {

  private val logger = Logger(classOf[SchemeConnectorImpl])

  override def listOfSchemes(pspId: String
                            )(implicit headerCarrier: HeaderCarrier,
                             ec: ExecutionContext,
                             request: RequestHeader
                            ): Future[Either[HttpResponse, JsValue]] = {

    val headers: Seq[(String, String)] = Seq(("idType", "pspid"), ("idValue", pspId))
    val url = url"${config.listOfSchemesUrl}"

    httpClientV2.get(url)
      .setHeader(headers: _*)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK => Right(response.json)
          case _ =>
            logger.warn(s"List schemes with headers: (idType, pspid), (idValue $pspId) and url $url" +
              s" returned response ${response.status} with body ${response.body}")
            Left(response)
        }
      }
  }
}
