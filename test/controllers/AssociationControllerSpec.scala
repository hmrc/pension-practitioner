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

import connectors.AssociationConnector
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
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociationControllerSpec extends AsyncWordSpec with MustMatchers with MockitoSugar with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val fakeRequest = FakeRequest("POST", "/")
  private val mockAssociationConnector = mock[AssociationConnector]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val pstr: String = "pstr"
  private val response: JsValue = Json.obj("response-key" -> "response-value")
  private val testJson: JsValue = Json.obj("key" -> "value")

  val modules: Seq[GuiceableModule] =
    Seq(
      bind[AuthConnector].toInstance(authConnector),
      bind[AssociationConnector].toInstance(mockAssociationConnector)
    )

  val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false).
    overrides(modules: _*).build()

  val controller: AssociationController = application.injector.instanceOf[AssociationController]

  before {
    reset(mockAssociationConnector, authConnector)
    when(authConnector.authorise[Option[String]](any(), any())(any(), any()))
      .thenReturn(Future.successful(Some("Ext-137d03b9-d807-4283-a254-fb6c30aceef1")))
  }

  "authorise PSP" must {
    "return OK when valid response from IF" in {

      when(mockAssociationConnector.authorisePsp(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, response.toString)))

      val result = controller.authorisePsp()(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      status(result) mustBe OK
    }

    "throw Upstream5XXResponse on Internal Server Error from IF" in {

      when(mockAssociationConnector.authorisePsp(any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.authorisePsp()(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  "deAuthorisePsp" must {
    "return OK when valid response from IF" in {

      when(mockAssociationConnector.deAuthorisePsp(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(
          HttpResponse(OK, response.toString)
        ))

      val result = controller.deAuthorisePsp()(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      status(result) mustBe OK
    }

    "throw Upstream5XXResponse on Internal Server Error from IF" in {

      when(mockAssociationConnector.deAuthorisePsp(any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(
          UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)
        ))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.deAuthorisePsp()(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
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
