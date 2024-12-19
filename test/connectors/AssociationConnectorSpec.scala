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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import repository.{DataCacheRepository, MinimalDetailsCacheRepository}
import uk.gov.hmrc.http._
import utils.WireMockHelper

class AssociationConnectorSpec extends AsyncWordSpec with Matchers with WireMockHelper
  with EitherValues with MockitoSugar {

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository])
    )

  private lazy val connector: AssociationConnector = injector.instanceOf[AssociationConnector]
  private val pstr: String = "pstr"

  private val pspAuthorisationUrl = s"/pension-online/association/pods/$pstr"
  private val pspDeAuthorisationUrl = s"/pension-online/cease-scheme/pods/$pstr"

  "authorisePsp" must {

    "return successfully when IF has returned OK" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspAuthorisationUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            ok
          )
      )

      connector.authorisePsp(data, pstr) map {
        _.value.status mustBe OK
      }
    }

    "return BAD REQUEST when IF has returned BadRequestException" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspAuthorisationUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            badRequest()
          )
      )

      connector.authorisePsp(data, pstr) map {
        _.left.value.responseCode mustBe BAD_REQUEST
      }
    }

    "return NOT FOUND when IF has returned NotFoundException" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspAuthorisationUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            notFound()
          )
      )

      connector.authorisePsp(data, pstr) map {
        _.left.value.responseCode mustEqual NOT_FOUND
      }
    }

    "return Upstream5xxResponse when ETMP has returned Internal Server Error" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspAuthorisationUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            serverError()
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse](
        connector.authorisePsp(data, pstr)
      ) map {
        ex =>
          ex.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  "deAuthorisePsp" must {

    "return successfully when IF has returned OK" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspDeAuthorisationUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            ok
          )
      )

      connector.deAuthorisePsp(data, pstr) map {
        _.value.status mustBe OK
      }
    }

    "return BAD REQUEST when IF has returned BadRequestException" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspDeAuthorisationUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            badRequest()
          )
      )

      connector.deAuthorisePsp(data, pstr) map {
        _.left.value.responseCode mustEqual BAD_REQUEST
      }
    }

    "return NOT FOUND when IF has returned NotFoundException" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspDeAuthorisationUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            notFound()
          )
      )

      connector.deAuthorisePsp(data, pstr) map {
        _.left.value.responseCode mustEqual NOT_FOUND
      }
    }

    "return Upstream5xxResponse when ETMP has returned Internal Server Error" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspDeAuthorisationUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            serverError()
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse](
        connector.deAuthorisePsp(data, pstr)
      ) map {
        ex =>
          ex.statusCode mustBe INTERNAL_SERVER_ERROR
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
