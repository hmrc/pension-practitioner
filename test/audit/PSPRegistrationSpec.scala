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

class PSPRegistrationSpec
  extends FlatSpec
    with Matchers {

  "PSPRegistration" should "output the correct map of data" in {

    def event(withId: Boolean) = PSPRegistration(
      withId = withId,
      externalId = "externalId",
      psaType = "psaType",
      found = true,
      isUk = Option(true),
      status = 200,
      request = Json.obj("some" -> "value"),
      response = Option(Json.obj("some" -> "value")),
    )

    val expected = Map(
      "withId" -> "true",
      "externalId" -> "externalId",
      "pensionSchemeAdministratorType" -> "psaType",
      "found" -> "true",
      "isUk" -> "true",
      "status" -> "200",
      "request" -> Json.prettyPrint(Json.obj("some" -> "value")),
      "response" -> Json.prettyPrint(Json.obj("some" -> "value")),
    )

    event(true).auditType shouldBe "PensionSchemePractitionerRegistration"
    event(false).auditType shouldBe "PensionSchemePractitionerRegWithoutId"
    event(true).details shouldBe expected
  }
}
