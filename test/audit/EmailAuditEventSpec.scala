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

import models.Sent
import models.enumeration.JourneyType
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class EmailAuditEventSpec
  extends FlatSpec
    with Matchers {

  "EmailAuditEvent" should "output the correct map of data" in {

    val event = EmailAuditEvent(
      pspId = "A2500001",
      emailAddress = "test@test.com",
      event = Sent,
      journeyType = JourneyType.PSP_SUBSCRIPTION,
      requestId = "test-request-id"
    )

    val expected = Map(
      "email-initiation-request-id" -> "test-request-id",
      "pensionSchemePractitionerId" -> "A2500001",
      "emailAddress" -> "test@test.com",
      "event" -> Sent.toString
    )

    event.auditType shouldBe "PensionSchemePractitionerSubscriptionEmailEvent"
    event.details shouldBe expected
  }
}
