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

### Testing the application
This repository contains unit and integration tests for the service. In order to run them, simply execute:

`sbt test` - for unit tests

`sbt it:test` - for integration tests

## Developer notes

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

### Feature flags
To set a feature flag via system properties

`sbt "run -Dmicroservice.features.exportsMigration=enabled"`

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
