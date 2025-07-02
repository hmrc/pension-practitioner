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

package controllers.actions

import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class NoEnrolmentAuthRequest[A](request: Request[A], externalId: String) extends WrappedRequest[A](request)

case class CredIdNotFoundFromAuth(msg: String = "Not Authorised - Unable to retrieve credentials - id")
  extends UnauthorizedException(msg)

class NoEnrolmentAuthAction @Inject()(
                            override val authConnector: AuthConnector,
                            val parser: BodyParsers.Default
                          )(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[NoEnrolmentAuthRequest, AnyContent]
    with ActionFunction[Request, NoEnrolmentAuthRequest]
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: NoEnrolmentAuthRequest[A] => Future[Result]): Future[Result] =
    invoke(request, block)(using HeaderCarrierConverter.fromRequest(request))

  private def invoke[A](request: Request[A], block: NoEnrolmentAuthRequest[A] => Future[Result])
               (implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(Retrievals.externalId) {
      case Some(externalId) => block(NoEnrolmentAuthRequest(request, externalId))
      case _ => Future.failed(CredIdNotFoundFromAuth())
    }
  } recover {
    case e:
      InsufficientEnrolments =>
      logger.warn("Failed to authorise due to insufficient enrolments", e)
      Forbidden("Current user doesn't have a valid enrolment.")
    case e:
      AuthorisationException =>
      logger.warn(s"Failed to authorise", e)
      Unauthorized(s"Failed to authorise user: ${e.reason}")
    case NonFatal(thr) =>
      logger.error(s"Error returned from auth service: ${thr.getMessage}", thr)
      throw thr
  }
}
