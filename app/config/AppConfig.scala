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

package config

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig, env: Environment) {

  lazy val appName: String = config.get[String](path = "appName")

  private val baseURL: String = servicesConfig.baseUrl(serviceName = "des-hod")
  private val ifURL: String = servicesConfig.baseUrl(serviceName = "if-hod")
  lazy val baseUrlPensionsScheme: String = servicesConfig.baseUrl("pensions-scheme")
  lazy val desEnvironment: String = config.getOptional[String]("microservice.services.des-hod.env").getOrElse("local")
  lazy val authorization: String = "Bearer " + config.getOptional[String]("microservice.services.des-hod.authorizationToken").getOrElse("local")

  lazy val integrationFrameworkEnvironment: String = config.getOptional[String](
    path = "microservice.services.if-hod.env").getOrElse("local")
  lazy val integrationFrameworkAuthorization: String = "Bearer " + config.getOptional[String](
    path = "microservice.services.if-hod.authorizationToken").getOrElse("local")

  lazy val registerWithoutIdOrganisationUrl: String = s"$baseURL${config.get[String](path = "serviceUrls.register-without-id-organisation")}"
  lazy val registerWithoutIdIndividualUrl: String = s"$baseURL${config.get[String](path = "serviceUrls.register-without-id-individual")}"
  lazy val registerWithIdIndividualUrl: String = s"$baseURL${config.get[String](path = "serviceUrls.register-with-id-individual")}"
  lazy val registerWithIdOrganisationUrl: String = s"$baseURL${config.get[String](path = "serviceUrls.register-with-id-organisation")}"
  lazy val pspSubscriptionUrl: String = s"$ifURL${config.get[String](path = "serviceUrls.psp-subscription")}"
  lazy val pspDeregistrationUrl: String = s"$ifURL${config.get[String](path = "serviceUrls.psp-de-register")}"
  lazy val subscriptionDetailsUrl: String = s"$ifURL${config.get[String](path = "serviceUrls.get-subscription-details")}"
  lazy val pspAuthorisationUrl: String = s"$ifURL${config.get[String](path = "serviceUrls.psp-association")}"
  lazy val pspDeAuthorisationUrl: String = s"$ifURL${config.get[String](path = "serviceUrls.psp-de-authorisation")}"
  lazy val minimalDetailsUrl: String = s"$ifURL${config.get[String]("serviceUrls.minimal-details")}"
  lazy val listOfSchemesUrl: String = s"$baseUrlPensionsScheme${config.get[String]("serviceUrls.listOfSchemes")}"

  val mongoEncryptionKey: Option[String] = config.getOptional[String]("mongodb.encryption.key") match {
      case None if env.mode == Mode.Prod => throw new RuntimeException("Encryption key is not set")
      case x => x
    }

}
