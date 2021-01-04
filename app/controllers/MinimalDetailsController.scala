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

import connectors.MinimalConnector
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.{ErrorHandler, HttpResponseHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MinimalDetailsController @Inject()(
                                          minimalConnector: MinimalConnector,
                                          cc: ControllerComponents
                                     ) extends BackendController(cc) with ErrorHandler with HttpResponseHelper {

  def getMinimalDetails: Action[AnyContent] = Action.async {
    implicit request =>
      retrieveIdAndTypeFromHeaders{ (idValue, idType, regime) =>
        minimalConnector.getMinimalDetails(idValue, idType, regime).map {
          case Right(minDetails) => Ok(Json.toJson(minDetails))
          case Left(e) => result(e)
        }
      }
  }

  private def retrieveIdAndTypeFromHeaders(block: (String, String, String) => Future[Result])(implicit request: RequestHeader):Future[Result] = {
    (request.headers.get("psaId"), request.headers.get("pspId")) match {
      case (Some(id), None) => block(id, "psaid", "poda")
      case (None, Some(id)) => block(id, "pspid", "podp")
      case _ => Future.failed(new BadRequestException("No PSA or PSP Id in the header for get minimal details"))
    }
  }

}
