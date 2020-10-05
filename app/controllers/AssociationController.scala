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
import connectors.AssociationConnector
import play.api.Logger
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.auth.core.retrieve.v2
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UnauthorizedException, Request => _}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.HttpResponseHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociationController @Inject()(override val authConnector: AuthConnector,
                                       associationConnector: AssociationConnector,
                                       cc: ControllerComponents
                                     ) extends BackendController(cc) with HttpResponseHelper with AuthorisedFunctions {

  def authorisePsp: Action[AnyContent] = Action.async {
    implicit request =>
      withAuth { _ =>
        val feJson = request.body.asJson
        val pstrOpt = request.headers.get("pstr")
        Logger.debug(s"[Psp-Association-Incoming-Payload]$feJson")

        (feJson, pstrOpt) match {
          case (Some(jsValue), Some(pstr)) =>
            associationConnector.associatePsp(jsValue, pstr).map(result)
          case _ =>
            Future.failed(new BadRequestException("No Request Body received for psp association"))
        }
      }
  }

  private def withAuth(block: String => Future[Result])
                      (implicit hc: HeaderCarrier): Future[Result] = {

    authorised().retrieve(v2.Retrievals.externalId) {
      case Some(externalId) =>
        block(externalId)
      case _ =>
        Future.failed(new UnauthorizedException("Not Authorised - Unable to retrieve credentials - externalId"))
    }
  }
}