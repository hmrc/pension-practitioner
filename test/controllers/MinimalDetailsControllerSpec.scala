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

import connectors.MinimalConnector
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, BodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.MinimalDetailsCacheRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MinimalDetailsControllerSpec extends PlaySpec with Matchers with GuiceOneAppPerSuite with BeforeAndAfter {

  import MinimalDetailsControllerSpec._

  def controller: MinimalDetailsController = new MinimalDetailsController(mockMinimalConnector, mockMinimalDetailsCacheRepository,
    stubControllerComponents(),
    new actions.PsaPspAuthAction(mockAuthConnector, app.injector.instanceOf[BodyParsers.Default]))

  before {
    AuthUtils.authStub(mockAuthConnector)
  }

  "getMinimalDetails" must {

    "return OK when service returns successfully" in {

      when(mockMinimalDetailsCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          Some(Json.toJson(minimalDetailsIndividualUser))
        })


      val result = controller.getMinimalDetails(fakeRequest.withHeaders(("pspId", "A2123456")))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(minimalDetailsIndividualUser)
    }

    "return OK when service returns successfully with Validation Error " in {

      when(mockMinimalDetailsCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          Some(Json.obj())
        })
      when(mockMinimalConnector.getMinimalDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(minimalDetailsIndividualUser)))

      when(mockMinimalDetailsCacheRepository.upsert(any(), any())(any()))
        .thenReturn(Future.successful((): Unit))
      val result = controller.getMinimalDetails(fakeRequest.withHeaders(("pspId", "A2123456")))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(minimalDetailsIndividualUser)
    }

    "return OK when service returns successfully with None data " in {

      when(mockMinimalDetailsCacheRepository.get(any())(any()))
        .thenReturn(Future.successful {
          None
        })
      when(mockMinimalConnector.getMinimalDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(minimalDetailsIndividualUser)))

      when(mockMinimalDetailsCacheRepository.upsert(any(), any())(any()))
        .thenReturn(Future.successful((): Unit))
      val result = controller.getMinimalDetails(fakeRequest.withHeaders(("pspId", "A2123456")))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(minimalDetailsIndividualUser)
    }

    "return bad request when connector returns BAD_REQUEST" in {

      when(mockMinimalConnector.getMinimalDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Left(HttpResponse(BAD_REQUEST, "bad request"))))

      val result = controller.getMinimalDetails(fakeRequest.withHeaders(("pspId", "A2123456")))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "bad request"
    }

    "return not found when connector returns NOT_FOUND" in {

      when(mockMinimalConnector.getMinimalDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Left(HttpResponse(NOT_FOUND, "not found"))))
      val result = controller.getMinimalDetails(fakeRequest.withHeaders(("pspId", "A2123456")))

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "not found"
    }
  }
}

object MinimalDetailsControllerSpec extends MockitoSugar {

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val individual: IndividualDetails = IndividualDetails("testFirst", Some("testMiddle"), "testLast")

  val minimalDetailsIndividualUser: MinimalDetails =
    MinimalDetails(
      "test@email.com",
      isPsaSuspended = true,
      None,
      Some(individual),
      rlsFlag = true,
      deceasedFlag = true
    )

  val minimalDetailsOrganisationUser: MinimalDetails =
    MinimalDetails(
      "test@email.com",
      isPsaSuspended = true,
      Some("PSA Ltd."),
      None,
      rlsFlag = true,
      deceasedFlag = true
    )

  val mockMinimalConnector: MinimalConnector = mock[MinimalConnector]
  val mockMinimalDetailsCacheRepository: MinimalDetailsCacheRepository = mock[MinimalDetailsCacheRepository]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]

}
