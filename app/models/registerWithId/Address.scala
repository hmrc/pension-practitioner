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

package models.registerWithId

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait Address

object Address {
  implicit val reads: Reads[Address] = ((__ \ "countryCode").read[String] orElse (__ \ "country").read[String]).flatMap(countryCode =>
    getReadsBasedOnCountry[UkAddress, InternationalAddress](UkAddress.apiReads, InternationalAddress.apiReads, countryCode))


  implicit val defaultWrites: Writes[Address] = Writes {
    case address: UkAddress =>
      UkAddress.apiWries.writes(address)
    case address: InternationalAddress =>
      InternationalAddress.apiWries.writes(address)
  }

  private def getReadsBasedOnCountry[T, B](ukAddressReads: Reads[T], nonUkAddressReads: Reads[B], countryCode: String) = {
    if (countryCode == "GB") ukAddressReads.map(c => c.asInstanceOf[Address]) else nonUkAddressReads.map(c => c.asInstanceOf[Address])
  }

  private def getCountryOrTerritoryCode(countryCode: String) = {
    if (countryCode.contains("territory")) countryCode.split(":").last.trim() else countryCode
  }

  val commonAddressElementsReads: Reads[(String, Option[String], Option[String], Option[String], String)] = (
    (JsPath \ "addressLine1").read[String] and
      (JsPath \ "addressLine2").readNullable[String] and
      (JsPath \ "addressLine3").readNullable[String] and
      (JsPath \ "addressLine4").readNullable[String] and
      ((JsPath \ "countryCode").read[String] orElse (JsPath \ "country").read[String])
    ) ((line1, line2, line3, line4, countryCode) => (line1, line2, line3, line4, getCountryOrTerritoryCode(countryCode)))
}

case class UkAddress(addressLine1: String, addressLine2: Option[String] = None, addressLine3: Option[String] = None,
                     addressLine4: Option[String] = None, countryCode: String, postalCode: String) extends Address

object UkAddress {

  val apiReads: Reads[UkAddress] = (
    JsPath.read(using Address.commonAddressElementsReads) and
      (JsPath \ "postalCode").read[String]
    ) ((common, postalCode) => UkAddress(common._1, common._2, common._3, common._4, common._5, postalCode))

  val apiWries: Writes[UkAddress] = Json.writes[UkAddress]
}

case class InternationalAddress(addressLine1: String, addressLine2: Option[String] = None, addressLine3: Option[String] = None,
                                addressLine4: Option[String] = None, countryCode: String,
                                postalCode: Option[String] = None) extends Address

object InternationalAddress {
  val apiReads: Reads[InternationalAddress] = (
    JsPath.read(using Address.commonAddressElementsReads) and
      (JsPath \ "postalCode").readNullable[String]
    ) ((common, postalCode) => InternationalAddress(common._1, common._2, common._3, common._4, common._5, postalCode))

  val apiWries: Writes[InternationalAddress] = Json.writes[InternationalAddress]
}

