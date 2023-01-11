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

package transformations.userAnswersToDes

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import transformations.JsonTransformer

class PSPSubscriptionTransformer extends JsonTransformer {

  lazy val transformPsp: Reads[JsObject] =
    (__ \ Symbol("subscriptionType")).readNullable[String].flatMap {
      case Some("Variation") => transformPspVariation
      case _ => transformPspSubscription
    }

  private lazy val transformPspSubscription: Reads[JsObject] =
    (transformSubscriptionDetails and
      transformLegalAndCustomer and
      transformName and
      transformAddress and
      transformContactDetails and
      transformDeclaration).reduce

  private lazy val transformPspVariation: Reads[JsObject] =
    (transformSubscriptionDetailsVariation and
      transformLegalAndCustomer and
      transformNameVariation and
      transformAddressVariation and
      transformContactDetails and
      transformDeclaration).reduce

  private def transformSubscriptionDetails: Reads[JsObject] =
    ((__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("subscriptionType")).json.put(JsString("Creation")) and
      (__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("existingPSPID")).json.copyFrom(transformBooleanToYesNo(__ \ Symbol("existingPSP") \ Symbol("isExistingPSP"))) and
      ((__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("pspid")).json.copyFrom((__ \ Symbol("existingPSP") \ Symbol("existingPSPId")).json.pick) orElse doNothing)).reduce

  private def transformSubscriptionDetailsVariation: Reads[JsObject] =
    ((__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("subscriptionType")).json.put(JsString("Variation")) and
      (__ \ Symbol("subscriptionTypeAndPSPIDDetails") \ Symbol("pspid")).json.copyFrom((__ \ Symbol("pspId")).json.pick)).reduce

  private def transformLegalAndCustomer: Reads[JsObject] =
    ((__ \ Symbol("regime")).json.put(JsString("PODP")) and
      ((__ \ Symbol("sapNumber")).json.copyFrom((__ \ Symbol("registrationInfo") \ Symbol("sapNumber")).json.pick) orElse doNothing) and
      (__ \ Symbol("legalEntityAndCustomerID") \ Symbol("legalStatus")).json.copyFrom((__ \ Symbol("registrationInfo") \ Symbol("legalStatus")).json.pick) and
      (__ \ Symbol("legalEntityAndCustomerID") \ Symbol("customerType")).json.copyFrom((__ \ Symbol("registrationInfo") \ Symbol("customerType")).json.pick) and
      ((__ \ Symbol("legalEntityAndCustomerID") \ Symbol("idType")).json.copyFrom((__ \ Symbol("registrationInfo") \ Symbol("idType")).json.pick) orElse doNothing) and
      ((__ \ Symbol("legalEntityAndCustomerID") \ Symbol("idNumber")).json.copyFrom((__ \ Symbol("registrationInfo") \ Symbol("idNumber")).json.pick) orElse doNothing)).reduce

  private def transformName: Reads[JsObject] =
    (__ \ Symbol("registrationInfo") \ Symbol("legalStatus")).read[String].flatMap {
      case "Individual" =>
        ((__ \ Symbol("individualDetails") \ Symbol("firstName")).json.copyFrom((__ \ Symbol("individualDetails") \ Symbol("firstName")).json.pick) and
          (__ \ Symbol("individualDetails") \ Symbol("lastName")).json.copyFrom((__ \ Symbol("individualDetails") \ Symbol("lastName")).json.pick)).reduce
      case _ =>
        (__ \ Symbol("orgOrPartnershipDetails") \ Symbol("organisationName")).json.copyFrom((__ \ Symbol("name")).json.pick)
    }

  private def transformNameVariation: Reads[JsObject] = (__ \ Symbol("registrationInfo") \ Symbol("customerType")).read[String].flatMap {
    case "UK" => transformName
    case _ =>
      (__ \ Symbol("registrationInfo") \ Symbol("legalStatus")).read[String].flatMap {
        case "Individual" =>
          ((__ \ Symbol("individualDetails") \ Symbol("firstName")).json.copyFrom((__ \ Symbol("individualDetails") \ Symbol("firstName")).json.pick) and
            (__ \ Symbol("individualDetails") \ Symbol("lastName")).json.copyFrom((__ \ Symbol("individualDetails") \ Symbol("lastName")).json.pick) and
            ((__ \ Symbol("individualDetails") \ Symbol("changeFlag")).json.copyFrom((__ \ Symbol("nameChange")).json.pick) orElse
              (__ \ Symbol("individualDetails") \ Symbol("changeFlag")).json.put(JsBoolean(false)))).reduce
        case _ =>
          ((__ \ Symbol("orgOrPartnershipDetails") \ Symbol("organisationName")).json.copyFrom((__ \ Symbol("name")).json.pick) and
            ((__ \ Symbol("orgOrPartnershipDetails") \ Symbol("changeFlag")).json.copyFrom((__ \ Symbol("nameChange")).json.pick) orElse
              (__ \ Symbol("orgOrPartnershipDetails") \ Symbol("changeFlag")).json.put(JsBoolean(false)))).reduce
      }
  }

  private def transformAddress: Reads[JsObject] =
    (nonUKAddress and
      (__ \ Symbol("correspondenceAddressDetails") \ Symbol("addressLine1")).json.copyFrom((__ \ Symbol("contactAddress") \ Symbol("addressLine1")).json.pick) and
      (__ \ Symbol("correspondenceAddressDetails") \ Symbol("addressLine2")).json.copyFrom((__ \ Symbol("contactAddress") \ Symbol("addressLine2")).json.pick) and
      ((__ \ Symbol("correspondenceAddressDetails") \ Symbol("addressLine3")).json.copyFrom((__ \ Symbol("contactAddress") \ Symbol("addressLine3")).json.pick) orElse doNothing) and
      ((__ \ Symbol("correspondenceAddressDetails") \ Symbol("addressLine4")).json.copyFrom((__ \ Symbol("contactAddress") \ Symbol("addressLine4")).json.pick) orElse doNothing) and
      (__ \ Symbol("correspondenceAddressDetails") \ Symbol("countryCode")).json.copyFrom((__ \ Symbol("contactAddress") \ Symbol("country")).json.pick) and
      ((__ \ Symbol("correspondenceAddressDetails") \ Symbol("postalCode")).json.copyFrom((__ \ Symbol("contactAddress") \ Symbol("postcode")).json.pick) orElse doNothing)).reduce

  private def transformAddressVariation: Reads[JsObject] = (transformAddress and
    ((__ \ Symbol("correspondenceAddressDetails") \ Symbol("changeFlag")).json.copyFrom((__ \ Symbol("addressChange")).json.pick) orElse
      (__ \ Symbol("correspondenceAddressDetails") \ Symbol("changeFlag")).json.put(JsBoolean(false)))).reduce

  private def nonUKAddress: Reads[JsObject] =
    (__ \ Symbol("contactAddress") \ Symbol("country")).read[String].flatMap {
      case "GB" =>
        (__ \ Symbol("correspondenceAddressDetails") \ Symbol("nonUKAddress")).json.put(JsString("false"))
      case _ =>
        (__ \ Symbol("correspondenceAddressDetails") \ Symbol("nonUKAddress")).json.put(JsString("true"))
    }

  private def transformContactDetails: Reads[JsObject] =
    ((__ \ Symbol("correspondenceContactDetails") \ Symbol("email")).json.copyFrom((__ \ Symbol("email")).json.pick) and
      (__ \ Symbol("correspondenceContactDetails") \ Symbol("telephone")).json.copyFrom((__ \ Symbol("phone")).json.pick)).reduce

  private def transformDeclaration: Reads[JsObject] = (__ \ Symbol("declaration") \ Symbol("pspDeclarationBox1")).json.put(JsBoolean(true))

}
