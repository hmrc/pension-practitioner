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

package controllers

import com.google.inject.Inject
import connectors.SubscriptionConnector
import play.api.Logger
import play.api.libs.json.JsError
import play.api.libs.json.JsResultException
import play.api.libs.json.JsSuccess
import play.api.mvc._
import transformations.userAnswersToDes.PSPSubscriptionTransformer
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.{Request => _}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.ErrorHandler
import utils.HttpResponseHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionController @Inject()(
  override val authConnector: AuthConnector,
  subscriptionConnector: SubscriptionConnector,
  pspSubscriptionTransformer: PSPSubscriptionTransformer,
  cc: ControllerComponents
) extends BackendController(cc)
    with AuthorisedFunctions
    with HttpResponseHelper
    with ErrorHandler {

  def subscribePsp: Action[AnyContent] = Action.async { implicit request =>
    {
      withAuth { externalId =>
        val feJson = request.body.asJson
        Logger.debug(s"[PSP-Subscription-Incoming-Payload]$feJson")
        feJson match {
          case Some(json) =>
            println("\n>>>>BEFORE:" + json)
            json.transform(pspSubscriptionTransformer.transformPspSubscription) match {
              case JsSuccess(data, _) =>
                println("\nAFTER:" + data)
                Logger.debug(s"[PSP-Subscription-Outgoing-Payload]$data")
                subscriptionConnector.pspSubscription(externalId, data).map {
                  case Right(response) => Ok(response)
                  case Left(e)         => result(e)
                }
              case JsError(errors) => throw JsResultException(errors)
            }
          case _ =>
            Future.failed(
              new BadRequestException(
                "Bad Request with no request body returned for PSP subscription"
              )
            )
        }
      }
    }
  }

  private def withAuth(
    block: String => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] = {

    authorised().retrieve(v2.Retrievals.externalId) {
      case Some(externalId) =>
        block(externalId)
      case _ =>
        Future.failed(
          new UnauthorizedException(
            "Not Authorised - Unable to retrieve credentials - externalId"
          )
        )
    }
  }
}
