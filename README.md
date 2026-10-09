# customs-declare-exports

## About
This microservice is part of Customs Exports Declaration Service (CEDS). It is designed to work in tandem with [customs-declare-exports-frontend](https://github.com/hmrc/customs-declare-exports-frontend) service.

It provides functionality to manage declaration-related data before and after it has been submitted.

|Key |Value|
|---|---|
| Digital service | CDS Exports |
| Local port | `6792` |
| Base path | `/customs-declarations` |
| Front-end | [customs-declare-exports-frontend](https://github.com/hmrc/customs-declare-exports-frontend) (port `6791`) |
| Acceptance tests | [exports-ui-acceptance-tests](https://github.com/hmrc/exports-ui-acceptance-tests) |
| Performance tests | [exports-declarations-performance-tests](https://github.com/hmrc/exports-declarations-performance-tests) |
| Stubs used in Local and Staging |[customs-declarations-stub](https://github.com/hmrc/customs-declarations-stub) |

## How to Run This Service

### Prerequisites
Start all the services CDS Exports depends on with [Service Manager](#service-manager-profiles):

```bash
sm2 --start CDS_EXPORTS_DECLARATION_ALL
```

If you want to start all services EXCEPT both declarations services:

```bash
sm2 --start CDS_EXPORTS_DECLARATION_DEPS
```

### Running the Service Locally
To run this service from source (for example, to test your own changes), stop the instance started by Service Manager and run it with sbt:

```bash
sm2 --stop CUSTOMS_DECLARE_EXPORTS
sbt run
```

To test this service via our frontend service, refer to the [customs-declare-exports-frontend](https://github.com/hmrc/customs-declare-exports-frontend#running-the-service-locally) repository's README.

## How to Test This Service

### Local

#### Unit Tests
```bash
sbt test
```

#### Integration Tests
```bash
sbt it:test
```

#### Pre-Push Check
There is a script called `precheck.sh` that runs all tests, examines their coverage and checks if all the files are properly formatted.
It is good practice to run it just before pushing to GitHub.

```bash
./precheck.sh
```

#### Seed Mongo

To provide high number of declarations (20 000) in system run:
```bash
sbt test:run testdata.SeedMongo
```
Output of program looks
```
Inserted 523 - 523 for GB1814503088
Inserted 788 - 265 for GB966964885
Inserted 1023 - 235 for GB1795007712
Inserted 1043 - 20 for GB1822591600
Inserted 1059 - 16 for GB1713564034
Inserted 1471 - 412 for GB1026524884
Inserted 2094 - 623 for GB1585987871
```

#### Test Only Endpoints
To enable these endpoint you must specify at startup the test-only conf file:

```bash
sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes
```


##### /test-only/create-submitted-dec-record
This endpoint is designed to quickly insert all the required db documents into the collections to represent a new accepted declaration submission. It does not make any downstream requests or auth checks
and is designed specifically to be used by QAs to populate the db collections en-masse quickly.

It will randomly (50/50 chance) also insert an extra notification of type DMSDOC.

You can call the endpoint like this:

```bash
curl --location --request POST 'http://localhost:6792/test-only/create-submitted-dec-record' --header 'Content-Type: application/json' --data-raw '{"eori": "GB1234567890", "lrn" : "SOMELRN", "ducr" : "2GB123456789000-XXXABC456TIM"}'
```

You will receive back the following response (if successful):

```
"EORI:GB1234567890, LRN:7SLXFBE0CKH2WNRRRKR7NZ, MRN:36GBRJYXDBZL4F9YZ, CREATED WITH DMSDOC"`
```


##### /test-only/create-draft-dec-record
This endpoint is designed to quickly create a new fully populated draft declaration with X specified number of items (see LINK TO PAYLOAD`itemCount` field of payload). It is designed specifically to be used by QAs in performance tests
to populate the declarations collection with many item (50+ item) draft declarations ready to be submitted.

You can call the endpoint like this:

```bash
curl --location --request POST 'http://localhost:6792/test-only/create-draft-dec-record' --header 'Content-Type: application/json' --data-raw '{"eori": "GB7172755022922", "itemCount" : 3, "lrn" : "SOMELRN", "ducr" : "2GB123456789000-123ABC456TIM"}'
```

You will receive back the following response (if successful):

```
{"declarationId": "a1c6c136-6552-485a-81a1-dd2973d5843d"}
```


#### Acceptance Tests (smoke and regression)
The acceptance tests for this service are in exports-ui-acceptance-tests, see [README](https://github.com/hmrc/exports-ui-acceptance-tests).

#### Performance tests
The performance tests for this service are in exports-declarations-performance-tests, see [README](https://github.com/hmrc/exports-declarations-performance-tests).

### Manual Testing in QA and Staging
After the successful [deployment pipeline](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSExports/job/customs-declare-exports-pipeline/), manual testing is performed in QA and/or Staging environment, see [Manual Service Verification in QA and Staging](https://github.com/hmrc/exports-ui-acceptance-tests#manual-service-verification-in-qa-and-staging).

## Service Catalogue
- [customs-declare-exports in the MDTP Catalogue](https://catalogue.tax.service.gov.uk/repositories/customs-declare-exports)

## Jenkins Pipeline
- [customs-declare-exports](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSExports/job/customs-declare-exports/)
- [customs-declare-exports pipeline](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSExports/job/customs-declare-exports-pipeline/)

- The acceptance test Jenkins jobs (smoke and regression) are listed in the [exports-ui-acceptance-tests README](https://github.com/hmrc/exports-ui-acceptance-tests#how-to-run-tests).
- The performance test Jenkins jobs are listed in the [exports-declarations-performance-tests README](https://github.com/hmrc/exports-declarations-performance-tests/#jenkins-builds).

## Service Manager Profiles
These profiles are defined in [service-manager-config](https://github.com/hmrc/service-manager-config).

| Profile | Use | Services started |
|---|---|---|
| `CDS_EXPORTS_DECLARATION_ALL` | Running and developing this service locally | `CUSTOMS_DECLARE_EXPORTS`, `CUSTOMS_DECLARE_EXPORTS_FRONTEND`, `AUTH`, `AUTH_LOGIN_API`, `CENTRALISED_AUTHORISATION_SERVER`, `AUTH_LOGIN_STUB`, `CUSTOMS_DECLARATIONS_STUB`, `CUSTOMS_DECLARATIONS_INFORMATION`, `USER_DETAILS`, `IDENTITY_VERIFICATION`, `CONTACT_FRONTEND`, `BAS_GATEWAY`, `BAS_GATEWAY_FRONTEND` |
| `CDS_EXPORTS_DECLARATION_ATS` | Running the [acceptance tests](https://github.com/hmrc/exports-ui-acceptance-tests#how-to-run-tests) | `CUSTOMS_DECLARE_EXPORTS`, `CUSTOMS_DECLARE_EXPORTS_FRONTEND`, `AUTH`, `AUTH_LOGIN_API`, `AUTH_LOGIN_STUB`, `BAS_GATEWAY`, `BAS_GATEWAY_FRONTEND`, `CUSTOMS_DECLARATIONS_STUB`, `USER_DETAILS`, `IDENTITY_VERIFICATION` |
| `CDS_EXPORTS_ALL` | All CDS Exports services (declarations, movements and internal) | See [service-manager-config](https://github.com/hmrc/service-manager-config) |

| Service | Port | Repository |
|---|---|---|
| `CUSTOMS_DECLARE_EXPORTS` | 6792 | This service |
| `CUSTOMS_DECLARE_EXPORTS_FRONTEND` | 6791 | [customs-declare-exports-frontend](https://github.com/hmrc/customs-declare-exports-frontend)|
| `CUSTOMS_DECLARATIONS_STUB` | 6790 | [customs-declarations-stub](https://github.com/hmrc/customs-declarations-stub) |


## Endpoints

All paths are relative to the service root (`http://localhost:6792` when running locally) and need an [authenticated user with the HMRC-CUS-ORG enrolment](#enrolment-required).

| Method | Path | Purpose | Sample request | Response |
|---|---|---|---|---|
| POST | `/declarations` | Create a new declaration | `ExportsDeclaration` JSON body – see example below | `201` with the created declaration |
| PUT | `/declarations` | Update an existing declaration | `ExportsDeclaration` JSON body – see example below | `200` with the updated declaration<br>Not found: `404` |
| GET | `/declarations/:id` | Get a declaration by ID | `/declarations/6f31582e-bfd5-4b27-90be-2dca6e236b20` | `200` with declaration JSON<br>Not found: `404` |
| DELETE | `/declarations/:id` | Delete a declaration | `/declarations/6f31582e-bfd5-4b27-90be-2dca6e236b20` | `204`<br>Completed declaration: `400` |
| GET | `/draft-declarations` | Get a paginated list of draft declarations | `?page-index=1&page-size=50&sort-by=declarationMeta.updatedDateTime&sort-direction=desc` | `200` with paginated draft declarations |
| POST | `/amendments` | Submit an amendment or amendment cancellation | ```{"submissionId":"subId","declarationId":"decId","isCancellation":false,"fieldPointers":["pointer"]}``` | `200` with action ID<br>Invalid declaration status: `409`<br>Not found: `404` |
| POST | `/amendment-resubmission` | Resubmit a previously submitted amendment | `{"submissionId":"subId","declarationId":"decId","isCancellation":false,"fieldPointers":["pointer"]}` | `200` with action ID<br>Invalid declaration status: `400`<br>Not found: `404` |
| GET | `/draft-declarations-by-parent/:parentId` | Find a draft declaration created from a parent declaration | `/draft-declarations-by-parent/parent-declaration-id` | `200` with declaration JSON<br>Not found: `404` |
| GET | `/amendment-draft/:parentId/:enhancedStatus` | Find or create an amendment draft from a submitted declaration | `/amendment-draft/parent-declaration-id/ERRORS` | Existing draft: `200` with declaration ID<br>Created draft: `201` with declaration ID<br>Parent not found: `404` |
| GET | `/rejected-submission-draft/:parentId` | Find or create a draft from an initially rejected declaration | `/rejected-submission-draft/parent-declaration-id` | Existing draft: `200` with declaration ID<br>Created draft: `201` with declaration ID<br>Parent not found: `404` |
| POST | `/cancellation-request` | Request cancellation of a submitted declaration | `{"submissionId":"id","functionalReferenceId":"ref","mrn":"24GB123456789ABC12","statementDescription":"No longer required","changeReason":"1"}` | `200` with cancellation result |
| GET | `/lrn-already-used/:lrn` | Check whether an LRN has been used by a non-rejected submission within the last 48 hours | `/lrn-already-used/QSLRN7285100` | `200` with `true` or `false` |
| GET | `/paginated-submissions` | Get submissions for the dashboard, grouped by status | `?groups=submitted&page=1&limit=25`<br>`groups`: `submitted`, `action`, `rejected`, `cancelled` | `200` with a page of submissions<br>Missing `groups`: `400`<br>Page less than 1: `400` |
| GET | `/submission/action/:actionId` | Get a submission action by action ID | `/submission/action/action-id` | `200` with action JSON<br>Not found: `404` |
| GET | `/submission/by-action/:actionId` | Get the submission containing an action | `/submission/by-action/action-id` | `200` with submission JSON<br>Not found: `404` |
| GET | `/submission/:id` | Get a submission by ID | `/submission/submission-id` | `200` with submission JSON<br>Not found: `404` |
| POST | `/submission/:id` | Submit the declaration with the specified declaration ID to CDS | `/submission/6f31582e-bfd5-4b27-90be-2dca6e236b20`<br>No request body | `201` with submission JSON<br>Already submitted: `409`<br>Declaration not found: `404` |
| GET | `/submissionByLatestDecId/:id` | Find a submission by its latest declaration ID | `/submissionByLatestDecId/declaration-id` | `200` with submission JSON<br>Not found: `404` |
| GET | `/submission/notifications/:id` | Get all parsed notifications related to a submission | `/submission/notifications/submission-id` | `200` with an array of notifications<br>Submission not found: `404` |
| GET | `/latest-notification/:actionId` | Get the latest parsed notification for an action | `/latest-notification/action-id` | `200` with the latest notification |
| GET | `/ead/:mrn` | Get EAD/MRN status information | `/ead/18GB9JLC3CU1LFGVR2` | `200` with MRN status JSON<br>Not found: `404` |
| GET | `/eori-email` | Get the email address associated with the authenticated user's EORI | – | `200` with email and deliverability status<br>Not found: `404`<br>Downstream failure: `500` |
| POST | `/customs-declare-exports/notify` | Receive an XML notification from CDS | XML body with `Authorization` and `X-Conversation-ID` headers | `202`<br>Missing/invalid required headers: `400` |

### Example payloads

#### Create or Update a Declaration

`POST /declarations` and `PUT /declarations`.

Example payload:

```json
{
  "id": "6f31582e-bfd5-4b27-90be-2dca6e236b20",
  "declarationMeta": {
    "status": "DRAFT",
    "createdDateTime": "2019-12-10T15:52:32.681Z",
    "updatedDateTime": "2019-12-10T15:53:13.697Z",
    "summaryWasVisited": true,
    "readyForSubmission": true,
    "maxSequenceIds": {
      "dummy": -1
    }
  },
  "eori": "",
  "type": "STANDARD",
  "additionalDeclarationType": "D",
  "consignmentReferences": {
    "ducr": {
      "ducr": "8GB123451068100-101SHIP1"
    },
    "lrn": "QSLRN7285100"
  },
  "transport": {},
  "parties": {},
  "locations": {},
  "items": []
}
```


#### Submit an Amendment

`POST /amendments`

Example payload:

```json
{
  "submissionId": "submission-id",
  "declarationId": "declaration-id",
  "isCancellation": false,
  "fieldPointers": [
    "pointer"
  ]
}
```

A successful amendment:

```json
"actionId"
```

#### Request a Cancellation

`POST /cancellation-request`

Example payload:

```json
{
  "submissionId": "submission-id",
  "functionalReferenceId": "functional-reference-id",
  "mrn": "24GB123456789ABC12",
  "statementDescription": "Goods are no longer being exported",
  "changeReason": "1"
}
```

Successful request:

```json
{
  "status": "CancellationRequestSent",
  "conversationId": "conversation-id"
}
```

Cancellation already requested:

```json
{
  "status": "CancellationAlreadyRequested"
}
```

Submission/MRN not found:

```json
{
  "status": "NotFound"
}
```

#### Paginated Draft Declarations

`GET /draft-declarations?page-index=1&page-size=50&sort-by=declarationMeta.updatedDateTime&sort-direction=desc`

Example response:

```json
{
  "currentPageElements": [
    {
      "id": "6f31582e-bfd5-4b27-90be-2dca6e236b20",
      "ducr": "8GB123451068100-101SHIP1",
      "status": "DRAFT",
      "updatedDateTime": "2026-10-07T10:30:00Z"
    }
  ],
  "page": {
    "index": 1,
    "size": 50
  },
  "total": 1
}
```

#### Paginated Submissions

`GET /paginated-submissions?groups=action,cancelled&limit=25`

A specific page can be requested:

`GET /paginated-submissions?groups=rejected&page=2&limit=25`

Example response:

```json
{
  "statusGroup": "submitted",
  "totalSubmissionsInGroup": 1,
  "submissions": [
    {
      "uuid": "submission-id",
      "eori": "GB123456789000",
      "lrn": "QSLRN7285100",
      "mrn": "24GB123456789ABC12",
      "ducr": "8GB123451068100-101SHIP1",
      "latestEnhancedStatus": "RECEIVED",
      "actions": [
        {
          "id": "action-id",
          "requestType": "SubmissionRequest",
          "decId": "declaration-id",
          "versionNo": 1
        }
      ],
      "latestDecId": "declaration-id",
      "latestVersionNo": 1
    }
  ],
  "reverse": false
}
```

#### Submission Notifications

`GET /submission/notifications/submission-id`

Example response:

```json
[
  {
    "actionId": "action-id",
    "mrn": "24GB123456789ABC12",
    "dateTimeIssued": "2026-10-07T10:30:00Z",
    "status": "RECEIVED",
    "errors": []
  }
]
```

#### EAD / MRN Status

`GET /ead/18GB9JLC3CU1LFGVR2`

Example response:

```json
{
  "mrn": "18GB9JLC3CU1LFGVR2",
  "versionId": "1",
  "eori": "GB123456789012000",
  "declarationType": "IMZ",
  "ucr": "20GBAKZ81EQJ2WXYZ",
  "receivedDateTime": "2019-07-02T11:07:00Z",
  "releasedDateTime": "2019-07-02T11:07:00Z",
  "acceptanceDateTime": "2019-07-02T11:07:00Z",
  "createdDateTime": "2020-03-10T01:13:00Z",
  "roe": "6",
  "ics": "15",
  "irc": "000",
  "totalPackageQuantity": "10",
  "goodsItemQuantity": "100",
  "previousDocuments": [
    {
      "id": "18GBAKZ81EQJ2FGVR",
      "type": "DCR"
    }
  ]
}
```

#### EORI Email

Example response:

```json
{
  "address": "some@email.com",
  "deliverable": true
}
```

#### CDS Notification Callback

`POST /customs-declare-exports/notify` 

```text
Authorization: Bearer <token>
X-Conversation-ID: b1c09f1b-7c94-4e90-b754-7c5c71c44e11
Content-Type: application/xml
```

The body is a CDS notification XML document.

A successfully accepted notification returns:

```text
202 Accepted
```

## Developer Notes

### Feature Flags
This service uses feature flags to enable or disable some of its features. You can change or override them in config under the `microservice.services.features.<featureName>` key.

The list of feature flags and what they are responsible for:

`exportsMigration=[enabled/disabled]` - If enabled, the service uses Exports Migration Tool for data migrations. Otherwise, it uses Mongock.

#### To Set a Feature Flag Via System Properties:

`sbt "run -Dmicroservice.features.exportsMigration=enabled"`

### Scalafmt
The code is formatted with [sbt-scalafmt](https://scalameta.org/scalafmt/docs/installation.html#sbt), using the rules in `.scalafmt.conf`.

Check that all project files are formatted as expected:

```bash
sbt scalafmtCheckAll scalafmtSbtCheck
```

Format `*.sbt` and `project/*.scala` files:

```bash
sbt scalafmtSbt
```

Format all project files:

```bash
sbt scalafmtAll
```

## License
This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).