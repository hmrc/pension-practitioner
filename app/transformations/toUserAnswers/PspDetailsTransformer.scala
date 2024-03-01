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

package transformations.toUserAnswers

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsObject, Reads, __}
import transformations.JsonTransformer

class PspDetailsTransformer extends JsonTransformer {

  lazy val transformToUserAnswers: Reads[JsObject] =
    (transformSubscriptionDetails and
      transformLegalAndCustomer and
      transformName and
      transformAddress and
      transformContactDetails).reduce

  private def transformSubscriptionDetails: Reads[JsObject] =
    ((__ \ Symbol("applicationDate")).json.copyFrom((__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("applicationDate")).json.pick) and
      (__ \ Symbol("subscriptionType")).json.copyFrom((__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("subscriptionType")).json.pick) and
      ((__ \ Symbol("existingPSP") \ Symbol("isExistingPSP")).json.copyFrom((__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("existingPSPID")).json.pick) orElse doNothing) and
      (__ \ Symbol("existingPSP") \ Symbol("existingPSPId")).json.copyFrom((__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("pspid")).json.pick)).reduce

  private def transformLegalAndCustomer: Reads[JsObject] =
    ((__ \ Symbol("registrationInfo") \ Symbol("legalStatus")).json.copyFrom((__ \ Symbol("legalEntityAndCustomerID") \ Symbol("legalStatus")).json.pick) and
      (__ \ Symbol("registrationInfo") \ Symbol("customerType")).json.copyFrom((__ \ Symbol("legalEntityAndCustomerID") \ Symbol("customerType")).json.pick) and
      ((__ \ Symbol("registrationInfo") \ Symbol("idType")).json.copyFrom((__ \ Symbol("legalEntityAndCustomerID") \ Symbol("idType")).json.pick) orElse doNothing) and
      ((__ \ Symbol("registrationInfo") \ Symbol("idNumber")).json.copyFrom((__ \ Symbol("legalEntityAndCustomerID") \ Symbol("idNumber")).json.pick) orElse doNothing)).reduce

  private def transformName: Reads[JsObject] =
    (__ \ Symbol("legalEntityAndCustomerID") \ Symbol("legalStatus")).read[String].flatMap {
      case "Individual" =>
        ((__ \ Symbol("individualDetails") \ Symbol("firstName")).json.copyFrom((__ \ Symbol("individualDetails") \ Symbol("firstName")).json.pick) and
          (__ \ Symbol("individualDetails") \ Symbol("lastName")).json.copyFrom((__ \ Symbol("individualDetails") \ Symbol("lastName")).json.pick)).reduce
      case _ =>
        (__ \ Symbol("name")).json.copyFrom((__ \ Symbol("orgOrPartnershipDetails") \ Symbol("organisationName")).json.pick)
    }

  private def transformAddress: Reads[JsObject] =
    ((__ \ Symbol("contactAddress") \ Symbol("addressLine1")).json.copyFrom((__ \ Symbol("correspondenceAddressDetails") \ Symbol("addressLine1")).json.pick) and
      (__ \ Symbol("contactAddress") \ Symbol("addressLine2")).json.copyFrom((__ \ Symbol("correspondenceAddressDetails") \ Symbol("addressLine2")).json.pick) and
      ((__ \ Symbol("contactAddress") \ Symbol("addressLine3")).json.copyFrom((__ \ Symbol("correspondenceAddressDetails") \ Symbol("addressLine3")).json.pick) orElse doNothing) and
      ((__ \ Symbol("contactAddress") \ Symbol("addressLine4")).json.copyFrom((__ \ Symbol("correspondenceAddressDetails") \ Symbol("addressLine4")).json.pick) orElse doNothing) and
      (__ \ Symbol("contactAddress") \ Symbol("country")).json.copyFrom((__ \ Symbol("correspondenceAddressDetails") \ Symbol("countryCode")).json.pick) and
      ((__ \ Symbol("contactAddress") \ Symbol("postcode")).json.copyFrom((__ \ Symbol("correspondenceAddressDetails") \ Symbol("postalCode")).json.pick) orElse doNothing)
      ).reduce


  private def transformContactDetails: Reads[JsObject] =
    ((__ \ Symbol("email")).json.copyFrom((__ \ Symbol("correspondenceContactDetails") \ Symbol("email")).json.pick) and
      (__ \ Symbol("phone")).json.copyFrom((__ \ Symbol("correspondenceContactDetails") \ Symbol("telephone")).json.pick)).reduce
}
