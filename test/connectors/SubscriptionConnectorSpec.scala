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

package connectors

import audit.AuditService
import audit.PSPSubscription
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.AsyncWordSpec
import org.scalatest.EitherValues
import org.scalatest.MustMatchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http._
import utils.WireMockHelper

class SubscriptionConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper
  with EitherValues with MockitoSugar {

  import SubscriptionConnectorSpec._

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  private implicit lazy val rh: RequestHeader = FakeRequest("", "")

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  private val mockAuditService = mock[AuditService]
  private val mockHeaderUtils = mock[HeaderUtils]

  private lazy val connector: SubscriptionConnector = injector.instanceOf[SubscriptionConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(mockAuditService),
      bind[HeaderUtils].toInstance(mockHeaderUtils)
    )

  private val pspSubscriptionUrl = "/pension-online/subscriptions/psp"
  private val getPspDetailsUrl = "/pension-online/subscriptions/getDetails"
  private val pspId = "psp-id"

  private val externalId = "id"

  private val eventCaptor = ArgumentCaptor.forClass(classOf[PSPSubscription])

  when(mockHeaderUtils.integrationFrameworkHeader).thenReturn(Nil)

  "pspSubscription" must {

    "return successfully when DES has returned OK" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            ok
              .withBody(Json.stringify(JsString("response")))
          )
      )

      connector.pspSubscription(externalId, data) collect {
        case Right(_) => succeed
      }
    }

    "return BAD REQUEST when DES has returned BadRequestException" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            badRequest()
          )
      )

      connector.pspSubscription(externalId, data).collect {
        case Left(_: BadRequestException) => succeed
      }
    }

    "return NOT FOUND when DES has returned NotFoundException" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            notFound()
          )
      )

      connector.pspSubscription(externalId, data).collect {
        case Left(_: NotFoundException) => succeed
      }
    }

    "throw Upstream5xxResponse when ETMP has returned Internal Server Error" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            serverError()
          )
      )

      recoverToSucceededIf[Upstream5xxResponse](connector.pspSubscription(externalId, data))
    }

    "send a PSPSubscription audit event on success" in {
      val response = JsString("mock response")
      val data = Json.obj(fields = "Id" -> "value")
      Mockito.reset(mockAuditService)
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(response))
          )
      )

      connector.pspSubscription(externalId, data).map {_ =>
        verify(mockAuditService, times(1)).sendEvent(eventCaptor.capture())(any(), any())
          eventCaptor.getValue mustEqual PSPSubscription(externalId, Status.OK, data, Some(response))
      }
    }
  }

  "getPspDetails" must {
    "return user answer json when successful response returned from API" in {
      server.stubFor(
        get(urlEqualTo(getPspDetailsUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(pspDetailsResponse.toString())
          )
      )

      connector.getSubscriptionDetails(pspId).map { response =>
        response.right.get mustBe pspUserAnswers
      }
    }

    "return a BadRequestException for a 400 INVALID_IDVALUE response" in {
      server.stubFor(
        get(urlEqualTo(getPspDetailsUrl))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_IDVALUE"))
          )
      )


      connector.getSubscriptionDetails(pspId).map { response =>
        response.left.get.status mustEqual BAD_REQUEST
        response.left.get.body must include("INVALID_IDVALUE")
      }
    }

    "return Not Found - 404" in {
      server.stubFor(
        get(urlEqualTo(getPspDetailsUrl))
          .willReturn(
            notFound
              .withBody(errorResponse("NOT_FOUND"))
          )
      )

      connector.getSubscriptionDetails(pspId).map { response =>
        response.left.get.status mustEqual NOT_FOUND
        response.left.get.body must include("NOT_FOUND")
      }
    }

    "throw Upstream4XX for server unavailable - 403" in {

      server.stubFor(
        get(urlEqualTo(getPspDetailsUrl))
          .willReturn(
            forbidden
              .withBody(errorResponse("FORBIDDEN"))
          )
      )

      connector.getSubscriptionDetails(pspId).map { response =>
        response.left.get.status mustEqual FORBIDDEN
        response.left.get.body must include("FORBIDDEN")
      }
    }

    "throw Upstream5XX for internal server error - 500 and log the event as error" in {

      server.stubFor(
        get(urlEqualTo(getPspDetailsUrl))
          .willReturn(
            serverError
              .withBody(errorResponse("SERVER_ERROR"))
          )
      )

      connector.getSubscriptionDetails(pspId).map { response =>
        response.left.get.status mustEqual INTERNAL_SERVER_ERROR
        response.left.get.body must include("SERVER_ERROR")
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

object SubscriptionConnectorSpec {

  private val pspDetailsResponse = Json.obj(
    "subscriptionTypeAndPSPIDDetails" -> Json.obj(
      "applicationDate" -> "2019-10-17T00 ->11 ->39.789Z",
      "subscriptionType" -> "Creation",
      "existingPSPID" -> "Yes",
      "pspid" -> "17948279"),
    "legalEntityAndCustomerID" -> Json.obj(
      "legalStatus" -> "Individual",
      "customerType" -> "UK",
      "idType" -> "NINO",
      "idNumber" -> "AB277252B"),
    "individualDetails" -> Json.obj(
      "firstName" -> "Anthony",
      "lastName" -> "Hood"),
    "correspondenceAddressDetails" -> Json.obj(
      "nonUKAddress" -> false,
      "addressLine1" -> "24 Trinity Street",
      "addressLine2" -> "Telford",
      "addressLine3" -> "Shropshire",
      "countryCode" -> "GB",
      "postalCode" -> "TF3 4ER"),
    "correspondenceContactDetails" -> Json.obj(
      "telephone" -> "0044-09876542312",
      "mobileNumber" -> "0044-09876542312",
      "fax" -> "0044-09876542312",
      "email" -> "abc@hmrc.gsi.gov.uk"),
    "declaration" -> Json.obj(
      "pspDeclarationBox1" -> false)
  )

  private val pspUserAnswers = Json.obj(
    "individualDetails" -> Json.obj(
      "firstName" -> "Anthony",
      "lastName" -> "Hood"),
    "phone" -> "0044-09876542312",
    "registrationInfo" -> Json.obj(
      "customerType" -> "UK",
      "idType" -> "NINO",
      "legalStatus" -> "Individual",
      "idNumber" -> "AB277252B"),
    "contactAddress" -> Json.obj(
      "country" -> "GB",
      "postcode" -> "TF3 4ER",
      "addressLine1" -> "24 Trinity Street",
      "addressLine2" -> "Telford",
      "addressLine3" -> "Shropshire"),
    "existingPSP" -> Json.obj(
      "existingPSPId" -> "17948279",
      "isExistingPSP" -> "Yes"),
    "email" -> "abc@hmrc.gsi.gov.uk")
}
