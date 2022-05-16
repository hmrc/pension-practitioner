Pension Practitioner
==================================

Back-end microservice to support the registration, variation, invitation, association and de-registration of a pension practitioner.

API
---

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
| ```/can-deregister/:id                                     ```          | GET                 | Can de-register a Psp                                                                                    |
