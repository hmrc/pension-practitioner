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

**Java version:** 11

**Scala version:** 2.13.14


## Running the Service
**Service Manager Profile:** PODS_ALL

**Port:** 8209

**Link:** http://localhost:8208/pension-scheme-practitioner/practitioner-details


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


## Service Documentation
[To Do]
Include relevant links or details to any additional, service-specific documents (e.g., stubs, testing protocols) when available. 

## Endpoints
[To Do]

**Standard Path**
```POST   /register-with-id/individual```

**Description**
Registers an individual based on the NINO from ETMP

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /register-with-id/organisation```

**Description**
Registers an organisation from ETMP based on the UTR

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /register-with-no-id/organisation```

**Description**
Registers an organisation on ETMP who does not have a UTR. Typically this will be a non-UK organisation

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |
|     | POST                |   |

---

**Standard Path**
```POST   /register-with-no-id/individual```

**Description**
Registers an individual on ETMP who does not have a UTR/NINO. Typically this will be a non-UK individual

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```GET   /journey-cache```

**Description**
Returns the data from Psp Data Cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /journey-cache```

**Description**
Saves the data to Psp Data Cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```DELETE   /journey-cache```

**Description**
Delete the data from Psp Data Cache

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /subscribePsp/:journeyType```

**Description**
Subscribe a pension scheme practitioner

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```GET   /getPsp```

**Description**
Get Psp subscription details

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /deregisterPsp/:pspId``` 

**Description**
De-register a Psp

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /authorise-psp```

**Description**
Authorise a Psp

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /de-authorise-psp```

**Description**
De-authorise a Psp

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /email-response/:journeyType/:requestId/:email/:pspId```

**Description**
Sends an audit event with the correct response returned from an email service

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```POST   /email-response-psp-dereg/:encryptedPspId/:encryptedEmail```

**Description**
Sends an Psp de-registration email with the correct response returned from an email service

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```GET   /get-minimal-details```

**Description**
Get minimal Psp details

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

**Standard Path**
```GET   /can-deregister/:id```

**Description**
Can de-register a Psp

| *Args*                        | *Expected Requests*                      | *Samples Response*                           | *Error Codes/Responses*                   |
|-------------------------------|------------------------------------------|----------------------------------------------|-------------------------------------------|
| ```INSERT ARGS```             | INSERT REQUEST HERE                      | INSERT RESPONSE HERE                         | INSERT ERROR CODES AND RESPONSES          |

---

## License
This code is open source software Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[↥ Back to Top](#pension-practitioner)
