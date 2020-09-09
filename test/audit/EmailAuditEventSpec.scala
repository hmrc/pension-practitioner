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

package audit

import models.Sent
import org.scalatest.{FlatSpec, Matchers}
import models.enumeration.JourneyType.AFT_SUBMIT_RETURN
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.domain.PsaId

class EmailAuditEventSpec extends FlatSpec with Matchers {

  "EmailAuditEvent" should "output the correct map of data" in {

    val event = EmailAuditEvent(
      psaId = PsaId("A2500001"),
      emailAddress = "test@test.com",
      event = Sent,
      journeyType = AFT_SUBMIT_RETURN,
      requestId = "test-request-id"
    )

    val expected = Map(
      "email-initiation-request-id" -> "test-request-id",
      "psaId" -> "A2500001",
      "emailAddress" -> "test@test.com",
      "event" -> Sent.toString
    )

    event.auditType shouldBe "AFTReturnSubmittedEmailEvent"
    event.details shouldBe expected
  }
}
