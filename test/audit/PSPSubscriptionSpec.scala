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

package audit

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

class PSPSubscriptionSpec
  extends FlatSpec
    with Matchers {

  "PSPSubscription" should "output the correct map of data" in {

    val requestJson = Json.parse(
      """{
        |  "subscriptionTypeAndPSPIDDetails": {
        |    "existingPSPID": "No",
        |    "subscriptionType": "Creation"
        |  }
        |}""".stripMargin)

    val requestJsonExpandedAcronym = Json.parse(
      """{
        |  "subscriptionTypeAndPensionSchemePractitionerIdDetails": {
        |    "subscriptionType": "Creation",
        |    "existingPensionSchemePractitionerId": "No"
        |  }
        |}""".stripMargin)

    val event = PSPSubscription(
      externalId = "externalId",
      status = 200,
      request = requestJson,
      response = Option(Json.obj("some" -> "value")),
    )

    val expected = Map(
      "externalId" -> "externalId",
      "status" -> "200",
      "request" -> Json.prettyPrint(requestJsonExpandedAcronym),
      "response" -> Json.prettyPrint(Json.obj("some" -> "value")),
    )

    event.auditType shouldBe "PensionSchemePractitionerSubscription"
    event.details shouldBe expected
  }
}
