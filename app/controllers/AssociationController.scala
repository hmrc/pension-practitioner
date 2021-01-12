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

package controllers

import com.google.inject.Inject
import connectors.AssociationConnector
import play.api.Logger
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{BadRequestException, Request => _}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.{AuthUtil, HttpResponseHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociationController @Inject()(override val authConnector: AuthConnector,
                                      associationConnector: AssociationConnector,
                                      cc: ControllerComponents,
                                      util: AuthUtil
                                     ) extends BackendController(cc) with HttpResponseHelper with AuthorisedFunctions {

  def authorisePsp: Action[AnyContent] = Action.async {
    implicit request =>
      util.doAuth { _ =>
        val feJson = request.body.asJson
        val pstrOpt = request.headers.get("pstr")
        Logger.debug(s"[Psp-Association-Incoming-Payload]$feJson")

        (feJson, pstrOpt) match {
          case (Some(jsValue), Some(pstr)) =>
            associationConnector.authorisePsp(jsValue, pstr).map(result)
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
        Logger.debug(s"[Psp-DeAuthorisation-Incoming-Payload]$feJson")

        (feJson, pstrOpt) match {
          case (Some(jsValue), Some(pstr)) =>
            associationConnector.deAuthorisePsp(jsValue, pstr).map(result)
          case _ =>
            Future.failed(new BadRequestException("No Request Body received for psp deAuthorisation"))
        }
      }
  }
}
