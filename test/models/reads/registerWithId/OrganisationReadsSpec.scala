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

package models.reads

import models.enumeration.OrganisationTypeEnum
import models.registerWithId.Organisation
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class OrganisationReadsSpec extends WordSpec with MustMatchers with OptionValues {
  private val organisation = Json.obj(
    "organisationName" -> "(Test Ltd)",
    "organisationType" -> "Partnership"
  )
  "A JSON Payload with an Organisation" must {
    "map correctly to Organisation" when {
      val result = organisation.as[Organisation]

      "we have organisation name" in {
        result.organisationName mustBe "Test Ltd"
      }

      "we have organisation type" in {
        result.organisationType mustBe OrganisationTypeEnum.Partnership
      }

    }
  }
}
