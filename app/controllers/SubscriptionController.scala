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

package controllers

import com.google.inject.Inject
import connectors.{SchemeConnector, SubscriptionConnector}
import models.ListOfSchemes
import models.enumeration.JourneyType
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import transformations.userAnswersToDes.PSPSubscriptionTransformer
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.{ErrorHandler, HttpResponseHelper}
import scala.annotation.unused


import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject()(
                                        subscriptionConnector: SubscriptionConnector,
                                        schemeConnector: SchemeConnector,
                                        pspSubscriptionTransformer: PSPSubscriptionTransformer,
                                        cc: ControllerComponents,
                                        pspAuthAction: actions.PspAuthAction,
                                        noEnrolmentAuthAction: actions.NoEnrolmentAuthAction
                                      )(implicit ec: ExecutionContext) extends BackendController(cc)
  with HttpResponseHelper
  with ErrorHandler {

  private val logger = Logger(classOf[SubscriptionController])

  def subscribePsp(@unused journeyType: JourneyType.Name): Action[AnyContent] = noEnrolmentAuthAction.async { implicit request =>
      val feJson = request.body.asJson
      logger.debug(s"[PSP-Subscription-Incoming-Payload]$feJson")
      feJson match {
        case Some(json) =>
          json.transform(pspSubscriptionTransformer.transformPsp) match {
            case JsSuccess(data, _) =>
              logger.debug(s"[PSP-Subscription-Outgoing-Payload]$data")
              subscriptionConnector.pspSubscription(request.externalId, data) map {
                case Right(response) => result(response)
                case Left(e) => result(e)
              }
            case JsError(errors) => throw JsResultException(errors)
          }
        case _ =>
          Future.failed(new BadRequestException("Bad Request with no request body returned for PSP subscription"))
      }
  }

  def getPspDetailsSelf: Action[AnyContent] = pspAuthAction.async {
    implicit request =>
      subscriptionConnector.getSubscriptionDetails(request.pspId.value).map {
        case Right(pspDetails) =>
          logger.debug(s"[Get-psp-details-transformed]$pspDetails")
          Ok(Json.toJson(pspDetails))
        case Left(e) => result(e)
      }
  }

  def deregisterPspSelf: Action[AnyContent] = pspAuthAction.async { implicit request =>
    val feJson = request.body.asJson
    logger.debug(s"[PSP-Deregistration-Payload]$feJson")
    feJson match {
      case Some(json) =>
        subscriptionConnector.pspDeregistration(request.pspId.value, json) map {
          case Right(response) => result(response)
          case Left(e) => result(e)
        }
      case _ =>
        Future.failed(new BadRequestException("Bad Request with no request body for PSP subscription"))
    }
  }

  def canDeregisterSelf: Action[AnyContent] = pspAuthAction.async {
    implicit request =>
      schemeConnector.listOfSchemes.map {
        case Right(jsValue) =>
          jsValue.validate[ListOfSchemes] match {
            case JsSuccess(listOfSchemes, _) =>
              val schemes = listOfSchemes.schemeDetails.getOrElse(List.empty)
              val canDeregister = schemes == List.empty || !schemes.exists(_.schemeStatus == "Open")

              Ok(Json.toJson(canDeregister))
            case JsError(errors) => throw JsResultException(errors)
          }
        case Left(e) => result(e)
      }
  }
}
