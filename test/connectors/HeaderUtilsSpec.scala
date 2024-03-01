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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import repository.{AdminDataRepository, DataCacheRepository, MinimalDetailsCacheRepository}

class HeaderUtilsSpec extends AnyWordSpec with Matchers {

  import HeaderUtilsSpec._

  "HeaderUtils" when {

    "call desHeader" must {
      "return all the appropriate headers" in {
        val result = headerUtils.desHeaderWithoutCorrelationId
        result mustEqual Seq("Environment" -> "local", "Authorization" -> "Bearer test-token",
          "Content-Type" -> "application/json")
      }
    }

    "call getCorrelationId" must {
      "return a CorrelationId of the correct size" in {
        val result = headerUtils.getCorrelationId
        result.length mustEqual headerUtils.maxLengthCorrelationId
      }
    }

    "call getCorrelationIdIF" must {
      "return a CorrelationId of the correct size" in {
        val result = headerUtils.getCorrelationIdIF
        result.length mustEqual headerUtils.maxLengthCorrelationIF
      }
    }
  }
}

object HeaderUtilsSpec extends MockitoSugar {

  private val app = new GuiceApplicationBuilder().configure(
    "microservice.services.des-hod.env" -> "local",
    "microservice.services.des-hod.authorizationToken" -> "test-token"
  ).overrides(Seq(
    bind[DataCacheRepository].toInstance(mock[DataCacheRepository]),
    bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
    bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository])
  )).build()

  val headerUtils: HeaderUtils = app.injector.instanceOf[HeaderUtils]
}