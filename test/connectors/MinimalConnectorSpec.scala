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

package connectors

import audit.AuditService
import com.github.tomakehurst.wiremock.client.WireMock._
import models.{IndividualDetails, MinimalDetails}
import org.mockito.Mockito.when
import org.scalatest.{AsyncWordSpec, EitherValues, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http._
import utils.WireMockHelper

class MinimalConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper
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
      bind[HeaderUtils].toInstance(mockHeaderUtils)
    )

  private val pspId = "psp-id"
  private val idType = "pspid"
  private val regime = "podp"
  private val minDetailsUrl = s"/pension-online/psa-min-details/$regime/$idType/$pspId"

  when(mockHeaderUtils.integrationFrameworkHeader).thenReturn(Nil)

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
          response.right.get mustBe minDetailsIndividual
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
        recoverToExceptionIf[BadRequestException](connector.getMinimalDetails(pspId, idType, regime)) map {
          ex =>
            ex.responseCode mustBe BAD_REQUEST
            ex.message must include("INVALID_IDVALUE")
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

        recoverToExceptionIf[NotFoundException](connector.getMinimalDetails(pspId, idType, regime)).map { ex =>
          ex.responseCode mustBe NOT_FOUND
          ex.message must include("NOT_FOUND")
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

        recoverToExceptionIf[UpstreamErrorResponse](connector.getMinimalDetails(pspId, idType, regime)).map { response =>
          response.statusCode mustEqual FORBIDDEN
          response.getMessage() must include("FORBIDDEN")
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

        recoverToExceptionIf[UpstreamErrorResponse](connector.getMinimalDetails(pspId, idType, regime)).map { response =>
          response.statusCode mustEqual INTERNAL_SERVER_ERROR
          response.getMessage() must include("SERVER_ERROR")
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
