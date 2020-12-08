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

package transformations.toUserAnswers

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.{JsObject, Json}

class PspDetailsTransformerSpec extends FreeSpec with MustMatchers with OptionValues {

  import PspDetailsTransformerSpec._

  val transformer = new PspDetailsTransformer
  "A PspDetailsTransformer" - {
    "must transform from UserAnswers to ETMP AFT Return format for UK individual" in {
      val transformedJson = individualUK.transform(transformer.transformToUserAnswers).asOpt.value
      transformedJson mustBe uaIndividualUK
    }

    "must transform from UserAnswers to ETMP AFT Return format for Non UK individual" in {
      val transformedJson = individualNonUk.transform(transformer.transformToUserAnswers).asOpt.value
      transformedJson mustBe uaIndividualNonUk
    }

    "must transform from UserAnswers to ETMP AFT Return format for UK company" in {
      val transformedJson = companyUK.transform(transformer.transformToUserAnswers).asOpt.value
      transformedJson mustBe uaCompanyUk
    }

    "must transform from UserAnswers to ETMP AFT Return format for Non UK partnership" in {
      val transformedJson = partnershipNonUK.transform(transformer.transformToUserAnswers).asOpt.value
      transformedJson mustBe uaPartnershipNonUK
    }
  }

}

object PspDetailsTransformerSpec {

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

  private val existingPsp: JsObject = Json.obj(
    "existingPSP" -> Json.obj(
      "existingPSPId" -> "A2345678",
      "isExistingPSP" -> "Yes"
    ),
    "subscriptionType" -> "Creation",
    "applicationDate" -> "2020-01-01"
  )

  private val whatTypeBusinessYourself = Json.obj(
    "whatTypeBusiness" -> "yourselfAsIndividual"
  )

  private val whatTypeBusinessCompanyOrPartnership = Json.obj(
    "whatTypeBusiness" -> "companyOrPartnership"
  )

  private val businessTypeCompany = Json.obj(
    "businessType" -> "limitedCompany"
  )

  private val businessRegistrationTypePartnership = Json.obj(
    "businessRegistrationType" -> "partnership"
  )

  val uaIndividualUK: JsObject = Json.obj(
    "registrationInfo" -> Json.obj(
      "legalStatus" -> "Individual",
      "customerType" -> "UK",
      "idType" -> "NINO",
      "idNumber" -> "AB123456C"
    )
  ) ++ individualDetails ++ uaAddress ++ uaContactDetails ++ existingPsp ++ whatTypeBusinessYourself

  private val uaIndividualNonUk: JsObject = Json.obj(
    "registrationInfo"  -> Json.obj(
      "legalStatus"  ->  "Individual",
      "customerType"  ->  "NonUK"
    )
  ) ++ individualDetails ++ uaAddressNonUk ++ uaContactDetails ++ existingPsp ++ whatTypeBusinessYourself

  private val uaCompanyUk: JsObject = Json.obj(
    "registrationInfo"  ->  Json.obj(
      "legalStatus"  ->  "Company",
      "customerType"  ->  "UK",
      "idType"  ->  "UTR",
      "idNumber"  ->  "1234567890"
    ),
    "name"  ->  "Test Ltd"
  ) ++ uaAddress ++ uaContactDetails ++ existingPsp ++ whatTypeBusinessCompanyOrPartnership ++ businessTypeCompany

  private val uaPartnershipNonUK: JsObject = Json.obj(
    "registrationInfo"  ->  Json.obj(
      "legalStatus"  ->  "Partnership",
      "customerType"  ->  "NonUK"
    ),
    "name"  ->  "Testing Ltd"
  ) ++ uaAddressNonUk ++ uaContactDetails ++ existingPsp ++ whatTypeBusinessCompanyOrPartnership ++ businessRegistrationTypePartnership

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
      "existingPSPID" -> "Yes",
      "pspid" -> "A2345678",
      "subscriptionType" -> "Creation",
      "applicationDate" -> "2020-01-01"
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
