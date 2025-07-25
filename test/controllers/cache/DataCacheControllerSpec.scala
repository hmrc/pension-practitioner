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

package controllers.cache

import controllers.actions.CredIdNotFoundFromAuth
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.{DataCacheRepository, MinimalDetailsCacheRepository}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.Future

class DataCacheControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val repo = mock[DataCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val id = "id"
  private val fakeRequest = FakeRequest()
  private val fakePostRequest = FakeRequest("POST", "/")

  private val app = new GuiceApplicationBuilder().configure(
    "microservice.services.des-hod.env" -> "local",
    "microservice.services.des-hod.authorizationToken" -> "test-token"
  ).overrides(Seq(
    bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
    bind[AuthConnector].toInstance(authConnector),
    bind[DataCacheRepository].toInstance(repo),
  )).build()

  val controller: DataCacheController = app.injector.instanceOf[DataCacheController]

  before {
    reset(repo)
    reset(authConnector)
  }

  "DataCacheController" when {
    "calling get" must {
      "return OK with the data" in {
        when(repo.get(eqTo(id))(using any())) `thenReturn` Future.successful(Some(Json.obj("testId" -> "data")))
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(Some(id))

        val result = controller.get(fakeRequest)
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.obj(fields = "testId" -> "data")
      }

      "return NOT FOUND when the data doesn't exist" in {
        when(repo.get(eqTo(id))(using any())) `thenReturn` Future.successful(None)
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(Some(id))

        val result = controller.get(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.get(eqTo(id))(using any())) `thenReturn` Future.failed(new Exception())
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(Some(id))
        val result = controller.get(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(None)

        val result = controller.get(fakeRequest)
        an[CredIdNotFoundFromAuth] must be thrownBy status(result)
      }

    }

    "calling save" must {

      "return OK when the data is saved successfully" in {
        when(repo.save(any(), any())(using any())) `thenReturn` Future.successful((): Unit)
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(Some(id))

        val result = controller.save(fakePostRequest.withJsonBody(Json.obj("value" -> "data")))
        status(result) mustEqual CREATED
      }

      "return BAD REQUEST when the request body cannot be parsed" in {
        when(repo.save(any(), any())(using any())) `thenReturn` Future.successful((): Unit)
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(Some(id))

        val result = controller.save(fakePostRequest.withRawBody(ByteString(nextBytes(512001))))
        status(result) mustEqual BAD_REQUEST
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(None)

        val result = controller.save(fakePostRequest.withJsonBody(Json.obj(fields = "value" -> "data")))
        an[CredIdNotFoundFromAuth] must be thrownBy status(result)
      }
    }

    "calling remove" must {
      "return OK when the data is removed successfully" in {
        when(repo.remove(eqTo(id))(using any())) `thenReturn` Future.successful((): Unit)
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(Some(id))

        val result = controller.remove(fakeRequest)
        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Option[String]](any(), any())(using any(), any())) `thenReturn` Future.successful(None)

        val result = controller.remove(fakeRequest)
        an[CredIdNotFoundFromAuth] must be thrownBy status(result)
      }
    }
  }
  private def nextBytes(count: Int): Array[Byte] = {
    val result = new Array[Byte](count)
    ThreadLocalRandom.current.nextBytes(result)
    result
  }
}
