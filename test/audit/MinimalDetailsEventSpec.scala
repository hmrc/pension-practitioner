/*
 * Copyright 2022 HM Revenue & Customs
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

package audit

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

class MinimalDetailsEventSpec
  extends FlatSpec
    with Matchers {

  "MinimalDetailsEvent" should "output the correct map of data" in {

    val event = MinimalDetailsEvent(
      idType = "pspid",
      idValue = "2100000",
      name = Option("test name"),
      isSuspended = Option(false),
      rlsFlag = Option(false),
      deceasedFlag = Option(false),
      status = 200,
      response = Option(Json.obj("some" -> "value"))
    )

    val expected = Map(
      "idType" -> "pspid",
      "idValue" -> "2100000",
      "name" -> "test name",
      "isPsaSuspended" -> "false",
      "rlsFlag" -> "false",
      "deceasedFlag" -> "false",
      "status" -> "200",
      "response" -> Json.stringify(Json.obj("some" -> "value"))
    )

    event.auditType shouldBe "GetMinDetails"
    event.details shouldBe expected
  }
}
