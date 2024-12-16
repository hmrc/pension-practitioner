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

package crypto

import play.api.libs.json.{JsValue, Json, OFormat}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.crypto.EncryptedValue

@Singleton
class DataEncryptor @Inject()(cipher: AesGcmAdCrypto){
  implicit val encryptedValueFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  def encrypt(id: String, data: JsValue): EncryptedValue = {
    cipher.encrypt(data.toString, id)
  }

  def decrypt(id:String, jsValue: JsValue): JsValue = {
    jsValue.validate[EncryptedValue]
      .map { encryptedValue =>
          Json.parse(cipher.decrypt(encryptedValue, id))
      }.getOrElse(jsValue)
  }
}
