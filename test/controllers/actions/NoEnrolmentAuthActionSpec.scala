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

package controllers.actions

import connectors.HeaderUtilsSpec.mock
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import utils.AuthUtils
import utils.AuthUtils.FakeFailingAuthConnector

import scala.concurrent.ExecutionContext.Implicits.global

class NoEnrolmentAuthActionSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  class Harness(authAction: NoEnrolmentAuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
  }

  "NoEnrolmentAuthAction" must {

    "when the user is logged in" must {

      "must succeed" in {

        running(app) {
          val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

          AuthUtils.noEnrolmentAuthStub(mockAuthConnector)

          val action = new NoEnrolmentAuthAction(mockAuthConnector, bodyParsers)
          val controller = new Harness(action)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual OK
        }
      }
    }

    "when the user is not logged in" must {

      "must return Unauthorized" in {
        running(app) {
          val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

          val authAction = new NoEnrolmentAuthAction(new FakeFailingAuthConnector(new MissingBearerToken), bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) `mustBe` UNAUTHORIZED
        }
      }
    }

  }
}
