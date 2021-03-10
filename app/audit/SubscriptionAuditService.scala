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

package audit

import com.google.inject.Inject
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class SubscriptionAuditService @Inject()(auditService: AuditService) {

  def sendSubscribeAuditEvent(externalId: String, requestJson: JsValue)
                             (implicit ec: ExecutionContext, request: RequestHeader): PartialFunction[Try[HttpResponse], Unit] = {

    case Success(response) =>
      auditService.sendExtendedEvent(PSPSubscription(externalId, response.status, requestJson, Some(response.json)))

    case Failure(error: UpstreamErrorResponse) =>
      auditService.sendExtendedEvent(PSPSubscription(externalId, error.statusCode, requestJson, None))

    case Failure(error: HttpException) =>
      auditService.sendExtendedEvent(PSPSubscription(externalId, error.responseCode, requestJson, None))
  }
}

case class PSPSubscription(
                            externalId: String,
                            status: Int,
                            request: JsValue,
                            response: Option[JsValue]
                          ) extends ExtendedAuditEvent {

  override def auditType: String = "PensionSchemePractitionerSubscription"

  val doNothing: Reads[JsObject] =
    __.json.put(Json.obj())

  private val expandAcronymTransformer: JsValue => JsObject =
    json => json.as[JsObject].transform(
      __.json.update(
        (
          ((__ \ "subscriptionTypeAndPensionSchemePractitionerIdDetails").json.copyFrom(
            (__ \ "subscriptionTypeAndPSPIDDetails").json.pick) orElse doNothing
            ) and
            ((__ \ "subscriptionTypeAndPensionSchemePractitionerIdDetails" \ "existingPensionSchemePractitionerId").json.copyFrom(
              (__ \ "subscriptionTypeAndPSPIDDetails" \ "existingPSPID").json.pick) orElse doNothing
              )
          ) reduce
      ) andThen
        (__ \ "subscriptionTypeAndPSPIDDetails").json.prune andThen
        (__ \ "subscriptionTypeAndPensionSchemePractitionerIdDetails" \ "existingPSPID").json.prune
    ).getOrElse(throw ExpandAcronymTransformerFailed)

  case object ExpandAcronymTransformerFailed extends Exception

  override def details: JsObject = Json.obj(
    "externalId" -> externalId,
    "status" -> status.toString,
    "request" -> expandAcronymTransformer(request),
    "response" -> response
  )
}

object PSPSubscription {
  implicit val formatsPSPSubscription: Format[PSPSubscription] = Json.format[PSPSubscription]
}
