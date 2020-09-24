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

package audit

import com.google.inject.Inject
import play.api.http.Status
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class SubscriptionAuditService @Inject()(auditService: AuditService) {

  def sendSubscribeAuditEvent(externalId: String, requestJson: JsValue)
                                  (implicit ec: ExecutionContext, request: RequestHeader): PartialFunction[Try[HttpResponse], Unit] = {
    case Success(response) =>
      println("\nYY")
      auditService.sendEvent(PSPSubscription(externalId, Status.OK, requestJson, Some(response.json)))

    case Failure(error: UpstreamErrorResponse) =>
      auditService.sendEvent(PSPSubscription(externalId, error.statusCode, requestJson, None))

    case Failure(error: HttpException) =>
      auditService.sendEvent(PSPSubscription(externalId, error.responseCode, requestJson, None))

  }

}

case class PSPSubscription(
                            externalId: String,
                            status: Int,
                            request: JsValue,
                            response: Option[JsValue]
                          ) extends AuditEvent {

  override def auditType: String = ""

  override def details: Map[String, String] =
    Map(
      "externalId" -> externalId,
      "status" -> status.toString,
      "request" -> Json.prettyPrint(request),
      "response" -> {
        response match {
          case Some(json) => Json.prettyPrint(json)
          case _ => ""
        }
      }
    )
}

object PSPSubscription {
  implicit val formatsPSPSubscription: Format[PSPSubscription] = Json.format[PSPSubscription]
}
