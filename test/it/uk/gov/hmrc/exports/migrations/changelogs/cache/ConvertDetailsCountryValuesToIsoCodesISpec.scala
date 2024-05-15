package uk.gov.hmrc.exports.migrations.changelogs.cache

import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec

class ConvertDetailsCountryValuesToIsoCodesISpec extends IntegrationTestMigrationToolSpec {
  override val collectionUnderTest = "declarations"
  override val changeLog = new ConvertDetailsCountryValuesToIsoCodes()

  "ConvertDetailsCountryValuesToIsoCodes migration" should {

    import ConvertDetailsCountryValuesToIsoCodesISpec._

    "correctly migrate and convert country fields to ISO codes" in {
      runTest(declarationBeforeMigration, declarationAfterMigration)
    }

    "leave contents when already a country code" in {
      runTest(declarationBeforeMigrationWithSomeIsoCodes, declarationAfterMigration)
    }

    "log a warning about unmappable values (check manually)" in {
      runTest(declarationBeforeMigrationWithUnmappableValue, declarationAfterMigrationWithUnmappableValue)
    }
  }
}

object ConvertDetailsCountryValuesToIsoCodesISpec {
  val declarationBeforeMigration =
    """{
      |  "_id":{"$oid":"664489eed5a3863887235eef"},
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "eori": "GB7172755049071",
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "Swaziland"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Tajikistan"
      |              }
      |          }
      |      },
      |      "consignorDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Ukraine"
      |              }
      |          }
      |      },
      |      "exporterDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Vietnam"
      |              }
      |          }
      |      },
      |      "representativeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Zimbabwe"
      |              }
      |          }
      |      }
      |  }
      |}""".stripMargin

  val declarationBeforeMigrationWithSomeIsoCodes =
    """{
      |  "_id":{"$oid":"664489eed5a3863887235eef"},
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "eori": "GB7172755049071",
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "SZ"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "TJ"
      |              }
      |          }
      |      },
      |      "consignorDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Ukraine"
      |              }
      |          }
      |      },
      |      "exporterDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Vietnam"
      |              }
      |          }
      |      },
      |      "representativeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Zimbabwe"
      |              }
      |          }
      |      }
      |  }
      |}""".stripMargin

  val declarationBeforeMigrationWithUnmappableValue =
    """{
      |  "_id":{"$oid":"664489eed5a3863887235eef"},
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "eori": "GB7172755049071",
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "Fake country"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Tajikistan"
      |              }
      |          }
      |      },
      |      "consignorDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Ukraine"
      |              }
      |          }
      |      },
      |      "exporterDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Vietnam"
      |              }
      |          }
      |      },
      |      "representativeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "Zimbabwe"
      |              }
      |          }
      |      }
      |  }
      |}""".stripMargin

  val declarationAfterMigration =
    """{
      |  "_id":{"$oid":"664489eed5a3863887235eef"},
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "eori": "GB7172755049071",
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "SZ"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "TJ"
      |              }
      |          }
      |      },
      |      "consignorDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "UA"
      |              }
      |          }
      |      },
      |      "exporterDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "VN"
      |              }
      |          }
      |      },
      |      "representativeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "ZW"
      |              }
      |          }
      |      }
      |  }
      |}""".stripMargin

  val declarationAfterMigrationWithUnmappableValue =
    """{
      |  "_id":{"$oid":"664489eed5a3863887235eef"},
      |  "id": "c0305e9b-c48a-40cf-b74e-406c205f70db",
      |  "eori": "GB7172755049071",
      |  "parties": {
      |      "consigneeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "Bags Export",
      |                  "addressLine": "1 Bags Avenue",
      |                  "townOrCity": "New York",
      |                  "postCode": "10001",
      |                  "country": "Fake country"
      |              }
      |          }
      |      },
      |      "declarantDetails": {
      |          "details": {
      |              "eori": "GB7172755049071"
      |          }
      |      },
      |      "carrierDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "TJ"
      |              }
      |          }
      |      },
      |      "consignorDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "UA"
      |              }
      |          }
      |      },
      |      "exporterDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "VN"
      |              }
      |          }
      |      },
      |      "representativeDetails": {
      |          "details": {
      |              "address": {
      |                  "fullName": "XYZ Carrier",
      |                  "addressLine": "School Road",
      |                  "townOrCity": "London",
      |                  "postCode": "WS1 2AB",
      |                  "country": "ZW"
      |              }
      |          }
      |      }
      |  }
      |}""".stripMargin
}
