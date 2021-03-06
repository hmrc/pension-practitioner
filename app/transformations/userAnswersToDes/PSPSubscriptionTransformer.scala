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

package transformations.userAnswersToDes

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import transformations.JsonTransformer

class PSPSubscriptionTransformer extends JsonTransformer {

  lazy val transformPsp: Reads[JsObject] =
    (__ \ 'subscriptionType).readNullable[String].flatMap {
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
    ((__ \ 'subscriptionTypeAndPSPIDDetails \ 'subscriptionType).json.put(JsString("Creation")) and
     (__ \ 'subscriptionTypeAndPSPIDDetails \ 'existingPSPID).json.copyFrom(transformBooleanToYesNo(__ \ 'existingPSP \ 'isExistingPSP)) and
     ((__ \ 'subscriptionTypeAndPSPIDDetails \ 'pspid).json.copyFrom((__ \ 'existingPSP \ 'existingPSPId).json.pick) orElse doNothing)).reduce

  private def transformSubscriptionDetailsVariation: Reads[JsObject] =
    ((__ \ 'subscriptionTypeAndPSPIDDetails \ 'subscriptionType).json.put(JsString("Variation")) and
    (__ \ 'subscriptionTypeAndPSPIDDetails \ 'pspid).json.copyFrom((__ \ 'pspId).json.pick)).reduce

  private def transformLegalAndCustomer: Reads[JsObject] =
    ((__ \ 'regime).json.put(JsString("PODP")) and
      ((__ \ 'sapNumber).json.copyFrom((__ \ 'registrationInfo \ 'sapNumber).json.pick) orElse doNothing) and
      (__ \ 'legalEntityAndCustomerID \ 'legalStatus).json.copyFrom((__ \ 'registrationInfo \ 'legalStatus).json.pick) and
      (__ \ 'legalEntityAndCustomerID \ 'customerType).json.copyFrom((__ \ 'registrationInfo \ 'customerType).json.pick) and
      ((__ \ 'legalEntityAndCustomerID \ 'idType).json.copyFrom((__ \ 'registrationInfo \ 'idType).json.pick) orElse doNothing) and
      ((__ \ 'legalEntityAndCustomerID \ 'idNumber).json.copyFrom((__ \ 'registrationInfo \ 'idNumber).json.pick) orElse doNothing)).reduce

  private def transformName: Reads[JsObject] =
    (__ \ 'registrationInfo \ 'legalStatus).read[String].flatMap {
      case "Individual" =>
        ((__ \ 'individualDetails \ 'firstName).json.copyFrom((__ \ 'individualDetails \ 'firstName).json.pick) and
          (__ \ 'individualDetails \ 'lastName).json.copyFrom((__ \ 'individualDetails \ 'lastName).json.pick)).reduce
      case _ =>
        (__ \ 'orgOrPartnershipDetails \ 'organisationName).json.copyFrom((__ \ 'name).json.pick)
    }

  private def transformNameVariation: Reads[JsObject] = (__ \ 'registrationInfo \ 'customerType).read[String].flatMap {
    case "UK" => transformName
    case _ =>
    (__ \ 'registrationInfo \ 'legalStatus).read[String].flatMap {
      case "Individual" =>
        ((__ \ 'individualDetails \ 'firstName).json.copyFrom((__ \ 'individualDetails \ 'firstName).json.pick) and
          (__ \ 'individualDetails \ 'lastName).json.copyFrom((__ \ 'individualDetails \ 'lastName).json.pick) and
          ((__ \ 'individualDetails \ 'changeFlag).json.copyFrom((__ \ 'nameChange).json.pick) orElse
            (__ \ 'individualDetails \ 'changeFlag).json.put(JsBoolean(false)))).reduce
      case _ =>
        ((__ \ 'orgOrPartnershipDetails \ 'organisationName).json.copyFrom((__ \ 'name).json.pick) and
          ((__ \ 'orgOrPartnershipDetails \ 'changeFlag).json.copyFrom((__ \ 'nameChange).json.pick) orElse
            (__ \ 'orgOrPartnershipDetails \ 'changeFlag).json.put(JsBoolean(false)))).reduce
    }
  }

  private def transformAddress: Reads[JsObject] =
    (nonUKAddress and
      (__ \ 'correspondenceAddressDetails \ 'addressLine1).json.copyFrom((__ \ 'contactAddress \ 'addressLine1).json.pick) and
      (__ \ 'correspondenceAddressDetails \ 'addressLine2).json.copyFrom((__ \ 'contactAddress \ 'addressLine2).json.pick) and
      ((__ \ 'correspondenceAddressDetails \ 'addressLine3).json.copyFrom((__ \ 'contactAddress \ 'addressLine3).json.pick) orElse doNothing) and
      ((__ \ 'correspondenceAddressDetails \ 'addressLine4).json.copyFrom((__ \ 'contactAddress \ 'addressLine4).json.pick) orElse doNothing) and
      (__ \ 'correspondenceAddressDetails \ 'countryCode).json.copyFrom((__ \ 'contactAddress \ 'country).json.pick) and
      ((__ \ 'correspondenceAddressDetails \ 'postalCode).json.copyFrom((__ \ 'contactAddress \ 'postcode).json.pick) orElse doNothing)).reduce

  private def transformAddressVariation: Reads[JsObject] = (transformAddress and
    ((__ \ 'correspondenceAddressDetails \ 'changeFlag).json.copyFrom((__ \ 'addressChange).json.pick) orElse
      (__ \ 'correspondenceAddressDetails \ 'changeFlag).json.put(JsBoolean(false)))).reduce

  private def nonUKAddress: Reads[JsObject] =
    (__ \ 'contactAddress \ 'country).read[String].flatMap {
      case "GB" =>
        (__ \ 'correspondenceAddressDetails \ 'nonUKAddress).json.put(JsString("false"))
      case _ =>
        (__ \ 'correspondenceAddressDetails \ 'nonUKAddress).json.put(JsString("true"))
    }

  private def transformContactDetails: Reads[JsObject] =
    ((__ \ 'correspondenceContactDetails \ 'email).json.copyFrom((__ \ 'email).json.pick) and
      (__ \ 'correspondenceContactDetails \ 'telephone).json.copyFrom((__ \ 'phone).json.pick)).reduce

  private def transformDeclaration: Reads[JsObject] = (__ \ 'declaration \ 'pspDeclarationBox1).json.put(JsBoolean(true))

}
