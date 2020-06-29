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

package models.registerWithId

import models.enumeration.{EnumUtils, OrganisationTypeEnum}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Organisation(organisationName: String, organisationType: OrganisationTypeEnum.OrganisationType)

object Organisation {
  implicit val reads: Reads[Organisation] = (
    (JsPath \ "organisationName").read[String] and
      (JsPath \ "organisationType").read(EnumUtils.enumReads(OrganisationTypeEnum))
    ) (
    (orgName, orgType) => {
      Organisation(
        orgName.replaceAll("""[^a-zA-Z0-9 '&\/]+""", ""),
        orgType
      )
    }
  )
  implicit val writes: Writes[Organisation] = Json.writes[Organisation]
}
