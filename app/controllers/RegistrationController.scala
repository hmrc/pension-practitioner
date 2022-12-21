/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.{Request => _, _}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ValidationUtils._
import utils.{AuthUtil, ErrorHandler}

import scala.concurrent.{ExecutionContext, Future}

class RegistrationController @Inject()(
                                        override val authConnector: AuthConnector,
                                        registerConnector: RegistrationConnector,
                                        cc: ControllerComponents,
                                        util: AuthUtil
                                      )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with ErrorHandler
    with AuthorisedFunctions {

  def registerWithIdIndividual: Action[AnyContent] = Action.async {
    implicit request => {
      util.doAuth { externalId =>
        request.headers.get("nino") match {
          case Some(nino) =>
            val registerWithIdData = Json.obj(fields = "regime" -> "PODP", "requiresNameMatch" -> false, "isAnAgent" -> false)
            registerConnector.registerWithIdIndividual(externalId, nino, registerWithIdData).map {
              case Right(response) => Ok(Json.toJson(response))
              case Left(e) => result(e)
            }
          case _ =>
            Future.failed(new BadRequestException(s"Bad Request with missing nino for register with id call for individual"))
        }
      }
    }
  }

  def registerWithIdOrganisation: Action[AnyContent] = Action.async {
    implicit request => {
      util.doAuth { externalId =>
        (request.headers.get("utr"), request.body.asJson) match {
          case (Some(utr), Some(jsBody)) =>
            val registerWithIdData =
              Json.obj(
                fields = "regime" -> "PODP",
                "requiresNameMatch" -> true,
                "isAnAgent" -> false,
                "organisation" -> Json.toJson(jsBody.convertTo[Organisation])
              )
            registerConnector.registerWithIdOrganisation(externalId, utr, registerWithIdData).map {
              case Right(response) => Ok(Json.toJson(response))
              case Left(e) => result(e)
            }

          case _ =>
            Future.failed(new BadRequestException("Bad Request with missing utr or request body for register with id call for organisation"))
        }
      }
    }
  }

  def registrationNoIdIndividual: Action[RegisterWithoutIdIndividualRequest] = Action.async(parse.json[RegisterWithoutIdIndividualRequest]) {
    implicit request => {
      util.doAuth { externalId =>
        registerConnector.registrationNoIdIndividual(externalId, request.body).map {
          case Right(response) => Ok(Json.toJson(response))
          case Left(e) => result(e)
        }
      }
    }
  }

  def registrationNoIdOrganisation: Action[OrganisationRegistrant] = Action.async(parse.json[OrganisationRegistrant]) {
    implicit request => {
      util.doAuth { externalId =>
        registerConnector.registrationNoIdOrganisation(externalId, request.body).map {
          case Right(response) => Ok(Json.toJson(response))
          case Left(e) => result(e)
        }
      }
    }
  }
}
