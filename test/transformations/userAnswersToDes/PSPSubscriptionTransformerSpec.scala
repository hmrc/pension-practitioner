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

package transformations.userAnswersToDes

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.{JsObject, Json}

class PSPSubscriptionTransformerSpec extends FreeSpec with MustMatchers with OptionValues {

  import PSPSubscriptionTransformerSpec._

  "A PSPSubscriptionTransformer" - {
    "must transform from UserAnswers to ETMP AFT Return format for UK individual" in {
      val transformer = new PSPSubscriptionTransformer

      val transformedJson = uaIndividualUK.transform(transformer.transformPspSubscription).asOpt.value
      transformedJson mustBe individualUK
    }

    "must transform from UserAnswers to ETMP AFT Return format for Non UK individual" in {
      val transformer = new PSPSubscriptionTransformer

      val transformedJson = uaIndividualNonUk.transform(transformer.transformPspSubscription).asOpt.value
      transformedJson mustBe individualNonUk
    }

    "must transform from UserAnswers to ETMP AFT Return format for UK company" in {
      val transformer = new PSPSubscriptionTransformer

      val transformedJson = uaCompanyUk.transform(transformer.transformPspSubscription).asOpt.value
      transformedJson mustBe companyUK
    }

    "must transform from UserAnswers to ETMP AFT Return format for Non UK partnership" in {
      val transformer = new PSPSubscriptionTransformer

      val transformedJson = uaPartnershipNonUK.transform(transformer.transformPspSubscription).asOpt.value
      transformedJson mustBe partnershipNonUK
    }
  }

}

object PSPSubscriptionTransformerSpec {

  private val individualDetails: JsObject = Json.obj(
    "individualDetails" -> Json.obj(
      "firstName" -> "Stephen",
      "lastName" -> "Wood"
    )
  )

  private val uaAddress: JsObject = Json.obj( "contactAddress" -> Json.obj(
    "addressLine1" -> "4 Other Place",
    "addressLine2" -> "Some District",
    "addressLine3" -> "Anytown",
    "addressLine4" -> "Somerset",
    "postcode" -> "ZZ1 1ZZ",
    "country" -> "GB"
  ))

  private val uaAddressNonUk: JsObject = Json.obj( "contactAddress" -> Json.obj(
    "addressLine1" -> "4 Other Place",
    "addressLine2" -> "Some District",
    "addressLine3" -> "Anytown",
    "addressLine4" -> "Somerset",
    "country" -> "FR"
  ))

  private val uaContactDetails: JsObject = Json.obj(
    "email" -> "sdd@ds.sd",
    "phone" -> "3445"
  )

  val uaIndividualUK: JsObject = Json.obj(
    "registrationInfo" -> Json.obj(
      "legalStatus" -> "Individual",
      "sapNumber" -> "1234567890",
      "noIdentifier" -> false,
      "customerType" -> "UK",
      "idType" -> "NINO",
      "idNumber" -> "AB123456C"
    )
  ) ++ individualDetails ++ uaAddress ++ uaContactDetails

  private val uaIndividualNonUk: JsObject = Json.obj(
    "registrationInfo"  -> Json.obj(
      "legalStatus"  ->  "Individual",
      "sapNumber"  ->  "1234567890",
      "noIdentifier"  ->  true,
      "customerType"  ->  "NonUK"
    )
  ) ++ individualDetails ++ uaAddressNonUk ++ uaContactDetails
  
  private val uaCompanyUk: JsObject = Json.obj(
      "registrationInfo"  ->  Json.obj(
        "legalStatus"  ->  "Company",
        "sapNumber"  ->  "1234567890",
        "noIdentifier"  ->  false,
        "customerType"  ->  "UK",
        "idType"  ->  "UTR",
        "idNumber"  ->  "1234567890"
      ),
      "name"  ->  "Test Ltd"
  ) ++ uaAddress ++ uaContactDetails

  private val uaPartnershipNonUK: JsObject = Json.obj(
    "registrationInfo"  ->  Json.obj(
      "legalStatus"  ->  "Partnership",
      "sapNumber"  ->  "1234567890",
      "noIdentifier"  ->  false,
      "customerType"  ->  "NonUK"
    ),
    "name"  ->  "Testing Ltd"
  ) ++ uaAddressNonUk ++ uaContactDetails

  private val contactDetails: JsObject = Json.obj(
    "correspondenceContactDetails" -> Json.obj(
      "telephone" -> "3445",
      "email" -> "sdd@ds.sd"
    ))

  private val address: JsObject = Json.obj(
    "correspondenceAddressDetails" -> Json.obj(
      "countryCode" -> "GB",
      "postalCode" -> "ZZ1 1ZZ",
      "addressLine1" -> "4 Other Place",
      "addressLine2" -> "Some District",
      "addressLine3" -> "Anytown",
      "addressLine4" -> "Somerset",
      "nonUKAddress" -> "false"
    )
  )

  private val addressNonUk: JsObject = Json.obj(
    "correspondenceAddressDetails" -> Json.obj(
      "countryCode" -> "FR",
      "addressLine1" -> "4 Other Place",
      "addressLine2" -> "Some District",
      "addressLine3" -> "Anytown",
      "addressLine4" -> "Somerset",
      "nonUKAddress" -> "true"
    )
  )

  private val declarationEtcDetails: JsObject = Json.obj(
    "regime" -> "PODP",
    "subscriptionTypeAndPSPIDDetails" -> Json.obj(
      "existingPSPID" -> "No",
      "subscriptionType" -> "Creation "
    ),
    "sapNumber" -> "1234567890",
    "declaration" -> Json.obj(
      "pspDeclarationBox1" -> true
    )
  )

  private val individualUK: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> Json.obj(
      "customerType" -> "UK",
      "idType" -> "NINO",
      "legalStatus" -> "Individual",
      "idNumber" -> "AB123456C"
    )) ++ individualDetails ++ address ++ contactDetails ++ declarationEtcDetails


  private val individualNonUk: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> Json.obj(
      "customerType" -> "NonUK",
      "legalStatus" -> "Individual"
    )) ++ individualDetails ++ addressNonUk ++ contactDetails ++ declarationEtcDetails

  private val companyUK: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> Json.obj(
      "customerType" -> "UK",
      "idType" -> "UTR",
      "legalStatus" -> "Company",
      "idNumber" -> "1234567890"
    ),
    "orgOrPartnershipDetails" -> Json.obj(
      "organisationName" -> "Test Ltd"
    )
  ) ++ address ++ contactDetails ++ declarationEtcDetails

  private val partnershipNonUK: JsObject = Json.obj(
    "legalEntityAndCustomerID" -> Json.obj(
      "customerType" -> "NonUK",
      "legalStatus" -> "Partnership"
    ),
    "orgOrPartnershipDetails" -> Json.obj(
      "organisationName" -> "Testing Ltd"
    )
  ) ++ addressNonUk ++ contactDetails ++ declarationEtcDetails

}