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

package connectors

import audit.{AuditService, PSPRegistration}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.registerWithId.RegisterWithIdResponse
import models.registerWithoutId._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import repository.{DataCacheRepository, MinimalDetailsCacheRepository}
import uk.gov.hmrc.http._
import utils.WireMockHelper

class RegistrationConnectorSpec
  extends AsyncWordSpec
    with Matchers
    with WireMockHelper
    with EitherValues
    with MockitoSugar {

  import RegistrationConnectorSpec._

  private implicit lazy val rh: RequestHeader = FakeRequest("", "")

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  private val mockAuditService = mock[AuditService]
  private val mockHeaderUtils = mock[HeaderUtils]

  private lazy val connector: RegistrationConnector = injector.instanceOf[RegistrationConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(mockAuditService),
      bind[HeaderUtils].toInstance(mockHeaderUtils),
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository])
    )

  private val registerIndividualWithIdUrl = s"/registration/individual/nino/$testNino"
  private val registerOrganisationWithIdUrl = s"/registration/organisation/utr/$testUtr"
  private val registerOrganisationWithoutIdUrl = "/registration/02.00.00/organisation"
  private val registerIndividualWithoutIdUrl = "/registration/02.00.00/individual"

  private val eventCaptor = ArgumentCaptor.forClass(classOf[PSPRegistration])

  private val testRegisterDataIndividual: JsObject =
    Json.obj("regime" -> "PODP", "requiresNameMatch" -> false, "isAnAgent" -> false)

  when(mockHeaderUtils.desHeaderWithoutCorrelationId).thenReturn(Nil)

  "registerWithIdIndividual" must {
    "handle OK (200) and return the correct response" in {
      server.stubFor(
        post(urlEqualTo(registerIndividualWithIdUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(registerIndividualResponse.toString())
          )
      )

      connector.registerWithIdIndividual(externalId, testNino, testRegisterDataIndividual).map {
        response =>
          response `mustBe` Right(registerIndividualResponse.as[RegisterWithIdResponse])
      }
    }

    "return a BadRequestException for a 400 INVALID_NINO response" in {
      server.stubFor(
        post(urlEqualTo(registerIndividualWithIdUrl))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_NINO"))
          )
      )

      connector.registerWithIdIndividual(externalId, testNino, testRegisterDataIndividual).map {
        response =>
          response.left.value.responseCode `mustBe` BAD_REQUEST
          response.left.value.message must include("INVALID_NINO")
      }
    }

    "return Upstream5xxResponse for a 503 Service Unavailable " in {
      server.stubFor(
        post(urlEqualTo(registerIndividualWithIdUrl))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse](
        connector.registerWithIdIndividual(externalId, testNino, testRegisterDataIndividual)
      ) map {
        ex =>
          ex.statusCode `mustBe` SERVICE_UNAVAILABLE
      }
    }

    "send a PSPRegistration audit event on success" in {
      reset(mockAuditService)
      server.stubFor(
        post(urlEqualTo(registerIndividualWithIdUrl))
          .withRequestBody(equalTo(Json.stringify(testRegisterDataIndividual)))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(registerIndividualResponse))
          )
      )
      connector.registerWithIdIndividual(externalId, testNino, testRegisterDataIndividual).map { _ =>
        verify(mockAuditService, times(1)).sendEvent(eventCaptor.capture())(using any(), any())
        eventCaptor.getValue mustEqual PSPRegistration(
          withId = true,
          externalId = externalId,
          psaType = "Individual",
          found = true,
          isUk = Some(true),
          status = Status.OK,
          request = testRegisterDataIndividual,
          response = Some(Json.toJson(registerIndividualResponse.as[RegisterWithIdResponse]))
        )
      }
    }
  }

  "registerWithIdOrganisation" must {
    "handle OK (200) and return the correct response" in {
      server.stubFor(
        post(urlEqualTo(registerOrganisationWithIdUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(registerOrganisationResponse.toString())
          )
      )

      connector.registerWithIdOrganisation(externalId, testUtr, testRegisterDataOrganisation).map {
        response =>
          response.value `mustBe` registerOrganisationResponse.as[RegisterWithIdResponse]
      }
    }

    "return a BadRequestException for a 400 INVALID_UTR response" in {
      server.stubFor(
        post(urlEqualTo(registerOrganisationWithIdUrl))
          .willReturn(
            badRequest()
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_UTR"))
          )
      )

      connector.registerWithIdOrganisation(externalId, testUtr, testRegisterDataOrganisation) map {
        res =>
          res.left.value.responseCode `mustBe` BAD_REQUEST
          res.left.value.message must include("INVALID_UTR")
      }
    }

    "return Upstream4xxResponse for a 409 Conflict " in {
      server.stubFor(
        post(urlEqualTo(registerOrganisationWithIdUrl))
          .willReturn(
            aResponse()
              .withStatus(CONFLICT)
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse](
        connector.registerWithIdOrganisation(externalId, testUtr, testRegisterDataOrganisation)
      ) map {
        ex =>
          ex.statusCode `mustBe` CONFLICT
      }
    }

    "send a PSPRegistration audit event on success" in {
      reset(mockAuditService)
      server.stubFor(
        post(urlEqualTo(registerOrganisationWithIdUrl))
          .withRequestBody(equalTo(Json.stringify(testRegisterDataOrganisation)))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(registerOrganisationResponse))
          )
      )

      connector.registerWithIdOrganisation(externalId, testUtr, testRegisterDataOrganisation).map { _ =>
        verify(mockAuditService, times(1)).sendEvent(eventCaptor.capture())(using any(), any())
        eventCaptor.getValue mustEqual PSPRegistration(
          withId = true,
          externalId = externalId,
          psaType = psaType,
          found = true,
          isUk = Some(true),
          status = Status.OK,
          request = testRegisterDataOrganisation,
          response = Some(Json.toJson(registerOrganisationResponse.as[RegisterWithIdResponse]))
        )
      }
    }
  }

  "registrationNoIdOrganisation" must {
    "handle OK (200) and return the correct response" in {
      server.stubFor(
        post(urlEqualTo(registerOrganisationWithoutIdUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(registerWithoutIdResponseJson.toString())
          )
      )

      connector.registrationNoIdOrganisation(externalId, organisationRegistrant).map {
        response =>
          response `mustBe` Right(registerWithoutIdResponse)
      }
    }

    "send a PSPRegistration audit event on success" in {
      reset(mockAuditService)
      when(mockHeaderUtils.getCorrelationId).thenReturn(testCorrelationId)
      val regWithoutIdRequest =
        Json.toJson(organisationRegistrant)(
          using OrganisationRegistrant.writesOrganisationRegistrantRequest(testCorrelationId)
        )
      server.stubFor(
        post(urlEqualTo(registerOrganisationWithoutIdUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(registerWithoutIdResponseJson))
          )
      )

      connector.registrationNoIdOrganisation(externalId, organisationRegistrant).map { _ =>
        verify(mockAuditService, times(1)).sendEvent(eventCaptor.capture())(using any(), any())
        eventCaptor.getValue mustEqual PSPRegistration(
          withId = false,
          externalId = externalId,
          psaType = "Organisation",
          found = true,
          isUk = Some(false),
          status = Status.OK,
          request = regWithoutIdRequest,
          response = Some(registerWithoutIdResponseJson)
        )
      }
    }
  }

  "registrationNoIdIndividual" must {
    "handle OK (200) and return the correct response" in {
      server.stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(registerWithoutIdResponseJson.toString())
          )
      )

      connector.registrationNoIdIndividual(externalId, registerIndividualWithoutIdRequest).map {
        response =>
          response `mustBe` Right(registerWithoutIdResponse)
      }
    }

    "send a PSPRegistration audit event on success" in {
      reset(mockAuditService)
      when(mockHeaderUtils.getCorrelationId).thenReturn(testCorrelationId)
      server.stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(registerWithoutIdResponseJson))
          )
      )

      connector.registrationNoIdIndividual(externalId, registerIndividualWithoutIdRequest).map { _ =>
        verify(mockAuditService, times(1)).sendEvent(eventCaptor.capture())(using any(), any())
        eventCaptor.getValue mustEqual PSPRegistration(
          withId = false,
          externalId = externalId,
          psaType = "Individual",
          found = true,
          isUk = Some(false),
          status = Status.OK,
          request = registerIndividualWithoutIdRequestJson,
          response = Some(registerWithoutIdResponseJson)
        )
      }
    }
  }
}

object RegistrationConnectorSpec {
  private val testNino: String = "AB123456C"
  private val testUtr: String = "1234567890"
  private val externalId = "test-external-id"

  private val psaType = "LLP"
  private val testCorrelationId = "testCorrelationId"

  private val testRegisterDataOrganisation: JsObject = Json.obj(
    "regime" -> "PODP",
    "requiresNameMatch" -> false,
    "isAnAgent" -> false,
    "organisation" -> Json.obj(
      "organisationName" -> "Test Ltd",
      "organisationType" -> "LLP"
    ))

  private val registerIndividualResponse: JsValue = Json.parse(
    """
      |{
      |  "safeId": "XE0001234567890",
      |  "sapNumber": "1234567890",
      |  "agentReferenceNumber": "AARN1234567",
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnASAgent": false,
      |  "isAnIndividual": true,
      |  "individual": {
      |    "firstName": "Stephen",
      |    "lastName": "Wood",
      |    "dateOfBirth": "1990-04-03"
      |  },
      |  "address": {
      |    "addressLine1": "100 SuttonStreet",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": "Surrey",
      |    "addressLine4": "London",
      |    "postalCode": "DH14EJ",
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {
      |    "primaryPhoneNumber": "01332752856",
      |    "secondaryPhoneNumber": "07782565326",
      |    "faxNumber": "01332754256",
      |    "emailAddress": "stephen@manncorpone.co.uk"
      |  }
      |}
      |
    """.stripMargin)

  private val registerOrganisationResponse: JsValue = Json.parse(
    s"""{
       |  "safeId": "XE0001234567890",
       |  "sapNumber": "1234567890",
       |  "isAnIndividual": false,
       |  "organisation": {
       |    "organisationName": "Test Ltd",
       |    "isAGroup": false,
       |    "organisationType": "$psaType"
       |  },
       |  "address": {
       |    "addressLine1": "100 SuttonStreet",
       |    "addressLine2": "Wokingham",
       |    "addressLine3": "Surrey",
       |    "addressLine4": "London",
       |    "postalCode": "DH14EJ",
       |    "countryCode": "GB"
       |  },
       |  "contactDetails": {
       |    "primaryPhoneNumber": "01332752856",
       |    "secondaryPhoneNumber": "07782565326",
       |    "faxNumber": "01332754256",
       |    "emailAddress": "stephen@manncorpone.co.uk"
       |  }
       |}
       |
    """.stripMargin)

  private val organisationRegistrant = OrganisationRegistrant(
    OrganisationName("Name"),
    Address("addressLine1", "addressLine2", None, None, None, "US")
  )

  private val registerWithoutIdResponse = RegisterWithoutIdResponse(
    "XE0001234567890",
    "1234567890"
  )

  private val registerWithoutIdResponseJson: JsValue = Json.toJson(registerWithoutIdResponse)

  private val registerIndividualWithoutIdRequest =
    RegisterWithoutIdIndividualRequest(
      "test-first-name",
      "test-last-name",
      Address(
        "test-address-line-1",
        "test-address-line-2",
        None,
        None,
        None,
        "XX"
      )
    )

  private val registerIndividualWithoutIdRequestJson: JsValue =
    Json.toJson(registerIndividualWithoutIdRequest)(
      using RegisterWithoutIdIndividualRequest.writesRegistrationNoIdIndividualRequest(testCorrelationId)
    )

  private def errorResponse(code: String): String = {
    Json.obj(
      "code" -> code,
      "reason" -> s"Reason for $code"
    ).toString()
  }
}
