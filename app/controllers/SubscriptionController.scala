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
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import play.api.mvc.{Result, _}
import transformations.userAnswersToDes.PSPSubscriptionTransformer
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.{Request => _, _}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.{ErrorHandler, HttpResponseHelper}

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
            json.transform(pspSubscriptionTransformer.transformPsp) match {
              case JsSuccess(data, _) =>
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

  def getPspDetails: Action[AnyContent] = Action.async {
    implicit request =>
      withAuth { _ =>
        val pspId = request.headers.get("pspId")
        pspId match {
          case Some(id) =>
            subscriptionConnector.getSubscriptionDetails(id).map {
              case Right(pspDetails) =>
                Logger.debug(s"[Get-psp-details-transformed]$pspDetails")
                Ok(Json.toJson(pspDetails))
              case Left(e) => result(e)
            }
          case _ => Future.failed(new BadRequestException("No PSP Id in the header"))
        }
      }
  }

  private def withAuth(block: String => Future[Result])
                      (implicit hc: HeaderCarrier): Future[Result] = {

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
