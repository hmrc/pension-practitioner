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

import models.enumeration.OrganisationTypeEnum
import play.api.libs.json.*

case class RegisterWithIdResponse(
                                   safeId: String,
                                   sapNumber: String,
                                   isAnIndividual: Boolean,
                                   individual: Option[IndividualType] = None,
                                   organisation: Option[OrganisationType] = None,
                                   address: Address,
                                   contactDetails: ContactCommDetailsType
                                 )

object RegisterWithIdResponse {
  implicit val format: OFormat[RegisterWithIdResponse] = Json.format[RegisterWithIdResponse]
}

case class OrganisationType(organisationName: String,
                            isAGroup: Option[Boolean] = None,
                            organisationType: Option[OrganisationTypeEnum.OrganisationType] = None)

object OrganisationType {
  implicit val formats: OFormat[OrganisationType] = Json.format[OrganisationType]
}

case class IndividualType(firstName: String,
                          middleName: Option[String] = None,
                          lastName: String,
                          dateOfBirth: Option[String] = None)

object IndividualType {
  implicit val formats: OFormat[IndividualType] = Json.format[IndividualType]
}

case class ContactCommDetailsType(primaryPhoneNumber: Option[String] = None,
                                  secondaryPhoneNumber: Option[String] = None,
                                  faxNumber: Option[String] = None,
                                  emailAddress: Option[String] = None)

object ContactCommDetailsType {
  implicit val formats: OFormat[ContactCommDetailsType] = Json.format[ContactCommDetailsType]
}


