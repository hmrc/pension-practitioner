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
import audit.AuditService
import audit.SubscriptionAuditService
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.Matchers.any
import org.mockito.Mockito.reset
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.{AsyncWordSpec, EitherValues, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import repository.DataCacheRepository
import uk.gov.hmrc.http._
import utils.WireMockHelper

import scala.concurrent.Future
import scala.util.Try
import scala.util.Try

class SubscriptionConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper
  with EitherValues with MockitoSugar with BeforeAndAfterEach {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  private implicit lazy val rh: RequestHeader = FakeRequest("", "")

 // private val mockAuditService: AuditService = mock[AuditService]

  //override protected def bindings: Seq[GuiceableModule] =
  //  Seq(
  //    bind[AuditService].toInstance(mockAuditService)
  //  )

  override def beforeEach(): Unit = {
//    reset(mockAuditService)

    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  private lazy val connector: SubscriptionConnector = injector.instanceOf[SubscriptionConnector]

  private val pspSubscriptionUrl = "/pension-online/subscriptions/psp"

  private val externalId = "externalId"

  "pspSubscription" must {

    "return successfully when DES has returned OK" in {
      val data = Json.obj(fields = "Id" -> "value")
      server.stubFor(
        post(urlEqualTo(pspSubscriptionUrl))
          .withRequestBody(equalTo(Json.stringify(data)))
          .willReturn(
            ok
          )
      )
println("\n><>>>OO")
      connector.pspSubscription(externalId, data) map {
        _.status mustBe OK
      }
    }

    //"return BAD REQUEST when DES has returned BadRequestException" in {
    //  val data = Json.obj(fields = "Id" -> "value")
    //  server.stubFor(
    //    post(urlEqualTo(pspSubscriptionUrl))
    //      .withRequestBody(equalTo(Json.stringify(data)))
    //      .willReturn(
    //        badRequest()
    //      )
    //  )
    //
    //  connector.pspSubscription(externalId, data).map {
    //    _.status mustEqual BAD_REQUEST
    //  }
    //}
    //
    //"return NOT FOUND when DES has returned NotFoundException" in {
    //  val data = Json.obj(fields = "Id" -> "value")
    //  server.stubFor(
    //    post(urlEqualTo(pspSubscriptionUrl))
    //      .withRequestBody(equalTo(Json.stringify(data)))
    //      .willReturn(
    //        notFound()
    //      )
    //  )
    //
    //  connector.pspSubscription(externalId, data).map {
    //    _.status mustEqual NOT_FOUND
    //  }
    //}
    //
    //"return Upstream5xxResponse when ETMP has returned Internal Server Error" in {
    //  val data = Json.obj(fields = "Id" -> "value")
    //  server.stubFor(
    //    post(urlEqualTo(pspSubscriptionUrl))
    //      .withRequestBody(equalTo(Json.stringify(data)))
    //      .willReturn(
    //        serverError()
    //      )
    //  )
    //  connector.pspSubscription(externalId, data).map {
    //    _.status mustBe INTERNAL_SERVER_ERROR
    //  }
    //}

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
