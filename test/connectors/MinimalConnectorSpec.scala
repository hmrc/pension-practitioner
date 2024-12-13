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

import audit.AuditService
import com.github.tomakehurst.wiremock.client.WireMock._
import models.{IndividualDetails, MinimalDetails}
import org.mockito.Mockito._
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import repository.{DataCacheRepository, MinimalDetailsCacheRepository}
import uk.gov.hmrc.http._
import utils.WireMockHelper

class MinimalConnectorSpec extends AsyncWordSpec with Matchers with WireMockHelper
  with EitherValues with MockitoSugar {

  import MinimalConnectorSpec._

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  private implicit lazy val rh: RequestHeader = FakeRequest("", "")

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  private val mockAuditService = mock[AuditService]
  private val mockHeaderUtils = mock[HeaderUtils]

  private lazy val connector: MinimalConnector = injector.instanceOf[MinimalConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(mockAuditService),
      bind[HeaderUtils].toInstance(mockHeaderUtils),
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository])
    )

  private val pspId = "psp-id"
  private val idType = "pspid"
  private val regime = "podp"
  private val minDetailsUrl = s"/pension-online/psa-min-details/$regime/$idType/$pspId"

  when(mockHeaderUtils.integrationFrameworkHeader()).thenReturn(Nil)

  "minimalDetails" must {
    "return user answer json when successful response returned from API" in {
      server.stubFor(
        get(urlEqualTo(minDetailsUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withBody(minDetailsPayload.toString())
          )
      )

      connector.getMinimalDetails(pspId, idType, regime).map { response =>
        response.toOption.get mustBe minDetailsIndividual
      }
    }

    "return a BadRequestException for a 400 INVALID_IDVALUE response" in {
      server.stubFor(
        get(urlEqualTo(minDetailsUrl))
          .willReturn(
            badRequest()
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_IDVALUE"))
          )
      )

      connector.getMinimalDetails(pspId, idType, regime) map {
        response =>
          response.swap.getOrElse(HttpResponse(0, "")).body contains "INVALID_IDVALUE"
          response.swap.getOrElse(HttpResponse(0, "")).status mustBe BAD_REQUEST
      }

    }

    "return Not Found - 404" in {
      server.stubFor(
        get(urlEqualTo(minDetailsUrl))
          .willReturn(
            notFound
              .withBody(errorResponse("NOT_FOUND"))
          )
      )

      connector.getMinimalDetails(pspId, idType, regime) map {
        response =>
          response.swap.getOrElse(HttpResponse(0, "")).body contains "NOT_FOUND"
          response.swap.getOrElse(HttpResponse(0, "")).status mustBe NOT_FOUND
      }
    }

    "throw Upstream4XX for server unavailable - 403" in {

      server.stubFor(
        get(urlEqualTo(minDetailsUrl))
          .willReturn(
            forbidden
              .withBody(errorResponse("FORBIDDEN"))
          )
      )

      connector.getMinimalDetails(pspId, idType, regime) map {
        response =>
          response.swap.getOrElse(HttpResponse(0, "")).body contains "FORBIDDEN"
          response.swap.getOrElse(HttpResponse(0, "")).status mustBe FORBIDDEN
      }
    }

    "throw Upstream5XX for internal server error - 500 and log the event as error" in {

      server.stubFor(
        get(urlEqualTo(minDetailsUrl))
          .willReturn(
            serverError
              .withBody(errorResponse("SERVER_ERROR"))
          )
      )

      connector.getMinimalDetails(pspId, idType, regime) map {
        response =>
          response.swap.getOrElse(HttpResponse(0, "")).body contains "SERVER_ERROR"
          response.swap.getOrElse(HttpResponse(0, "")).status mustBe INTERNAL_SERVER_ERROR
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

object MinimalConnectorSpec {
  private val minDetailsPayload = Json.parse(
    """{
      |	"processingDate": "2001-12-17T09:30:47Z",
      |	"minimalDetails": {
      |		"individualDetails": {
      |			"firstName": "testFirst",
      |			"middleName": "testMiddle",
      |			"lastName": "testLast"
      |		}
      |	},
      |	"email": "test@email.com",
      |	"psaSuspensionFlag": true,
      |	"rlsFlag": true,
      |	"deceasedFlag": true
      |}""".stripMargin)

  val minDetailsIndividual: MinimalDetails = MinimalDetails(
    "test@email.com",
    isPsaSuspended = true,
    None,
    Some(IndividualDetails(
      "testFirst",
      Some("testMiddle"),
      "testLast"
    )),
    rlsFlag = true,
    deceasedFlag = true
  )
}
