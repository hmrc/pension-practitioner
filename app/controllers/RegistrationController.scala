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
import connectors.RegistrationConnector
import models.registerWithId.Organisation
import models.registerWithoutId.{OrganisationRegistrant, RegisterWithoutIdIndividualRequest}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http.{Request => _, _}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler
import utils.ValidationUtils._

import scala.concurrent.{ExecutionContext, Future}

class RegistrationController @Inject()(
                                        registerConnector: RegistrationConnector,
                                        cc: ControllerComponents,
                                        authAction: actions.NoEnrolmentAuthAction
                                      )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with ErrorHandler {

  def registerWithIdIndividual: Action[AnyContent] = authAction.async {
    implicit request => {
      request.headers.get("nino") match {
        case Some(nino) =>
          val registerWithIdData = Json.obj(fields = "regime" -> "PODP", "requiresNameMatch" -> false, "isAnAgent" -> false)
          registerConnector.registerWithIdIndividual(request.externalId, nino, registerWithIdData).map {
            case Right(response) => Ok(Json.toJson(response))
            case Left(e) => result(e)
          }
        case _ =>
          Future.failed(new BadRequestException(s"Bad Request with missing nino for register with id call for individual"))
      }
    }
  }

  def registerWithIdOrganisation: Action[AnyContent] = authAction.async {
    implicit request => {
      (request.headers.get("utr"), request.body.asJson) match {
        case (Some(utr), Some(jsBody)) =>
          val registerWithIdData =
            Json.obj(
              fields = "regime" -> "PODP",
              "requiresNameMatch" -> true,
              "isAnAgent" -> false,
              "organisation" -> Json.toJson(jsBody.convertTo[Organisation])
            )
          registerConnector.registerWithIdOrganisation(request.externalId, utr, registerWithIdData).map {
            case Right(response) => Ok(Json.toJson(response))
            case Left(e) => result(e)
          }

        case _ =>
          Future.failed(new BadRequestException("Bad Request with missing utr or request body for register with id call for organisation"))
      }
    }
  }

  def registrationNoIdIndividual: Action[RegisterWithoutIdIndividualRequest] = authAction.async(parse.json[RegisterWithoutIdIndividualRequest]) {
    implicit request => {
      registerConnector.registrationNoIdIndividual(request.externalId, request.body).map {
        case Right(response) => Ok(Json.toJson(response))
        case Left(e) => result(e)
      }
    }
  }

  def registrationNoIdOrganisation: Action[OrganisationRegistrant] = authAction.async(parse.json[OrganisationRegistrant]) {
    implicit request => {
      registerConnector.registrationNoIdOrganisation(request.externalId, request.body).map {
        case Right(response) => Ok(Json.toJson(response))
        case Left(e) => result(e)
      }
    }
  }
}
