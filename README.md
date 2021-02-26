# customs-declare-exports

[![Build Status](https://travis-ci.org/hmrc/customs-declare-exports.svg)](https://travis-ci.org/hmrc/customs-declare-exports) [ ![Download](https://api.bintray.com/packages/hmrc/releases/customs-declare-exports/images/download.svg) ](https://bintray.com/hmrc/releases/customs-declare-exports/_latestVersion)

This is a placeholder README.md for a new repository

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

### Required services

In order to set up all services required for customs-declare-exports to work, there are profiles in service-manager-config repository:
- CDS_EXPORTS_DECLARATION_DEPS - all services EXCEPT both declarations services
- CDS_EXPORTS_DECLARATION_ALL - all services together with both declarations services

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