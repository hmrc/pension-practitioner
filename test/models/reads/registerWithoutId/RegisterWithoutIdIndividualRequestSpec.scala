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

package models.reads.registerWithoutId

import models.registerWithoutId
import models.registerWithoutId.{Address, RegisterWithoutIdIndividualRequest, RegisterWithoutIdResponse}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsResultException, JsValue, Json}

class RegisterWithoutIdIndividualRequestSpec extends AnyFlatSpec with Matchers {

  import RegisterWithoutIdIndividualRequestSpec._

  "RegistrationNoIdIndividualRequest.apiWrites" should "transform a request with full address details" in {

    val actual = Json.toJson(fullAddressRequest)(RegisterWithoutIdIndividualRequest.writesRegistrationNoIdIndividualRequest(acknowledgementReference))

    actual shouldEqual expectedFullAddressJson

  }

  it should "transform a request with minimal address details" in {

    val actual = Json.toJson(minimalAddressRequest)(RegisterWithoutIdIndividualRequest.writesRegistrationNoIdIndividualRequest(acknowledgementReference))

    actual shouldEqual expectedMinimalAddressJson

  }

  "RegistrationNoIdIndividualResponse.apiReads" should "transform a success response" in {

    val actual = responseJson.validate[RegisterWithoutIdResponse]

    actual.fold(
      errors => {
        fail(
          "RegistrationNoIdIndividualResponse is not valid",
          JsResultException(errors)
        )
      },
      response => response shouldBe expectedResponse
    )
  }
}

// scalastyle:off magic.number

object RegisterWithoutIdIndividualRequestSpec {

  private val acknowledgementReference = "test-acknowledgement-reference"

  private val fullAddressRequest = RegisterWithoutIdIndividualRequest(
    "John",
    "Smith",
    Address(
      "100, Sutton Street",
      "Wokingham",
      Some("Surrey"),
      Some("London"),
      Some("DH1 4EJ"),
      "GB"
    )
  )

  private val expectedFullAddressJson: JsValue = Json.parse(
    """
      |{
      |  "regime": "PODP",
      |  "acknowledgementReference": "test-acknowledgement-reference",
      |  "isAnAgent": false,
      |  "isAGroup": false,
      |  "individual": {
      |    "firstName": "John",
      |    "lastName": "Smith"
      |    },
      |  "address": {
      |    "addressLine1": "100, Sutton Street",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": "Surrey",
      |    "addressLine4": "London",
      |    "postalCode": "DH1 4EJ",
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {}
      |}
    """.stripMargin
  )

  private val minimalAddressRequest = registerWithoutId.RegisterWithoutIdIndividualRequest(
    "John",
    "Smith",
    Address(
      "100, Sutton Street",
      "Wokingham",
      None,
      None,
      None,
      "GB"
    )
  )

  private val expectedMinimalAddressJson: JsValue = Json.parse(
    """
      |{
      |  "regime": "PODP",
      |  "acknowledgementReference": "test-acknowledgement-reference",
      |  "isAnAgent": false,
      |  "isAGroup": false,
      |  "individual": {
      |    "firstName": "John",
      |    "lastName": "Smith"
      |    },
      |  "address": {
      |    "addressLine1": "100, Sutton Street",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": null,
      |    "addressLine4": null,
      |    "postalCode": null,
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {}
      |}
    """.stripMargin
  )

  private val responseJson: JsValue = Json.parse(
    """
      |{
      |  "processingDate": "2001-12-17T09:30:472",
      |  "sapNumber": "1234567890",
      |  "safeId": "XE0001234567890"
      |}
    """.stripMargin
  )

  private val expectedResponse = RegisterWithoutIdResponse("XE0001234567890", "1234567890")

}
