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
import models.SchemeDetails
import org.mockito.Mockito.when
import org.scalatest.{AsyncWordSpec, EitherValues, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import utils.WireMockHelper

class SchemeConnectorSpec
  extends AsyncWordSpec
    with MustMatchers
    with WireMockHelper
    with EitherValues
    with MockitoSugar {

  import SchemeConnectorSpec._

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  private implicit lazy val rh: RequestHeader = FakeRequest("", "")

  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  private val mockAuditService = mock[AuditService]
  private val mockHeaderUtils = mock[HeaderUtils]

  private lazy val connector: SchemeConnector = injector.instanceOf[SchemeConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(mockAuditService),
      bind[HeaderUtils].toInstance(mockHeaderUtils)
    )

  private val pspId = "21000000"
  private val idType = "pspid"
  private val listOfSchemesUrl = "/pensions-scheme/list-of-schemes"

  when(mockHeaderUtils.integrationFrameworkHeader).thenReturn(Nil)

  "SchemeConnector" must {
    "get list of schemes" in {
      server.stubFor(
        get(urlEqualTo(listOfSchemesUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withHeader("idType", idType)
              .withHeader("idValue", pspId)
              .withBody(listOfSchemesPayload.toString())
          )
      )

      connector.listOfSchemes(pspId).map { response =>
        response.right.get mustBe listOfSchemesPayload
      }
    }

    "return not found" in {
      server.stubFor(
        get(urlEqualTo(listOfSchemesUrl))
          .willReturn(
            notFound()
              .withHeader("Content-Type", "application/json")
              .withHeader("idType", idType)
              .withHeader("idValue", pspId)
              .withBody(errorResponse("NOT_FOUND"))
          )
      )

      connector.listOfSchemes(pspId).map { response =>
        response.left.get.body contains "NOT_FOUND"
        response.left.get.status mustBe NOT_FOUND
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

object SchemeConnectorSpec {
  private val listOfSchemesPayload: JsValue =
    Json.toJson(
      SchemeDetails(
        name = "test",
        referenceNumber = "ref",
        schemeStatus = "OPEN",
        openDate = None,
        pstr = None,
        relationship = None,
        pspDetails = None
      )
    )
}


