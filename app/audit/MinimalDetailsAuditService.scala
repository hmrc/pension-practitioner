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

package audit

import models.MinimalDetails
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

import scala.util.{Failure, Success, Try}

trait MinimalDetailsAuditService {

  private val logger = Logger(classOf[MinimalDetailsAuditService])


  def sendGetMinimalDetailsEvent(idType: String, idValue: String)
                                (sendEvent: MinimalDetailsEvent => Unit):
  PartialFunction[Try[Either[HttpResponse, MinimalDetails]], Unit] = {

    case Success(Right(minimalDetails)) =>
      sendEvent(
        MinimalDetailsEvent(
          idType = idType,
          idValue = idValue,
          name = minimalDetails.name,
          isSuspended = Some(minimalDetails.isPsaSuspended),
          rlsFlag = Some(minimalDetails.rlsFlag),
          deceasedFlag = Some(minimalDetails.deceasedFlag),
          status = Status.OK,
          response = Some(Json.toJson(minimalDetails))
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        MinimalDetailsEvent(
          idType = idType,
          idValue = idValue,
          name = None,
          isSuspended = None,
          rlsFlag = None,
          deceasedFlag = None,
          status = e.status,
          response = None
        )
      )
    case Failure(t) =>
      logger.error("Error in MinimalDetails connector", t)
  }


}
