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

import akka.stream.Materializer
import connectors.RegistrationConnector
import models.registerWithId.RegisterWithIdResponse
import models.registerWithoutId.{OrganisationRegistrant, RegisterWithoutIdIndividualRequest, RegisterWithoutIdResponse}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncWordSpec, BeforeAndAfter, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, contentAsJson, status, _}
import repository.DataCacheRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._

import scala.concurrent.Future

class RegistrationControllerSpec extends AsyncWordSpec with MustMatchers with MockitoSugar with BeforeAndAfter {

  import RegistrationControllerSpec._

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val mockDataCacheRepository = mock[DataCacheRepository]

  private val modules: Seq[GuiceableModule] =
    Seq(
      bind[AuthConnector].toInstance(authConnector),
      bind[RegistrationConnector].toInstance(mockRegistrationConnector),
      bind[DataCacheRepository].toInstance(mockDataCacheRepository)
    )

  private val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false).
    overrides(modules: _*).build()
  implicit val mat: Materializer = application.materializer

  before {
    reset(mockRegistrationConnector, authConnector)
    when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(externalId))
  }
  private val controller = application.injector.instanceOf[RegistrationController]

  private val fakeRequestWithNino = FakeRequest("GET", "/").withHeaders("nino" -> nino)
  private val fakeRequestWithUtr = FakeRequest("GET", "/").withHeaders("utr" -> utr).withJsonBody(requestBodyOrg)

  "registerWithIdIndividual " must {

    val mandatoryRequestData = Json.obj("regime" -> "PODP", "requiresNameMatch" -> false, "isAnAgent" -> false)

    "return OK when the registration with id is successful for Individual" in {

      val successResponse: RegisterWithIdResponse = registerWithIdIndividualResponse.as[RegisterWithIdResponse]

      when(mockRegistrationConnector.registerWithIdIndividual(
        Matchers.eq(externalId), Matchers.eq(nino), Matchers.eq(mandatoryRequestData)
      )(any(), any(), any())).thenReturn(Future.successful(successResponse))

      val result = controller.registerWithIdIndividual(fakeRequestWithNino)

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual Json.toJson(successResponse)
      }
    }

    "throw BadRequestException when nino is not present in the header" in {

      val successResponse: RegisterWithIdResponse = registerWithIdIndividualResponse.as[RegisterWithIdResponse]

      when(mockRegistrationConnector.registerWithIdIndividual(
        Matchers.eq(externalId), Matchers.eq(nino), Matchers.eq(mandatoryRequestData)
      )(any(), any(), any())).thenReturn(Future.successful(successResponse))

      recoverToExceptionIf[BadRequestException] {
        controller.registerWithIdIndividual(FakeRequest("", ""))
      } map { response =>
        response.responseCode mustBe BAD_REQUEST
        response.message mustBe "Bad Request with missing nino for register with id call for individual"
      }
    }

    "throw Upstream5XXResponse on Internal Server Error from DES" in {

      when(mockRegistrationConnector.registerWithIdIndividual(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.registerWithIdIndividual(fakeRequestWithNino)
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "registerWithIdOrganisation " must {

    val mandatoryRequestData = Json.parse(
      """
        |{"regime":"PODP","isAnAgent":false,"organisation":{"organisationName":"Test Ltd","organisationType":"Corporate Body"},"requiresNameMatch":true}
      """.stripMargin
    )

    "return OK when the registration with id is successful for Individual" in {

      val successResponse: RegisterWithIdResponse = registerWithIdOrganisationResponse.as[RegisterWithIdResponse]

      when(mockRegistrationConnector.registerWithIdOrganisation(
        Matchers.eq(externalId), Matchers.eq(utr), Matchers.eq(mandatoryRequestData)
      )(any(), any(), any())).thenReturn(Future.successful(successResponse))

      val result = controller.registerWithIdOrganisation(fakeRequestWithUtr)

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual Json.toJson(successResponse)
      }
    }

    "throw BadRequestException when utr is not present in the header" in {

      val successResponse: RegisterWithIdResponse = registerWithIdOrganisationResponse.as[RegisterWithIdResponse]

      when(mockRegistrationConnector.registerWithIdOrganisation(
        Matchers.eq(externalId), Matchers.eq(nino), Matchers.eq(mandatoryRequestData)
      )(any(), any(), any())).thenReturn(Future.successful(successResponse))

      recoverToExceptionIf[BadRequestException] {
        controller.registerWithIdOrganisation(FakeRequest("", ""))
      } map { response =>
        response.responseCode mustBe BAD_REQUEST
        response.message mustBe "Bad Request with missing utr or request body for register with id call for organisation"
      }
    }

    "throw Upstream5XXResponse on Internal Server Error from DES" in {

      when(mockRegistrationConnector.registerWithIdOrganisation(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "CONFLICT", CONFLICT, CONFLICT)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        controller.registerWithIdOrganisation(fakeRequestWithUtr)
      } map {
        _.statusCode mustBe CONFLICT
      }
    }
  }

  "registrationNoIdIndividual " must {

    val requestBody = Json.parse(
      """
        |{
        |  "firstName": "John",
        |  "lastName": "Smith",
        |  "dateOfBirth": "1990-04-03",
        |  "address": {
        |    "addressLine1": "31 Myers Street",
        |    "addressLine2": "Haddonfield",
        |    "addressLine3": "Illinois",
        |    "addressLine4": "USA",
        |    "country": "US"
        |  }
        |}
        |
      """.stripMargin
    )
    val fakeRequestWithNoIdIndBody = FakeRequest("POST", "/").withJsonBody(requestBody)

    "return OK when the registration with id is successful for Individual" in {

      val successResponse: RegisterWithoutIdResponse = RegisterWithoutIdResponse("XE0001234567890", "1234567890")

      when(mockRegistrationConnector.registrationNoIdIndividual(
        Matchers.eq(externalId), Matchers.eq(requestBody.as[RegisterWithoutIdIndividualRequest]))(any(), any(), any()))
        .thenReturn(Future.successful(successResponse))

      val result = call(controller.registrationNoIdIndividual, fakeRequestWithNoIdIndBody)

      ScalaFutures.whenReady(result) { _ =>
        verify(mockRegistrationConnector, times(1)).registrationNoIdIndividual(any(), any())(any(), any(), any())
        status(result) mustBe OK
      }
    }

    "throw Upstream5XXResponse on Internal Server Error from DES" in {

      when(mockRegistrationConnector.registrationNoIdIndividual(any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        call(controller.registrationNoIdIndividual, fakeRequestWithNoIdIndBody)
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "registrationNoIdOrganisation " must {

    val requestBody = Json.parse(
      """
        |{
        |  "regime": "PODA",
        |  "acknowledgementReference": "12345678901234567890123456789012",
        |  "isAnAgent": false,
        |  "isAGroup": false,
        |  "organisation": {
        |    "organisationName": "John"
        |  },
        |  "address": {
        |    "addressLine1": "31 Myers Street",
        |    "addressLine2": "Haddonfield",
        |    "addressLine3": "Illinois",
        |    "addressLine4": "USA",
        |    "country": "US"
        |  },
        |  "contactDetails": {
        |    "phoneNumber": "01332752856"
        |  }
        |}
      """.stripMargin
    )
    val fakeRequestWithNoIdOrgBody = FakeRequest("POST", "/").withJsonBody(requestBody)

    "return OK when the registration with id is successful for Individual" in {

      val successResponse: RegisterWithoutIdResponse = RegisterWithoutIdResponse("XE0001234567890", "1234567890")

      when(mockRegistrationConnector.registrationNoIdOrganisation(
        Matchers.eq(externalId), Matchers.eq(requestBody.as[OrganisationRegistrant]))(any(), any(), any()))
        .thenReturn(Future.successful(successResponse))

      val result = call(controller.registrationNoIdOrganisation, fakeRequestWithNoIdOrgBody)

      ScalaFutures.whenReady(result) { _ =>
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(any(), any())(any(), any(), any())
        status(result) mustBe OK
      }
    }

    "throw Upstream5XXResponse on Internal Server Error from DES" in {

      when(mockRegistrationConnector.registrationNoIdOrganisation(any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(message = "Internal Server Error", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      recoverToExceptionIf[UpstreamErrorResponse] {
        call(controller.registrationNoIdOrganisation, fakeRequestWithNoIdOrgBody)
      } map {
        _.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}

object RegistrationControllerSpec {

  private val nino = "test-nino"
  private val utr = "test-utr"
  private val externalId = "test-externalId"

  private val registerWithIdIndividualResponse = Json.parse(
    """{
      |  "safeId": "XE0001234567890",
      |  "sapNumber": "1234567890",
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
      |}""".stripMargin
  )

  private val registerWithIdOrganisationResponse = Json.parse(
    """
      |{
      |  "safeId": "XE0001234567890",
      |  "sapNumber": "1234567890",
      |  "isAnIndividual": false,
      |  "organisation": {
      |    "organisationName": "Test Ltd",
      |    "isAGroup": false,
      |    "organisationType": "LLP"
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
    """.stripMargin
  )

  private val requestBodyOrg = Json.obj(
    "organisationName" -> "Test Ltd",
    "organisationType" -> "Corporate Body"
  )
}
