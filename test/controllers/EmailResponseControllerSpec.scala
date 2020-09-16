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

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import audit.AuditService
import audit.EmailAuditEvent
import models.enumeration.JourneyType.PSP_SUBSCRIPTION
import models.Sent
import models._
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.scalatest.AsyncWordSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.crypto.PlainText

import scala.concurrent.Future

class EmailResponseControllerSpec extends AsyncWordSpec with MustMatchers with MockitoSugar with BeforeAndAfterEach {

  import EmailResponseControllerSpec._

  private val mockAuditService = mock[AuditService]
  private val mockAuthConnector = mock[AuthConnector]

  private val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false).
    overrides(Seq(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuditService].toInstance(mockAuditService)
    )).build()

  private val injector = application.injector
  private val controller = injector.instanceOf[EmailResponseController]
  private val encryptedPspId = URLEncoder
    .encode(injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(psp)).value, StandardCharsets.UTF_8.toString)
  private val encryptedEmail = URLEncoder
    .encode( injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(email)).value, StandardCharsets.UTF_8.toString)

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuditService, mockAuthConnector)
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
      .thenReturn(Future.successful(enrolments))
  }

  "EmailResponseController" must {

    "respond OK when given EmailEvents" in {
      val result = controller.retrieveStatus(PSP_SUBSCRIPTION, requestId, encryptedEmail, encryptedPspId)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) mustBe OK
      verify(mockAuditService, times(4)).sendEvent(eventCaptor.capture())(any(), any())
      eventCaptor.getValue mustEqual EmailAuditEvent(psp, email, Complained, PSP_SUBSCRIPTION, requestId)
    }

    "respond with BAD_REQUEST when not given EmailEvents" in {
      val result = controller.retrieveStatus(PSP_SUBSCRIPTION, requestId, encryptedEmail, encryptedPspId)(fakeRequest.withBody(Json.obj("name" -> "invalid")))

      verify(mockAuditService, never()).sendEvent(any())(any(), any())
      status(result) mustBe BAD_REQUEST
    }
  }
}

object EmailResponseControllerSpec {
  private val psp = "A7654321"
  private val email = "test@test.com"
  private val requestId = "test-request-id"
  private val fakeRequest = FakeRequest("", "")
  private val enrolments = Enrolments(Set(
    Enrolment("HMRC-PODS-ORG", Seq(
      EnrolmentIdentifier("PSPID", "A0000000")
    ), "Activated", None)
  ))
  private val eventCaptor = ArgumentCaptor.forClass(classOf[EmailAuditEvent])
  private val emailEvents = EmailEvents(Seq(EmailEvent(Sent, DateTime.now()), EmailEvent(Delivered, DateTime.now()),
    EmailEvent(PermanentBounce, DateTime.now()), EmailEvent(Opened, DateTime.now()), EmailEvent(Complained, DateTime.now())))
}
