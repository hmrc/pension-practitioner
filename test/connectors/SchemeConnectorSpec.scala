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
import models.SchemeDetails
import org.mockito.Mockito._
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsBoolean, JsValue, Json}
import play.api.test.Helpers._
import repository.{DataCacheRepository, MinimalDetailsCacheRepository}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import utils.{AuthUtils, WireMockHelper}

class SchemeConnectorSpec
  extends AsyncWordSpec
    with Matchers
    with WireMockHelper
    with EitherValues
    with MockitoSugar {

  import SchemeConnectorSpec._

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  private val mockAuditService = mock[AuditService]
  private val mockHeaderUtils = mock[HeaderUtils]
  private val checkForAssociationUrl = "/pensions-scheme/is-psa-associated"

  private lazy val connector: SchemeConnector = app.injector.instanceOf[SchemeConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(mockAuditService),
      bind[HeaderUtils].toInstance(mockHeaderUtils),
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository])
    )

  private val idType = "pspid"
  private val listOfSchemesUrl = "/pensions-scheme/list-of-schemes-self"

  when(mockHeaderUtils.integrationFrameworkHeader()).thenReturn(Nil)

  "SchemeConnector" must {
    "get list of schemes" in {
      server.stubFor(
        get(urlEqualTo(listOfSchemesUrl))
          .willReturn(
            ok
              .withHeader("Content-Type", "application/json")
              .withHeader("idType", idType)
              .withBody(listOfSchemesPayload.toString())
          )
      )

      connector.listOfSchemes.map { response =>
        response.toOption.get `mustBe` listOfSchemesPayload
      }
    }

    "return not found" in {
      server.stubFor(
        get(urlEqualTo(listOfSchemesUrl))
          .willReturn(
            notFound()
              .withHeader("Content-Type", "application/json")
              .withHeader("idType", idType)
              .withBody(errorResponse("NOT_FOUND"))
          )
      )

      connector.listOfSchemes.map { response =>
        response.swap.getOrElse(HttpResponse(0, "")).body must include("NOT_FOUND")
        response.swap.getOrElse(HttpResponse(0, "")).status `mustBe` NOT_FOUND
      }
    }

    "checkForAssociation" must {
      lazy val connector: SchemeConnector = injector.instanceOf[SchemeConnector]

      "handle OK (200)" in {

        server.stubFor(
          get(urlEqualTo(checkForAssociationUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("psaId", equalTo(AuthUtils.psaId))
            .withHeader("schemeReferenceNumber", equalTo(AuthUtils.srn))
            .willReturn(
              ok(JsBoolean(true).toString())
                .withHeader("Content-Type", "application/json")
            )
        )

        connector.checkForAssociation(Left(PsaId(AuthUtils.psaId)), AuthUtils.srn) map { response =>
          response.value `mustBe` true
        }

      }

      "relay BadRequestException when headers are missing" in {

        server.stubFor(
          get(urlEqualTo(checkForAssociationUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
              badRequest
                .withBody("Bad Request with missing parameters PSA Id or SRN")
            )
        )

        connector.checkForAssociation(Left(PsaId(AuthUtils.psaId)), AuthUtils.srn) map { response =>
          response.left.value `mustBe` a[BadRequestException]
        }

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
        windUpDate = None,
        pstr = None,
        relationship = None,
        pspDetails = None
      )
    )
}


