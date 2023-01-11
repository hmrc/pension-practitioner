/*
 * Copyright 2023 HM Revenue & Customs
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

package models.registerWithoutId

import play.api.libs.json._

case class RegisterWithoutIdIndividualRequest(firstName: String, lastName: String, address: Address)

object RegisterWithoutIdIndividualRequest {
  implicit val formats: Format[RegisterWithoutIdIndividualRequest] = Json.format[RegisterWithoutIdIndividualRequest]


  def writesRegistrationNoIdIndividualRequest(acknowledgementReference: String): OWrites[RegisterWithoutIdIndividualRequest] = {

    new OWrites[RegisterWithoutIdIndividualRequest] {

      override def writes(registrant: RegisterWithoutIdIndividualRequest): JsObject = {
        Json.obj(
          "regime" -> "PODP",
          "acknowledgementReference" -> acknowledgementReference,
          "isAnAgent" -> JsBoolean(false),
          "isAGroup" -> JsBoolean(false),
          "individual" -> Json.obj(
            "firstName" -> registrant.firstName,
            "lastName" -> registrant.lastName
          ),
          "address" -> Json.obj(
            "addressLine1" -> registrant.address.addressLine1,
            "addressLine2" -> registrant.address.addressLine2,
            "addressLine3" -> registrant.address.addressLine3.map(JsString).getOrElse[JsValue](JsNull),
            "addressLine4" -> registrant.address.addressLine4.map(JsString).getOrElse[JsValue](JsNull),
            "postalCode" -> registrant.address.postcode.map(JsString).getOrElse[JsValue](JsNull),
            "countryCode" -> registrant.address.country
          ),
          "contactDetails" -> Json.obj()
        )
      }

    }

  }
}

