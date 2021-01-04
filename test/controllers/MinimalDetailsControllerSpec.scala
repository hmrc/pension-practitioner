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

package controllers

import connectors.MinimalConnector
import models._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{AsyncWordSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http._

import scala.concurrent.Future

class MinimalDetailsControllerSpec extends AsyncWordSpec with MustMatchers {

  import MinimalDetailsControllerSpec._

  "getMinimalDetails" must {
    "return OK when service returns successfully" in {

      when(mockMinimalConnector.getMinimalDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(minimalDetailsIndividualUser)))

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
  def controller: MinimalDetailsController = new MinimalDetailsController(mockMinimalConnector, stubControllerComponents())

}
