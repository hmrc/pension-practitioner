/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.Logger
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{BadRequestException, Request => _}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.{AuthUtil, ErrorHandler, HttpResponseHelper}

import scala.concurrent.{ExecutionContext, Future}

class AssociationController @Inject()(
                                       override val authConnector: AuthConnector,
                                       associationConnector: AssociationConnector,
                                       cc: ControllerComponents,
                                       util: AuthUtil
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with HttpResponseHelper
    with ErrorHandler
    with AuthorisedFunctions {

  private val logger = Logger(classOf[AssociationController])

  def authorisePsp: Action[AnyContent] = Action.async {
    implicit request =>
      util.doAuth { _ =>
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
  }

  def deAuthorisePsp: Action[AnyContent] = Action.async {
    implicit request =>
      util.doAuth { _ =>
        val feJson = request.body.asJson
        val pstrOpt = request.headers.get("pstr")
        logger.debug(s"[Psp-DeAuthorisation-Incoming-Payload]$feJson")

        (feJson, pstrOpt) match {
          case (Some(jsValue), Some(pstr)) =>
            associationConnector.deAuthorisePsp(jsValue, pstr).map {
              case Right(response) => result(response)
              case Left(e) => result(e)
            }
          case _ =>
            Future.failed(new BadRequestException("No Request Body received for psp deAuthorisation"))
        }
      }
  }
}
