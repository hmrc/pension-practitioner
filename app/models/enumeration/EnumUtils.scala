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

package models.enumeration

import play.api.libs.json._

import scala.language.implicitConversions

object EnumUtils {
  def enumReads[E <: Enumeration](enumValue: E): Reads[enumValue.Value] = {
    case JsString(s) =>
      try {
        JsSuccess(enumValue.withName(s))
      } catch {
        case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enumValue.getClass}'," +
          s"but it does not appear to contain the value: '$s'")
      }
    case _ => JsError("String value expected")
  }

  implicit def enumWrites[E <: Enumeration](enumValue: E): Writes[enumValue.Value] = (v: enumValue.Value) => JsString(v.toString)

  implicit def enumFormat[E <: Enumeration](enumValue: E): Format[enumValue.Value] = {
    Format(enumReads(enumValue), enumWrites(enumValue))
  }
}
