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

import com.google.inject.Inject
import config.AppConfig
import uk.gov.hmrc.crypto.{AdDecrypter, AdEncrypter, EncryptedValue, SymmetricCryptoFactory}


class AesGcmAdCrypto @Inject()(aesGcmAdCryptoFactory: AesGcmAdCryptoFactory) {
  private lazy val aesGcmAdCrypto = aesGcmAdCryptoFactory.instance()

  def encrypt(valueToEncrypt: String, associatedText: String): EncryptedValue = {
      aesGcmAdCrypto.encrypt(valueToEncrypt, associatedText)
  }

  def decrypt(encryptedValue: EncryptedValue, associatedText: String): String = {
      aesGcmAdCrypto.decrypt(encryptedValue, associatedText)
  }
}

class AesGcmAdCryptoFactory @Inject()(appConfig: AppConfig) {

  private lazy val aesGcmAdCrypto = SymmetricCryptoFactory.aesGcmAdCrypto(appConfig.mongoEncryptionKey)

  def instance(): AdEncrypter with AdDecrypter = aesGcmAdCrypto
}
