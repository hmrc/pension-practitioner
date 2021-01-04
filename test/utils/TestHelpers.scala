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

package utils

import play.api.libs.json.{JsObject, Json}

object TestHelpers {

  val ukAddress: JsObject = Json.obj(
    "countryCode" -> "GB",
    "postalCode" -> "ZZ1 1ZZ",
    "addressLine1" -> "4 Other Place",
    "addressLine2" -> "Some District",
    "addressLine3" -> "Anytown",
    "addressLine4" -> "Somerset",
    "nonUKAddress" -> "false"
  )

  val uaUkAddress: JsObject = Json.obj(
    "addressLine1" -> "4 Other Place",
    "addressLine2" -> "Some District",
    "addressLine3" -> "Anytown",
    "addressLine4" -> "Somerset",
    "postcode" -> "ZZ1 1ZZ",
    "country" -> "GB"
  )

  val nonUkAddress: JsObject = Json.obj(
    "countryCode" -> "FR",
    "addressLine1" -> "4 Other Place",
    "addressLine2" -> "Some District",
    "addressLine3" -> "Anytown",
    "addressLine4" -> "Somerset",
    "nonUKAddress" -> "true"
  )

  val uaNonUkAddress: JsObject = Json.obj(
    "addressLine1" -> "4 Other Place",
    "addressLine2" -> "Some District",
    "addressLine3" -> "Anytown",
    "addressLine4" -> "Somerset",
    "country" -> "FR"
  )

  val correspondenceContactDetails: JsObject = Json.obj(
    "telephone" -> "3445",
    "email" -> "sdd@ds.sd")

  val uaCorrespondenceContactDetails: JsObject = Json.obj(
    "email" -> "sdd@ds.sd",
    "phone" -> "3445")

  val uaExistingPsp: JsObject = Json.obj(
    "existingPSP" -> Json.obj("isExistingPSP" -> true, "existingPSPId" -> "A2345678")
  )

  val pspVariation: JsObject = Json.obj("pspId" -> "A2345678")

  val individualDetails: JsObject = Json.obj("firstName" -> "Stephen", "lastName" -> "Wood")

  val nonUkIndividual: JsObject = Json.obj(
    "legalStatus" -> "Individual",
    "customerType" -> "NonUK"
  )

  val ukIndividual: JsObject = Json.obj(
    "customerType" -> "UK",
    "idType" -> "NINO",
    "legalStatus" -> "Individual",
    "idNumber" -> "AB123456C"
  )

  def ukOrg(org: String): JsObject = Json.obj(
    "legalStatus" -> org,
    "customerType" -> "UK",
    "idType" -> "UTR",
    "idNumber" -> "1234567890"
  )

  def nonUkOrg(org: String): JsObject = Json.obj(
    "legalStatus" -> org,
    "customerType" -> "NonUK"
  )

  val uaSapNo: JsObject = Json.obj(
    "sapNumber" -> "1234567890",
    "noIdentifier" -> false
  )

  val uaAddressChange: JsObject = Json.obj(
    "addressChange" -> true
  )

  val uaNameChange: JsObject = Json.obj(
    "nameChange" -> true
  )

  val changeFlag: JsObject = Json.obj(
    "changeFlag" -> true
  )

  val declarationNRegime: JsObject = Json.obj("regime" -> "PODP",
    "declaration" -> Json.obj("pspDeclarationBox1" -> true))

}
