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

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }
}
