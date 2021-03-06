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
    ((__ \ 'applicationDate).json.copyFrom((__ \ 'subscriptionTypeAndPSPIDDetails \ 'applicationDate).json.pick) and
      (__ \ 'subscriptionType).json.copyFrom((__ \ 'subscriptionTypeAndPSPIDDetails \ 'subscriptionType).json.pick) and
      ((__ \ 'existingPSP \ 'isExistingPSP).json.copyFrom((__ \ 'subscriptionTypeAndPSPIDDetails \ 'existingPSPID).json.pick) orElse doNothing) and
      (__ \ 'existingPSP \ 'existingPSPId).json.copyFrom((__ \ 'subscriptionTypeAndPSPIDDetails \ 'pspid).json.pick)).reduce

  private def transformLegalAndCustomer: Reads[JsObject] =
    ((__ \ 'registrationInfo \ 'legalStatus).json.copyFrom((__ \ 'legalEntityAndCustomerID \ 'legalStatus).json.pick) and
      (__ \ 'registrationInfo \ 'customerType).json.copyFrom((__ \ 'legalEntityAndCustomerID \ 'customerType).json.pick) and
      ((__ \ 'registrationInfo \ 'idType).json.copyFrom((__ \ 'legalEntityAndCustomerID \ 'idType).json.pick) orElse doNothing) and
      ((__ \ 'registrationInfo \ 'idNumber).json.copyFrom((__ \ 'legalEntityAndCustomerID \ 'idNumber).json.pick) orElse doNothing)).reduce

  private def transformName: Reads[JsObject] =
    (__ \ 'legalEntityAndCustomerID \ 'legalStatus).read[String].flatMap {
      case "Individual" =>
        ((__ \ 'individualDetails \ 'firstName).json.copyFrom((__ \ 'individualDetails \ 'firstName).json.pick) and
          (__ \ 'individualDetails \ 'lastName).json.copyFrom((__ \ 'individualDetails \ 'lastName).json.pick)).reduce
      case _ =>
        (__ \ 'name).json.copyFrom((__ \ 'orgOrPartnershipDetails \ 'organisationName).json.pick)
    }

  private def transformAddress: Reads[JsObject] =
    ((__ \ 'contactAddress \ 'addressLine1).json.copyFrom((__ \ 'correspondenceAddressDetails \ 'addressLine1).json.pick) and
      (__ \ 'contactAddress \ 'addressLine2).json.copyFrom((__ \ 'correspondenceAddressDetails \ 'addressLine2).json.pick) and
      ((__ \ 'contactAddress \ 'addressLine3).json.copyFrom((__ \ 'correspondenceAddressDetails \ 'addressLine3).json.pick) orElse doNothing) and
      ((__ \ 'contactAddress \ 'addressLine4).json.copyFrom((__ \ 'correspondenceAddressDetails \ 'addressLine4).json.pick) orElse doNothing) and
      (__ \ 'contactAddress \ 'country).json.copyFrom((__ \ 'correspondenceAddressDetails \ 'countryCode).json.pick) and
      ((__ \ 'contactAddress \ 'postcode).json.copyFrom((__ \ 'correspondenceAddressDetails \ 'postalCode).json.pick) orElse doNothing)
      ).reduce


  private def transformContactDetails: Reads[JsObject] =
    ((__ \ 'email).json.copyFrom((__ \ 'correspondenceContactDetails \ 'email).json.pick) and
      (__ \ 'phone).json.copyFrom((__ \ 'correspondenceContactDetails \ 'telephone).json.pick)).reduce
}
