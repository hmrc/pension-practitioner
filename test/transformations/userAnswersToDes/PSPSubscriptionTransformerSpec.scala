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

import org.scalatest.FreeSpec
import org.scalatest.MustMatchers
import org.scalatest.OptionValues
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import utils.TestHelpers._

class PSPSubscriptionTransformerSpec
  extends FreeSpec
    with MustMatchers
    with OptionValues {

  import PSPSubscriptionTransformerSpec._

  "A PSPSubscriptionTransformer" - {
    "in Subscription journey" - {
      "must transform from UserAnswers to ETMP AFT Return format for UK individual" in {
        val transformer = new PSPSubscriptionTransformer

        val transformedJson = uaIndividualUK.transform(transformer.transformPsp).asOpt.value
        transformedJson mustBe individualUK
      }

      "must transform from UserAnswers to ETMP AFT Return format for Non UK individual" in {
        val transformer = new PSPSubscriptionTransformer

        val transformedJson = uaIndividualNonUk.transform(transformer.transformPsp).asOpt.value
        transformedJson mustBe individualNonUk
      }

      "must transform from UserAnswers to ETMP AFT Return format for existing UK company" in {
        val transformer = new PSPSubscriptionTransformer

        val transformedJson =uaCompanyUk.transform(transformer.transformPsp).asOpt.value
        transformedJson mustBe companyUK
      }

      "must transform from UserAnswers to ETMP AFT Return format for Non UK partnership" in {
        val transformer = new PSPSubscriptionTransformer

        val transformedJson = uaPartnershipNonUK.transform(transformer.transformPsp).asOpt.value
        transformedJson mustBe partnershipNonUK
      }
    }

    "in Variations journey" - {
      "must transform from UserAnswers to ETMP AFT Return format for UK individual" in {
        val transformer = new PSPSubscriptionTransformer

        val transformedJson = uaIndividualUKVariation.transform(transformer.transformPsp).asOpt.value
        transformedJson mustBe individualUKVariation
      }

      "must transform from UserAnswers to ETMP AFT Return format for Non UK individual" in {
        val transformer = new PSPSubscriptionTransformer

        val transformedJson = uaIndividualNonUkVariation.transform(transformer.transformPsp).asOpt.value
        transformedJson mustBe individualNonUkVariation
      }

      "must transform from UserAnswers to ETMP AFT Return format for existing UK company" in {
        val transformer = new PSPSubscriptionTransformer

        val transformedJson = uaCompanyNonUkVariation.transform(transformer.transformPsp).asOpt.value
        transformedJson mustBe companyNonUKVariation
      }

      "must transform from UserAnswers to ETMP AFT Return format for Non UK partnership" in {
        val transformer = new PSPSubscriptionTransformer

        val transformedJson = uaPartnershipUKVariation.transform(transformer.transformPsp).asOpt.value
        transformedJson mustBe partnershipUKVariation
      }
    }
  }
}

object PSPSubscriptionTransformerSpec {

  val uaIndividualUK: JsObject = Json.obj(
    "registrationInfo" -> (ukIndividual ++ uaSapNo),
    "individualDetails" -> individualDetails,
    "existingPSP" -> Json.obj("isExistingPSP" -> false),
    "contactAddress" -> uaUkAddress
  ) ++ uaCorrespondenceContactDetails

  val uaIndividualUKVariation: JsObject = Json.obj(
    "subscriptionType" -> "Variation",
    "registrationInfo" -> ukIndividual,
    "individualDetails" -> individualDetails,
    "contactAddress" -> uaUkAddress
  ) ++ pspVariation ++ uaCorrespondenceContactDetails ++ uaAddressChange

  val uaIndividualNonUk: JsObject = Json.obj(
    "registrationInfo" -> (nonUkIndividual ++ uaSapNo),
    "individualDetails" -> individualDetails,
    "contactAddress" -> uaNonUkAddress,
    "existingPSP" -> Json.obj("isExistingPSP" -> false)
  ) ++ uaCorrespondenceContactDetails

  val uaIndividualNonUkVariation: JsObject = Json.obj(
    "subscriptionType" -> "Variation",
    "registrationInfo" -> nonUkIndividual,
    "individualDetails" -> individualDetails,
    "contactAddress" -> uaNonUkAddress
  ) ++ pspVariation ++ uaCorrespondenceContactDetails ++ uaNameChange ++ uaAddressChange

  private val uaCompanyUk: JsObject = Json.obj(
    "registrationInfo" -> (ukOrg("Company") ++ uaSapNo),
    "name" -> "Test Ltd",
    "contactAddress" -> uaUkAddress
  ) ++ uaExistingPsp ++ uaCorrespondenceContactDetails

  private val uaPartnershipNonUK: JsObject = Json.obj(
    "registrationInfo" -> (nonUkOrg("Partnership") ++ uaSapNo),
    "name" -> "Testing Ltd",
    "contactAddress" -> uaNonUkAddress,
    "existingPSP" -> Json.obj("isExistingPSP" -> false)
  ) ++ uaCorrespondenceContactDetails

  private val uaCompanyNonUkVariation: JsObject = Json.obj(
    "subscriptionType" -> "Variation",
    "registrationInfo" -> nonUkOrg("Company"),
    "name" -> "Test Ltd",
    "contactAddress" -> uaUkAddress
  ) ++ pspVariation ++ uaCorrespondenceContactDetails ++ uaAddressChange ++ uaNameChange

  private val uaPartnershipUKVariation: JsObject = Json.obj(
    "subscriptionType" -> "Variation",
    "registrationInfo" -> ukOrg("Partnership"),
    "name" -> "Testing Ltd",
    "contactAddress" -> uaNonUkAddress,
    "existingPSP" -> Json.obj("isExistingPSP" -> false)
  ) ++ pspVariation ++ uaCorrespondenceContactDetails ++ uaAddressChange ++ uaNameChange


  private def declarationCreation: JsObject = {
    Json.obj(
      "subscriptionTypeAndPSPIDDetails" -> Json.obj(
        "existingPSPID" -> "Yes",
        "pspid" -> "A2345678",
        "subscriptionType" -> "Creation"
      ),
      "sapNumber" -> "1234567890"
    ) ++ declarationNRegime
  }

  private def declarationVariation: JsObject =
    Json.obj("subscriptionTypeAndPSPIDDetails" -> Json.obj(
        "pspid" -> "A2345678",
        "subscriptionType" -> "Variation"
      )
    ) ++ declarationNRegime

  private val individualUK: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> ukIndividual,
    "individualDetails" -> individualDetails,
    "correspondenceContactDetails" -> correspondenceContactDetails,
      "subscriptionTypeAndPSPIDDetails" -> Json.obj(
        "existingPSPID" -> "No",
        "subscriptionType" -> "Creation"),
    "sapNumber" -> "1234567890",
    "correspondenceAddressDetails" -> ukAddress

  ) ++ declarationNRegime

  private val individualNonUk: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> nonUkIndividual,
    "individualDetails" -> individualDetails,
    "correspondenceContactDetails" -> correspondenceContactDetails,
    "correspondenceAddressDetails" -> nonUkAddress,
    "subscriptionTypeAndPSPIDDetails" -> Json.obj(
      "existingPSPID" -> "No",
      "subscriptionType" -> "Creation"),
    "sapNumber" -> "1234567890"
  ) ++ declarationNRegime

  private val companyUK: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> ukOrg("Company"),
    "orgOrPartnershipDetails" -> Json.obj("organisationName" -> "Test Ltd"),
    "correspondenceContactDetails" -> correspondenceContactDetails,
    "correspondenceAddressDetails" -> ukAddress
  ) ++ declarationCreation

  private val partnershipNonUK: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> nonUkOrg("Partnership"),
    "orgOrPartnershipDetails" -> Json.obj("organisationName" -> "Testing Ltd"),
    "correspondenceContactDetails" -> correspondenceContactDetails,
    "correspondenceAddressDetails" -> nonUkAddress,
    "subscriptionTypeAndPSPIDDetails" -> Json.obj(
      "existingPSPID" -> "No",
      "subscriptionType" -> "Creation"),
    "sapNumber" -> "1234567890"
  ) ++ declarationNRegime

  private val individualUKVariation: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> ukIndividual,
    "correspondenceContactDetails" -> correspondenceContactDetails,
    "correspondenceAddressDetails" -> (ukAddress ++ changeFlag),
    "individualDetails" -> individualDetails
  ) ++ declarationVariation

  private val individualNonUkVariation: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> nonUkIndividual,
    "correspondenceContactDetails" -> correspondenceContactDetails,
    "correspondenceAddressDetails" -> (nonUkAddress ++ changeFlag),
    "individualDetails" -> (individualDetails ++ changeFlag)
  ) ++ declarationVariation

  private val companyNonUKVariation: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> nonUkOrg("Company"),
    "orgOrPartnershipDetails" -> (Json.obj("organisationName" -> "Test Ltd") ++ changeFlag),
    "correspondenceContactDetails" -> correspondenceContactDetails,
    "correspondenceAddressDetails" -> (ukAddress ++ changeFlag)
  ) ++ declarationVariation

  private val partnershipUKVariation: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> ukOrg("Partnership"),
    "orgOrPartnershipDetails" -> Json.obj("organisationName" -> "Testing Ltd"),
    "correspondenceContactDetails" -> correspondenceContactDetails,
    "correspondenceAddressDetails" -> (nonUkAddress ++ changeFlag)
  ) ++ declarationVariation

}
