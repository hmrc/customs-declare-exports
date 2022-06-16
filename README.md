# customs-declare-exports

## Summary
This microservice is part of Customs Exports Declaration Service (CEDS). It is designed to work in tandem with [customs-declare-exports-frontend](https://github.com/hmrc/customs-declare-exports-frontend) service.

It provides functionality to manage declaration-related data before and after it has been submitted.


## Prerequisites
This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at a [JRE](https://www.java.com/en/download/) to run and a JDK for development.

This service does **not** use MongoDB.

This service depends on other services. The easiest way to set up required microservices is to use Service Manager and profiles from [service-manager-config](https://github.com/hmrc/service-manager-config/) repository:
- CDS_EXPORTS_DECLARATION_DEPS - all services EXCEPT both declarations services
- CDS_EXPORTS_DECLARATION_ALL - all services together with both declarations services

### Running the application
In order to run the application you need to have SBT installed. Then, it is enough to start the service with:

`sbt run`

However, note that the first time before starting the service or running the tests, you need to execute the following commands, as the service involves operations that use MongoDB transactions, which require a replica-set MongoDB instance to work successfully.
Once the replica-set is installed locally, you can then start and stop it with the usual `docker start mongo44-replica` and `docker stop mongo44-replica` commands.  

`cd customs-declare-exports`

`docker build ./ -t mongodb:4.4.14-replica`

`docker run --name mongo44-replica -p 27017:27017 -d mongodb:4.4.14-replica`     # Note that this command already starts the replica-set. You do not need to also run `docker start ...`. 

### Testing the application
This repository contains unit and integration tests for the service. In order to run them, simply execute:

`sbt test` - for unit tests

`sbt it:test` - for integration tests

## Test Only endpoints
To enable these endpoint you must specify at startup the test-only conf file like this:

`sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes`

### /test-only/create-submitted-dec-record
This endpoint is designed to quickly insert all the required db documents into the collections to represent a new accepted declaration submission. It does not make any downstream requests or auth checks
and is designed specifically to be used by QAs to populated the db collections en masse quickly.

It will randomly (50/50 chance) also insert an extra notification of type DMSDOC.

You can call the endpoint like this:

`curl --location --request POST 'http://localhost:6792/test-only/create-submitted-dec-record' --header 'Content-Type: application/json' --data-raw '{"eori": "GB1234567890"}'`

You will receive back the following response (if successful):

`"EORI:GB1234567890, LRN:7SLXFBE0CKH2WNRRRKR7NZ, MRN:36GBRJYXDBZL4F9YZ, CREATED WITH DMSDOC"`

### /test-only/create-draft-dec-record
This endpoint is designed to quickly create a new fully populated draft declaration with X specified number of items (see `itemCount` field of payload). It is designed specifically to be used by QAs in performance tests 
to populate the declarations collection with many item (50+ item) draft declarations ready to be submitted.

You can call the endpoint like this:

`curl --location --request POST 'http://localhost:6792/test-only/create-draft-dec-record' --header 'Content-Type: application/json' --data-raw '{"eori": "GB7172755022922", "itemCount" : 3, "lrn" : "SOMELRN""}'`

You will receive back the following response (if successful):

`{"declarationId": "a1c6c136-6552-485a-81a1-dd2973d5843d"}`

## Developer notes

### Feature flags
This service uses feature flags to enable/disable some of its features. These can be changed/overridden in config under `microservice.features.<featureName>` key.

The list of feature flags and what they are responsible for:

`exportsMigration=[enabled/disabled]` - If enabled, the service uses Exports Migration Tool for data migrations. Otherwise, it uses Mongock.

#### To set a feature flag via system properties:

`sbt "run -Dmicroservice.features.exportsMigration=enabled"`

### Scalastyle

Project contains scalafmt plugin.

Commands for code formatting:

```
scalafmt        # format compile sources
test:scalafmt   # format test sources
sbt:scalafmt    # format .sbt source
```

To ensure everything is formatted you can check project using commands below

```
scalafmt::test      # check compile sources
test:scalafmt::test # check test sources
sbt:scalafmt::test  # check .sbt sources
```

### Pre-merge check
There is a script called `precheck.sh` that runs all tests, examine their coverage and check if all the files are properly formatted.
It is a good practise to run it just before pushing to GitHub.

### Seed mongo

To provide high number of declarations (20 000) in system run
```
sbt test:run util.SeedMongo
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


## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
