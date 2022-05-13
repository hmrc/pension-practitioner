Pension Practitioner
==================================

// TODO: template needs filling in.

// Description of the microservice:
Back-end microservice to ...

// Table of endpoints
API
---

| *Task*                                                                  | *Supported Methods* | *Description*               |
|-------------------------------------------------------------------------|---------------------|-----------------------------|
| ```/register-with-id/individual                                  ```    | POST                | Description of what happens |
| ```/register-with-id/organisation                                   ``` | POST                | Description of what happens |
| ```/register-with-no-id/organisation                             ```    | POST                | Description of what happens |
| ```/register-with-no-id/individual                               ```    | POST                | Description of what happens |
| ```/journey-cache                                     ```               | GET                 | Description of what happens |
| ```/journey-cache                                     ```               | POST                | Description of what happens |
| ```/journey-cache                                     ```               | DELETE              | Description of what happens |
| ```/subscribePsp/:journeyType                                    ```    | POST                | Description of what happens |
| ```/getPsp                                                       ```    | GET                 | Description of what happens |
| ```/deregisterPsp/:pspId                                         ```    | POST                | Description of what happens |
| ```/authorise-psp                                                ```    | POST                | Description of what happens |
| ```/de-authorise-psp                                             ```    | POST                | Description of what happens |
| ```/email-response/:journeyType/:requestId/:email/:pspId        ```     | POST                | Description of what happens |
| ```/email-response-psp-dereg/:encryptedPspId/:encryptedEmail     ```    | POST                | Description of what happens |
| ```/get-minimal-details                                    ```          | GET                 | Description of what happens |
| ```/can-deregister/:id                                     ```          | GET                 | Description of what happens |


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
