# Pension Practitioner

- [Overview](#overview)
- [Requirements](#requirements)
- [Running the Service](#running-the-service)
- [Enrolments](#enrolments)
- [Compile & Test](#compile--test)
- [Navigation and Dependent Services](#navigation-and-dependent-services)
- [Service Documentation](#service-documentation)
- [Endpoints](#endpoints)
- [License](#license)

## Overview

This is the backend repository for the Pension Practitioner service. It allows a user to register and perform duties as a pension practitioner. The pension practitioner is a person or organisation that the pension scheme administrator authorises to manage a pension scheme on their behalf.

This service has a corresponding front-end microservice, namely pension-practitioner-frontend.

**Associated Frontend Link:** https://github.com/hmrc/pension-practitioner-frontend

**Stubs:** https://github.com/hmrc/pensions-scheme-stubs



## Requirements
This service is written in Scala and Play, so needs at least a [JRE] to run.

**Node version:** 16.20.2

**Java version:** 21

**Scala version:** 2.13.14


## Running the Service
**Service Manager Profile:** PODS_ALL

**Port:** 8209

In order to run the service, ensure Service Manager is installed (see [MDTP guidance](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html) if needed) and launch the relevant configuration by typing into the terminal:
`sm2 --start PODS_ALL`

To run the service locally, enter `sm2 --stop PENSION_PRACTITIONER`.

In your terminal, navigate to the relevant directory and enter `sbt run`.

Access the Authority Wizard and login with the relevant enrolment details [here](http://localhost:9949/auth-login-stub/gg-sign-in)


## Enrolments
There are several different options for enrolling through the auth login stub. In order to enrol as a dummy user to access the platform for local development and testing purposes, the following details must be entered on the auth login page.


In order to access the **Pension Practitioner dashboard** for local development, enter the following information: 

**Redirect URL -** http://localhost:8204/manage-pension-schemes/dashboard 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODSPP-ORG 

**Identifier Name -** PspID 

**Identifier Value -** 21000005

---

For access to the **Pension Administrator dashboard** for local development, enter the following information: 

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODS-ORG 

**Identifier Name -** PsaID 

**Identifier Value -** A2100005

---

**Dual enrolment** as both a Pension Administrator and Practitioner is also possible and can be accessed by entering:

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key 1 -** HMRC-PODSPP-ORG Identifier 

**Name 1 -** PspID Identifier 

**Value 1 -** 21000005

**Enrolment Key 2 -** HMRC-PODS-ORG 

**Identifier Name 2 -** PsaID 

**Identifier Value 2 -** A2100005

---

To access the **Scheme Registration journey**, enter the following information:

**Redirect URL -** http://localhost:8204/manage-pension-schemes/you-need-to-register 

**GNAP Token -** NO 

**Affinity Group -** Organisation

---


## Compile & Test
**To compile:** Run `sbt compile`

**To test:** Use `sbt test`

**To view test results with coverage:** Run `sbt clean coverage test coverageReport`

For further information on the PODS Test Approach and wider testing including acceptance, accessibility, performance, security and E2E testing, visit the PODS Confluence page [here](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=PODSP&title=PODS+Test+Approach).

For Journey Tests, visit the [Journey Test Repository](| Journey tests(https://github.com/hmrc/pods-journey-tests).

View the prototype [here](https://pods-event-reporting-prototype.herokuapp.com/).


## Navigation and Dependent Services
The Pension Practitioner Frontend integrates with the Manage Pension Schemes (MPS) service and uses various stubs available on [GitHub](https://github.com/hmrc/pensions-scheme-stubs). From the Authority Wizard page you will be redirected to the dashboard. Navigate to the appropriate area by accessing items listed within the service-specific tiles on the dashboard. On the Pension Practitioner frontend, a practitioner can change their details, stop being a practitioner, or search for and view a pension scheme.


There are numerous APIs implemented throughout the MPS architecture, and the relevant endpoints are illustrated below. For an overview of all PODS APIs, refer to the [PODS API Documentation](https://confluence.tools.tax.service.gov.uk/display/PODSP/PODS+API+Latest+Version).


## Note on terminology
The terms scheme reference number and submission reference number (SRN) are interchangeable within the PODS codebase; some downstream APIs use scheme reference number, some use submission reference number, probably because of oversight on part of the technical teams who developed these APIs. This detail means the same thing, the reference number that was returned from ETMP when the scheme details were submitted.

## Endpoints

| *Task*                                                                  | *Supported Methods* | *Description*                                                                                            |
|-------------------------------------------------------------------------|---------------------|----------------------------------------------------------------------------------------------------------|
| ```/register-with-id/individual                                  ```    | POST                | Registers an individual based on the NINO from ETMP                                                      |
| ```/register-with-id/organisation                                   ``` | POST                | Registers an organisation from ETMP based on the UTR                                                     |
| ```/register-with-no-id/organisation                             ```    | POST                | Registers an organisation on ETMP who does not have a UTR. Typically this will be a non-UK organisation  |
| ```/register-with-no-id/individual                               ```    | POST                | Registers an individual on ETMP who does not have a UTR/NINO. Typically this will be a non-UK individual |
| ```/journey-cache                                     ```               | GET                 | Returns the data from Psp Data Cache                                                                     |
| ```/journey-cache                                     ```               | POST                | Saves the data to Psp Data Cache                                                                         |
| ```/journey-cache                                     ```               | DELETE              | Delete the data from Psp Data Cache                                                                      |
| ```/subscribePsp/:journeyType                                    ```    | POST                | Subscribe a pension scheme practitioner                                                                  |
| ```/getPsp                                                       ```    | GET                 | Get Psp subscription details                                                                             |
| ```/deregisterPsp/:pspId                                         ```    | POST                | De-register a Psp                                                                                        |
| ```/authorise-psp                                                ```    | POST                | Authorise a Psp                                                                                          |
| ```/de-authorise-psp                                             ```    | POST                | De-authorise a Psp                                                                                       |
| ```/email-response/:journeyType/:requestId/:email/:pspId        ```     | POST                | Sends an audit event with the correct response returned from an email service                            |
| ```/email-response-psp-dereg/:encryptedPspId/:encryptedEmail     ```    | POST                | Sends an Psp de-registration email with the correct response returned from an email service              |
| ```/get-minimal-details                                    ```          | GET                 | Get minimal Psp details                                                                                  |
| ```/can-deregister/:id                                     ```          | GET                 | Can de-register a Psp                 

---

## License
This code is open source software Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[↥ Back to Top](#pension-practitioner)
