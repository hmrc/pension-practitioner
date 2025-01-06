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

package controllers

import connectors.AssociationConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.{DataCacheRepository, MinimalDetailsCacheRepository}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import utils.{AuthUtils, FakePsaSchemeAuthAction, FakePspSchemeAuthAction}

import scala.concurrent.Future

class AssociationControllerSpec
  extends AsyncWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfter {

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
      bind[AssociationConnector].toInstance(mockAssociationConnector),
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[actions.PsaSchemeAuthAction].toInstance(new FakePsaSchemeAuthAction),
      bind[actions.PspSchemeAuthAction].toInstance(new FakePspSchemeAuthAction)
    )

  val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false)
    .overrides(modules: _*).build()

  val controller: AssociationController = application.injector.instanceOf[AssociationController]

  before {
    reset(mockAssociationConnector)
    reset(authConnector)
    AuthUtils.authStub(authConnector)
  }

  "authorise PSP" must {
    "return OK when valid response from IF" in {

      when(mockAssociationConnector.authorisePsp(any(), any())(any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, response.toString))))

      val result = controller.authorisePsp()(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      status(result) mustBe OK
    }

    "throw Upstream5XXResponse on Internal Server Error from IF" in {

      when(mockAssociationConnector.authorisePsp(any(), any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.authorisePsp()(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }

    "throw BadRequestException on invalid request" in {
      recoverToExceptionIf[BadRequestException] {
        controller.authorisePsp()(fakeRequest.withHeaders(("pstr", pstr)))
      } map { result =>
        result.responseCode mustBe BAD_REQUEST
      }
    }

  }

  "authorise PSP SRN" must {
    "return OK when valid response from IF" in {

      when(mockAssociationConnector.authorisePsp(any(), any())(any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, response.toString))))
      AuthUtils.authStubPsa(authConnector)


      val result = controller.authorisePspSrn(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      status(result) mustBe OK
    }

    "throw Upstream5XXResponse on Internal Server Error from IF" in {

      when(mockAssociationConnector.authorisePsp(any(), any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
      AuthUtils.authStubPsa(authConnector)
      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.authorisePspSrn(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  "deAuthorisePsp" must {
    "return OK when valid response from IF" in {

      when(mockAssociationConnector.deAuthorisePsp(any(), any())(any()))
        .thenReturn(Future.successful(
          Right(HttpResponse(OK, response.toString))
        ))

      val result = controller.deAuthorisePsp()(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      status(result) mustBe OK
    }

    "throw Upstream5XXResponse on Internal Server Error from IF" in {

      when(mockAssociationConnector.deAuthorisePsp(any(), any())(any()))
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

  "deAuthorisePspSrn" must {
    val testJson = Json.obj(
      "ceaseNumber" -> AuthUtils.pspId,
      "initiatedIDNumber" -> AuthUtils.psaId
    )
    "return OK when valid response from IF" in {

      when(mockAssociationConnector.deAuthorisePsp(any(), any())(any()))
        .thenReturn(Future.successful(
          Right(HttpResponse(OK, response.toString))
        ))
      AuthUtils.authStubPsa(authConnector)
      val result = controller.deAuthorisePspSrn(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      status(result) mustBe OK
    }

    "throw Upstream5XXResponse on Internal Server Error from IF" in {

      when(mockAssociationConnector.deAuthorisePsp(any(), any())(any()))
        .thenReturn(Future.failed(
          UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)
        ))
      AuthUtils.authStubPsa(authConnector)

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.deAuthorisePspSrn(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return BadRequest if initiatedIDNumber not available in body" in {
      AuthUtils.authStubPsa(authConnector)
      val result = controller.deAuthorisePspSrn(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(Json.parse("{}")))
      status(result) mustBe BAD_REQUEST
    }
    "return BadRequest if initiatedIDNumber is not a string" in {
      AuthUtils.authStubPsa(authConnector)
      val result = controller.deAuthorisePspSrn(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).
        withJsonBody(Json.parse("""{ "initiatedIDNumber": true }""")))
      status(result) mustBe BAD_REQUEST
    }
    "return Forbidden if initiatedIDNumber does not match PspId in session" in {
      AuthUtils.authStubPsa(authConnector)
      val result = controller.deAuthorisePspSrn(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).
        withJsonBody(Json.parse("""{ "initiatedIDNumber": "A9999999" }""")))
      status(result) mustBe FORBIDDEN
    }
  }

  "deAuthorisePspSelf" must {
    val testJson = Json.obj(
      "ceaseNumber" -> AuthUtils.pspId,
      "initiatedIDNumber" -> AuthUtils.pspId
    )
    "return OK when valid response from IF" in {

      when(mockAssociationConnector.deAuthorisePsp(any(), any())(any()))
        .thenReturn(Future.successful(
          Right(HttpResponse(OK, response.toString))
        ))

      val result = controller.deAuthorisePspSelf(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      status(result) mustBe OK
    }

    "throw Upstream5XXResponse on Internal Server Error from IF" in {

      when(mockAssociationConnector.deAuthorisePsp(any(), any())(any()))
        .thenReturn(Future.failed(
          UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)
        ))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.deAuthorisePspSelf(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(testJson))
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return BadRequest if required json body values are missing or are not strings" in {

      val seq = Seq(
        Json.obj(),
        Json.obj(
          "ceaseNumber" -> true,
          "initiatedIDNumber" -> "21000005"
        ),
        Json.obj(
          "initiatedIDNumber" -> "21000005"
        ),
        Json.obj(
          "ceaseNumber" -> "21000005"
        )
      )

      seq.map { json =>
        val result = controller.deAuthorisePspSelf(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(json))
        status(result)
      }.forall(_ == BAD_REQUEST) mustBe true
    }

    "return Forbidden if ceaseNumber or initiatedIDNumber does not match authenticated PSP id" in {
      val seq = Seq(
        Json.obj(
          "ceaseNumber" -> "21000005",
          "initiatedIDNumber" -> "21000006"
        ),
        Json.obj(
          "ceaseNumber" -> "21000006",
          "initiatedIDNumber" -> "21000005"
        )
      )

      seq.map { json =>
        val result = controller.deAuthorisePspSelf(AuthUtils.srn)(fakeRequest.withHeaders(("pstr", pstr)).withJsonBody(json))
        status(result)
      }.forall(_ == FORBIDDEN) mustBe true
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
