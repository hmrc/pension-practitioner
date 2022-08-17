/*
 * Copyright 2022 HM Revenue & Customs
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

package models.reads.registerWithId

import models.registerWithId.{Address, InternationalAddress, UkAddress}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json._

class AddressReadsSpec extends AnyWordSpec with Matchers with OptionValues {
  "A JSON Payload with an address" must {
    "map correctly to an Address type" when {

      val address = Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
        "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
        "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))
      val ukAddressSample = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "NE1")

      "we have a UK address" when {
        val result = address.as[Address].asInstanceOf[UkAddress]
        "with addressLine 1" in {
          result.addressLine1 mustBe ukAddressSample.addressLine1
        }

        "with addressLine 2" in {
          result.addressLine2 mustBe ukAddressSample.addressLine2
        }

        "with an optional addressLine 3" in {
          result.addressLine3 mustBe ukAddressSample.addressLine3
        }

        "with an optional addressLine 4" in {
          result.addressLine4 mustBe ukAddressSample.addressLine4
        }

        "with a postalCode" in {
          result.postalCode mustBe ukAddressSample.postalCode
        }

        "with countryCode" in {
          result.countryCode mustBe ukAddressSample.countryCode
        }

      }

      "we have a non UK address" when {
        val nonUkAddressSample = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("NE1"))
        val address = Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
          "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"), "countryCode" -> JsString("IT"))
        val result = address.as[Address].asInstanceOf[InternationalAddress]

        "with addressLine 1" in {
          result.addressLine1 mustBe ukAddressSample.addressLine1
        }

        "with optional addressLine 2" in {
          result.addressLine2 mustBe ukAddressSample.addressLine2
        }

        "with an optional addressLine 3" in {
          result.addressLine3 mustBe ukAddressSample.addressLine3
        }

        "with an optional addressLine 4" in {
          result.addressLine4 mustBe ukAddressSample.addressLine4
        }

        "with no postal code" in {
          val result = address.as[Address]

          result.asInstanceOf[InternationalAddress].postalCode mustBe None
        }

        "with postal code" in {
          val input = address + ("postalCode" -> JsString("NE1"))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].postalCode mustBe nonUkAddressSample.postalCode
        }

        "with territory defined as country code" in {
          val input = address + ("countryCode" -> JsString("territory:IT"))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].countryCode mustBe nonUkAddressSample.countryCode
        }

        "with territory defined as country code with leading space" in {
          val input = address + ("countryCode" -> JsString("territory: IT"))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].countryCode mustBe nonUkAddressSample.countryCode
        }
      }
    }
  }
}
