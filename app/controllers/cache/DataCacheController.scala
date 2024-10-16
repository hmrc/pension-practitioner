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

package controllers.cache

import com.google.inject.Inject
import play.api.Logger
import play.api.mvc._
import repository.DataCacheRepository
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class DataCacheController @Inject()(
                                     repository: DataCacheRepository,
                                     val authConnector: AuthConnector,
                                     cc: ControllerComponents
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  import DataCacheController._

  private val logger = Logger(classOf[DataCacheController])

  def save: Action[AnyContent] = Action.async {
    implicit request =>
      getId { id =>
        request.body.asJson.map {
          jsValue =>
            repository.save(id, jsValue)
              .map(_ => Created)
        } getOrElse Future.successful(BadRequest)
      }
  }

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      getId { id =>
        repository.get(id).map { response =>
          logger.debug(message = s"DataCacheController.get: Response for request Id $id is $response")
          response.map {
            Ok(_)
          } getOrElse NotFound
        }
      }
  }

  def remove: Action[AnyContent] = Action.async {
    implicit request =>
      getId { id =>
        repository.remove(id).map(_ => Ok)
      }
  }

  private def getId(block: String => Future[Result])
                   (implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(Retrievals.externalId) {
      case Some(id) =>
        block(id)
      case _ => Future.failed(CredIdNotFoundFromAuth())
    }
  }

}

object DataCacheController {

  case class CredIdNotFoundFromAuth(msg: String = "Not Authorised - Unable to retrieve credentials - id")
    extends UnauthorizedException(msg)

}
