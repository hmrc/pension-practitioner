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

package utils

import play.api.Logger
import play.api.libs.json.JsResultException
import play.api.mvc.Result
import uk.gov.hmrc.http._
import play.api.http.Status.OK
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ErrorHandler {

  def recoverFromError: PartialFunction[Throwable, Future[Result]] = {
    case e: JsResultException =>
      Future.failed(new BadRequestException(e.getMessage))
    case e: BadRequestException =>
      Future.failed(new BadRequestException(e.message))
    case e: NotFoundException =>
      Future.failed(new NotFoundException(e.message))
    case e: UpstreamErrorResponse =>
      Future.failed(UpstreamErrorResponse(e.message, e.statusCode, e.reportAs, e.headers))
    case e: Exception =>
      Future.failed(new Exception(e.getMessage))
  }

  protected def logWarning[A](endpoint: String): PartialFunction[Try[Either[HttpResponse, A]], Unit] = {
    case Success(Left(response)) if response.status != OK =>
      Logger.warn(s"$endpoint received error response from integration framework with status ${response.status} and details ${response.body}")
    case Failure(e) =>
      Logger.error(s"$endpoint received error response from integration framework", e)
  }
}

