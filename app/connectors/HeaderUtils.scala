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

package connectors

import com.google.inject.Inject
import config.AppConfig

import java.util.UUID.randomUUID

class HeaderUtils @Inject()(config: AppConfig) {
  private[connectors] val maxLengthCorrelationId = 32
  private[connectors] val maxLengthCorrelationIF = 36

  def desHeaderWithoutCorrelationId: Seq[(String, String)] = {
    Seq(
      "Environment" -> config.desEnvironment,
      "Authorization" -> config.authorization,
      "Content-Type" -> "application/json"
    )
  }

  def desHeader(): Seq[(String, String)] = {

    Seq(
      "Environment" -> config.desEnvironment,
      "Authorization" -> config.authorization,
      "Content-Type" -> "application/json",
      "CorrelationId" -> getCorrelationId)
  }

  def integrationFrameworkHeader(): Seq[(String, String)] = {
    Seq(
      "Environment" -> config.integrationFrameworkEnvironment,
      "Authorization" -> config.integrationFrameworkAuthorization,
      "Content-Type" -> "application/json",
      "CorrelationId" -> getCorrelationIdIF)
  }

  def getCorrelationId: String = randomUUID.toString
    .replaceAll("-", "")
    .slice(0, maxLengthCorrelationId)

  def getCorrelationIdIF: String = randomUUID.toString.slice(0, maxLengthCorrelationIF)
}
