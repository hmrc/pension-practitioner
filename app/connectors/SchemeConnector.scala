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
import models.SchemeReferenceNumber
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{ErrorHandler, HttpResponseHelper}

import scala.concurrent.{ExecutionContext, Future}

class SchemeConnector @Inject()(
  httpClientV2: HttpClientV2,
  config: AppConfig
)(implicit ec: ExecutionContext)
  extends HttpResponseHelper
    with ErrorHandler
    with Logging {

  def checkForAssociation(psaIdOrPspId: Either[PsaId, PspId], srn: SchemeReferenceNumber)
                         (implicit headerCarrier: HeaderCarrier): Future[Either[HttpException, Boolean]] = {

    val id = psaIdOrPspId match {
      case Left(psaId) => ("psaId", psaId.value)
      case Right(pspId) => ("pspId", pspId.value)
    }
    val headers: Seq[(String, String)] = Seq(id, ("schemeReferenceNumber", srn), ("Content-Type", "application/json"))

    httpClientV2
      .get(url"${config.checkAssociationUrl}")
      .setHeader(headers*)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            response
              .json
              .validate[Boolean]
              .fold(_ =>
                Left(new InternalServerException("Response from pension-scheme cannot be parsed to boolean")),
                Right(_)
              )
          case _ =>
            Left(handleErrorResponse(
              httpMethod = "GET",
              url        = config.checkAssociationUrl,
              args       = Seq("Bad Request with missing parameters PSA Id or SRN")*
            )(response))
        }
      }

  }

  def listOfSchemes(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpResponse, JsValue]] = {

    val headers: Seq[(String, String)] = Seq(("idType", "pspid"))
    val url = url"${config.listOfSchemesUrl}"

    httpClientV2
      .get(url)
      .setHeader(headers*)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Right(response.json)
          case _ =>
            logger.warn(s"List schemes with headers: (idType, pspid) and url $url" +
              s" returned response ${response.status} with body ${response.body}")
            Left(response)
        }
      }
  }
}
