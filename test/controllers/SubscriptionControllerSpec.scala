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

import connectors.{SchemeConnector, SubscriptionConnector}
import models.enumeration.JourneyType
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
import transformations.userAnswersToDes.PSPSubscriptionTransformer
import transformations.userAnswersToDes.PSPSubscriptionTransformerSpec._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import utils.AuthUtils

import scala.concurrent.Future

class SubscriptionControllerSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val fakeRequest = FakeRequest("GET", "/")
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val mockPspSubscriptionTransformer = mock[PSPSubscriptionTransformer]
  private val mockSchemeConnector = mock[SchemeConnector]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val response: JsValue = Json.obj("response-key" -> "response-value")
  private val deregistrationRequestJson: JsValue = Json.obj("request-key" -> "request-value")

  def listOfSchemesJson(statuses: Seq[String] = Seq("Open", "Open")): JsObject = Json.obj(
    "processingDate" -> "2001-12-17T09 ->30 ->47Z",
    "totalSchemesRegistered" -> "1",
    "schemeDetails" -> Json.arr(
      Json.obj(
        "name" -> "abcdefghi",
        "referenceNumber" -> "S1000000456",
        "schemeStatus" -> JsString(statuses.head),
        "pstr" -> "10000678RE",
        "relationShip" -> "Primary",
        "underAppeal" -> "Yes"
      ),
      Json.obj(
        "name" -> "abcdefghi",
        "referenceNumber" -> "S1000000456",
        "schemeStatus" -> JsString(statuses(1)),
        "pstr" -> "10000678RE",
        "relationShip" -> "Primary",
        "underAppeal" -> "Yes"
      )
    )
  )

  def noSchemesJson: JsObject = Json.obj(
    "processingDate" -> "2001-12-17T09 ->30 ->47Z",
    "totalSchemesRegistered" -> "1")


  val modules: Seq[GuiceableModule] =
    Seq(
      bind[AuthConnector].toInstance(authConnector),
      bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
      bind[SchemeConnector].toInstance(mockSchemeConnector),
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository])
    )

  val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false).
    overrides(modules*).build()

  val controller: SubscriptionController = application.injector.instanceOf[SubscriptionController]

  before {
    reset(mockSubscriptionConnector)
    reset(mockPspSubscriptionTransformer)
    reset(authConnector)
    AuthUtils.authStub(authConnector)
  }

  "subscribePsp" must {
    "return OK when valid response from API" in {
      reset(authConnector)
      AuthUtils.noEnrolmentAuthStub(authConnector)
      when(mockSubscriptionConnector.pspSubscription(any(), any())(using any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, response.toString))))

      val result = controller.subscribePsp(JourneyType.PSP_SUBSCRIPTION)(fakeRequest.withJsonBody(uaIndividualUK))
      status(result) `mustBe` OK
    }

    "throw Upstream5XXResponse on Internal Server Error from API" in {
      reset(authConnector)
      AuthUtils.noEnrolmentAuthStub(authConnector)
      when(mockSubscriptionConnector.pspSubscription(any(), any())(using any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.subscribePsp(JourneyType.PSP_SUBSCRIPTION)(fakeRequest.withJsonBody(uaIndividualUK))
      } map {
        _.statusCode `mustBe` INTERNAL_SERVER_ERROR
      }
    }

  }

  "getPspDetailsSelf" must {
    "return OK when service returns successfully" in {

      when(mockSubscriptionConnector.getSubscriptionDetails(any())(using any()))
        .thenReturn(Future.successful(Right(Json.obj())))
      val result = controller.getPspDetailsSelf(fakeRequest)

      status(result) `mustBe` OK
      contentAsJson(result) `mustBe` Json.obj()
    }

    "return bad request when connector returns BAD_REQUEST" in {

      when(mockSubscriptionConnector.getSubscriptionDetails(any())(using any()))
        .thenReturn(Future.successful(Left(HttpResponse(BAD_REQUEST, "bad request")))
        )

      val result = controller.getPspDetailsSelf(fakeRequest)

      status(result) `mustBe` BAD_REQUEST
      contentAsString(result) `mustBe` "bad request"
    }

    "return not found when connector returns NOT_FOUND" in {

      when(mockSubscriptionConnector.getSubscriptionDetails(any())(using any()))
        .thenReturn(Future.successful(Left(HttpResponse(NOT_FOUND, "not found"))))

      val result = controller.getPspDetailsSelf(fakeRequest)

      status(result) `mustBe` NOT_FOUND
      contentAsString(result) `mustBe` "not found"
    }
  }

  "deregisterPspSelf" must {
    "return OK when valid response from API" in {

      when(mockSubscriptionConnector.pspDeregistration(any(), any())(using any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, response.toString))))

      val result = controller.deregisterPspSelf(fakeRequest.withJsonBody(deregistrationRequestJson))
      status(result) `mustBe` OK
    }

    "throw Upstream5XXResponse on Internal Server Error from API" in {

      when(mockSubscriptionConnector.pspDeregistration(any(), any())(using any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.deregisterPspSelf(fakeRequest.withJsonBody(deregistrationRequestJson))
      } map {
        _.statusCode `mustBe` INTERNAL_SERVER_ERROR
      }
    }

  }

  "canDeregisterSelf" must {
    "return OK and false when canDeregister called with psa ID having some schemes" in {
      when(mockSchemeConnector.listOfSchemes(using any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemesJson())))
      val result = controller.canDeregisterSelf(fakeRequest)

      status(result) `mustBe` OK
      contentAsJson(result) mustEqual JsBoolean(false)
    }

    "return OK and true when canDeregister called with psa ID having no scheme detail item at all" in {
      when(mockSchemeConnector.listOfSchemes(using any(), any()))
        .thenReturn(Future.successful(Right(noSchemesJson)))
      val result = controller.canDeregisterSelf(fakeRequest)

      status(result) `mustBe` OK
      contentAsJson(result) mustEqual JsBoolean(true)
    }

    "return OK and false when canDeregister called with psa ID having only wound-up schemes" in {
      when(mockSchemeConnector.listOfSchemes(using any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemesJson(Seq("Wound-up", "Deregistered")))))
      val result = controller.canDeregisterSelf(fakeRequest)

      status(result) `mustBe` OK
      contentAsJson(result) mustEqual JsBoolean(true)
    }

    "return OK and false when canDeregister called with psp ID having both wound-up schemes and non-wound-up schemes" in {
      when(mockSchemeConnector.listOfSchemes(using any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemesJson(Seq("Open", "Wound-up")))))
      val result = controller.canDeregisterSelf(fakeRequest)

      status(result) `mustBe` OK
      contentAsJson(result) mustEqual JsBoolean(false)
    }

    "return http exception when non OK httpresponse returned" in {
      when(mockSchemeConnector.listOfSchemes(using any(), any()))
        .thenReturn(Future.successful(Left(HttpResponse(BAD_REQUEST, "bad request"))))
      val result = controller.canDeregisterSelf(fakeRequest)
      status(result) `mustBe` BAD_REQUEST
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
