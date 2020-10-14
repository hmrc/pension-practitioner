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

import connectors.SubscriptionConnector
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.{AsyncWordSpec, BeforeAndAfter, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import transformations.userAnswersToDes.PSPSubscriptionTransformer
import transformations.userAnswersToDes.PSPSubscriptionTransformerSpec._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionControllerSpec extends AsyncWordSpec with MustMatchers with MockitoSugar with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val fakeRequest = FakeRequest("GET", "/")
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val mockPspSubscriptionTransformer = mock[PSPSubscriptionTransformer]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val response: JsValue = Json.obj("response-key" -> "response-value")

  val modules: Seq[GuiceableModule] =
    Seq(
      bind[AuthConnector].toInstance(authConnector),
      bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
    )

  val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false).
    overrides(modules: _*).build()

  val controller: SubscriptionController = application.injector.instanceOf[SubscriptionController]

  before {
    reset(mockSubscriptionConnector, mockPspSubscriptionTransformer, authConnector)
    when(authConnector.authorise[Option[String]](any(), any())(any(), any()))
    .thenReturn(Future.successful(Some("Ext-137d03b9-d807-4283-a254-fb6c30aceef1")))
  }

  "subscribePsp" must {
    "return OK when valid response from DES" in {

      when(mockSubscriptionConnector.pspSubscription(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(response)))

      val result = controller.subscribePsp()(fakeRequest.withJsonBody(uaIndividualUK))
      status(result) mustBe OK
    }

    "throw Upstream5XXResponse on Internal Server Error from DES" in {

      when(mockSubscriptionConnector.pspSubscription(any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.subscribePsp()(fakeRequest.withJsonBody(uaIndividualUK))
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  "getPspDetails" must {
    "return OK when service returns successfully" in {

      when(mockSubscriptionConnector.getSubscriptionDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(Json.obj())))
      val result = controller.getPspDetails(fakeRequest.withHeaders(("pspId", "A2123456")))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj()
    }

    "return bad request when connector returns BAD_REQUEST" in {

      when(mockSubscriptionConnector.getSubscriptionDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(Left(HttpResponse(BAD_REQUEST, "bad request")))
      )

      val result = controller.getPspDetails(fakeRequest.withHeaders(("pspId", "A2123456")))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "bad request"
      }

    "return not found when connector returns NOT_FOUND" in {

      when(mockSubscriptionConnector.getSubscriptionDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(Left(HttpResponse(NOT_FOUND, "not found"))))

      val result = controller.getPspDetails(fakeRequest.withHeaders(("pspId", "A2123456")))

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "not found"
    }
  }

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }
}
