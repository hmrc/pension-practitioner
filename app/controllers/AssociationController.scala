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
import connectors.AssociationConnector
import controllers.actions.{PsaAuthAction, PsaPspAuthAction, PsaSchemeAuthAction, PspAuthAction, PspSchemeAuthAction}
import models.SchemeReferenceNumber
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.{ErrorHandler, HttpResponseHelper}

import scala.concurrent.{ExecutionContext, Future}

class AssociationController @Inject()(
                                       associationConnector: AssociationConnector,
                                       cc: ControllerComponents,
                                       authAction: PsaPspAuthAction,
                                       psaAuthAction: PsaAuthAction,
                                       psaSchemeAuthAction: PsaSchemeAuthAction,
                                       pspAuthAction: PspAuthAction,
                                       pspSchemeAuthAction: PspSchemeAuthAction
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with HttpResponseHelper
    with ErrorHandler {

  private val logger = Logger(classOf[AssociationController])

  def authorisePspSrn(srn: SchemeReferenceNumber): Action[AnyContent] = (psaAuthAction andThen psaSchemeAuthAction(srn)).async {
    implicit request =>
      val feJson = request.body.asJson
      val pstrOpt = request.headers.get("pstr")
      logger.debug(s"[Psp-Association-Incoming-Payload]$feJson")

      (feJson, pstrOpt) match {
        case (Some(jsValue), Some(pstr)) =>
          associationConnector.authorisePsp(jsValue, pstr).map {
            case Right(response) => result(response)
            case Left(e) => result(e)
          }
        case _ =>
          Future.failed(new BadRequestException("No Request Body received for psp association"))
      }
  }

  def deAuthorisePspSrn(srn: SchemeReferenceNumber): Action[AnyContent] = (psaAuthAction andThen psaSchemeAuthAction(srn)).async {
    implicit request =>
      val feJson = request.body.asJson
      val pstrOpt = request.headers.get("pstr")
      logger.debug(s"[Psp-DeAuthorisation-Incoming-Payload]$feJson")

      (feJson, pstrOpt) match {
        case (Some(jsValue), Some(pstr)) =>
          val error = conditionalErrorResponse(jsValue, "initiatedIDNumber", id => PsaId(id) == request.psaId)
          error match {
            case Some(error) => Future.successful(error)
            case None =>
              associationConnector.deAuthorisePsp(jsValue, pstr).map {
                case Right(response) => result(response)
                case Left(e) => result(e)
              }
          }

        case _ =>
          Future.failed(new BadRequestException("No Request Body received for psp deAuthorisation"))
      }
  }

  private def conditionalErrorResponse(json: JsValue, valueName: String, condition: String => Boolean):Option[Result] = {
    val ceaseNumber = (json \ valueName).toOption
    ceaseNumber
      .map { value =>
        value.asOpt[String]
          .map { valueToCheck =>
            if(condition(valueToCheck)) {
              None
            }
            else {
              Some(Forbidden(s"$valueName authentication condition failed"))
            }
          }.getOrElse(Some(BadRequest(s"$valueName is not a string")))
      }.getOrElse(Some(BadRequest(s"$valueName is not available in json body")))
  }

  def deAuthorisePspSelf(srn: SchemeReferenceNumber): Action[AnyContent] = (pspAuthAction andThen pspSchemeAuthAction(srn)).async {
    implicit request =>
      val feJson = request.body.asJson
      val pstrOpt = request.headers.get("pstr")
      logger.debug(s"[Psp-DeAuthorisation-Incoming-Payload]$feJson")

      (feJson, pstrOpt) match {
        case (Some(jsValue), Some(pstr)) =>
          def condition(pspIdToCheck: String) = {
            PspId(pspIdToCheck) == request.pspId
          }
          val error = {
            val ceaseNumber = conditionalErrorResponse(jsValue, "ceaseNumber", condition)
            if(ceaseNumber.isEmpty) {
              conditionalErrorResponse(jsValue, "initiatedIDNumber", condition)
            } else {
              ceaseNumber
            }
          }
          error match {
              case Some(error) => Future.successful(error)
              case None =>
                associationConnector.deAuthorisePsp(jsValue, pstr).map {
                  case Right(response) => result(response)
                  case Left(e) => result(e)
                }
            }
        case _ =>
          Future.failed(new BadRequestException("No Request Body received for psp deAuthorisation"))
      }
  }
}
