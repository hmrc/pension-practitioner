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

import audit.{AuditService, MinimalDetailsAuditService}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import models.MinimalDetails
import play.api.Logger
import play.api.http.Status._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{ErrorHandler, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[MinimalConnectorImpl])
trait MinimalConnector {

  def getMinimalDetails(idValue: String, idType: String, regime: String
                       )(implicit headerCarrier: HeaderCarrier,
                         ec: ExecutionContext,
                         request: RequestHeader): Future[Either[HttpResponse, MinimalDetails]]
}

@Singleton
class MinimalConnectorImpl @Inject()(
    httpClientV2: HttpClientV2,
    appConfig: AppConfig,
    invalidPayloadHandler: InvalidPayloadHandler,
    headerUtils: HeaderUtils,
    auditService: AuditService
) extends MinimalConnector with ErrorHandler with MinimalDetailsAuditService {

  private val logger = Logger(classOf[MinimalConnectorImpl])

  override def getMinimalDetails(idValue: String, idType: String, regime: String
                                )(implicit headerCarrier: HeaderCarrier,
                                  ec: ExecutionContext,
                                  request: RequestHeader): Future[Either[HttpResponse, MinimalDetails]] = {

    val url = url"${appConfig.minimalDetailsUrl.format(regime, idType, idValue)}"

    httpClientV2.get(url)
      .setHeader(headerUtils.integrationFrameworkHeader()*)
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          logger.debug(s"[Get-psp-minimal-details-untransformed]${response.json}")
          Right(validateGetJson(response))
        case _ => Left(response)
      }
    } andThen sendGetMinimalDetailsEvent(idType, idValue)(auditService.sendEvent) andThen logWarning
  }

  private case object MinDetailsInvalidResponseException extends Exception

  private def validateGetJson(response: HttpResponse): MinimalDetails =
    response.json.validate[MinimalDetails](using MinimalDetails.reads).fold(
      _ => {
        invalidPayloadHandler.logFailures("/resources/schemas/minimalDetails.json")(response.json)
        throw MinDetailsInvalidResponseException
      },
      value => value
    )

  private def logWarning[A]: PartialFunction[Try[Either[HttpResponse, A]], Unit] = {
    case Success(Left(response)) if response.status != OK =>
      logger.warn(s"Minimal details received error response from integration " +
        s"framework with status and ${response.status} details ${response.body}")
    case Failure(e) =>
      logger.error(s"Minimal details received error response from integration framework", e)
  }

}

