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

import audit.*
import models.*
import models.enumeration.JourneyType.PSP_SUBSCRIPTION
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repository.{DataCacheRepository, MinimalDetailsCacheRepository}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.crypto.PlainText
import org.scalactic.Prettifier.default
import services.JsonCryptoService

import java.time.Instant
import scala.concurrent.Future

class EmailResponseControllerSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  import EmailResponseControllerSpec._

  private val mockAuditService = mock[AuditService]
  private val mockAuthConnector = mock[AuthConnector]

  private val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false).
    overrides(Seq(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuditService].toInstance(mockAuditService),
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository])
    )).build()

  private val crypto = application.injector.instanceOf[JsonCryptoService]
  private val controller = application.injector.instanceOf[EmailResponseController]
  private val encryptedPspId = crypto.jsonCrypto.encrypt(PlainText(psp)).value
  private val encryptedEmail = crypto.jsonCrypto.encrypt(PlainText(email)).value
  private val invalidEncryptedPspId = crypto.jsonCrypto.encrypt(PlainText(invalidPsp)).value
  private val invalidEncryptedEmail = crypto.jsonCrypto.encrypt(PlainText(invalidEmail)).value

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    reset(mockAuthConnector)
    when(mockAuthConnector.authorise[Enrolments](any(), any())(using any(), any()))
      .thenReturn(Future.successful(enrolments))
  }

  "retrieveStatus" must {
    "respond OK when given EmailEvents" in {
      val result = controller.retrieveStatus(PSP_SUBSCRIPTION, requestId, encryptedEmail, encryptedPspId)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) `mustBe` OK
      verify(mockAuditService, times(4)).sendEvent(eventCaptor.capture())(using any(), any())
      eventCaptor.getValue mustEqual EmailAuditEvent(psp, email, Complained, PSP_SUBSCRIPTION, requestId)
    }

    "respond with BAD_REQUEST when not given EmailEvents" in {
      val result = controller.retrieveStatus(PSP_SUBSCRIPTION, requestId, encryptedEmail, encryptedPspId)(fakeRequest.withBody(Json.obj("name" -> "invalid")))

      verify(mockAuditService, never).sendEvent(any())(using any(), any())
      status(result) `mustBe` BAD_REQUEST
    }

    "respond with Forbidden when psp is invalid" in {
      val result = controller.retrieveStatus(PSP_SUBSCRIPTION, requestId, encryptedEmail, invalidEncryptedPspId)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) `mustBe` FORBIDDEN
      contentAsString(result) mustBe "Malformed PSPID"
    }

    "respond with Forbidden when email is invalid" in {
      val result = controller.retrieveStatus(PSP_SUBSCRIPTION, requestId, invalidEncryptedEmail, encryptedPspId)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) `mustBe` FORBIDDEN
      contentAsString(result) mustBe "Malformed Email Address"
    }

    "respond with Forbidden when psp & email are invalid" in {
      val result = controller.retrieveStatus(PSP_SUBSCRIPTION, requestId, invalidEncryptedEmail, invalidEncryptedPspId)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) `mustBe` FORBIDDEN
      contentAsString(result) mustBe "Malformed PSPID & Malformed Email Address"
    }
  }
}

object EmailResponseControllerSpec {
  private val psp = "27654321"
  private val email = "test@test.com"
  private val invalidPsp = "xxxxx"
  private val invalidEmail = "invalid"
  private val requestId = "test-request-id"
  private val fakeRequest = FakeRequest("", "")
  private val enrolments = Enrolments(Set(
    Enrolment("HMRC-PODS-ORG", Seq(
      EnrolmentIdentifier("PSPID", "A0000000")
    ), "Activated", None)
  ))
  private val eventCaptor = ArgumentCaptor.forClass(classOf[EmailAuditEvent])
  private val emailEvents = EmailEvents(Seq(EmailEvent(Sent, Instant.now()), EmailEvent(Delivered, Instant.now()),
    EmailEvent(PermanentBounce, Instant.now()), EmailEvent(Opened, Instant.now()), EmailEvent(Complained, Instant.now())))
}
