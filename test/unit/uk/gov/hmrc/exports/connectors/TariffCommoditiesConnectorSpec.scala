/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.connectors

import org.mockito.ArgumentMatchers.any
import play.api.http.Status
import uk.gov.hmrc.exports.base.{MockMetrics, UnitSpec}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.TariffCommoditiesResponse
import uk.gov.hmrc.http.{HttpClient, InternalServerException, _}

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class TariffCommoditiesConnectorSpec extends UnitSpec with MockMetrics {

  class SetUpWithResponse(response: TariffCommoditiesResponse) {

    val appConfig: AppConfig = mock[AppConfig]
    val httpClient: HttpClient = mock[HttpClient]

    val testObj = new TariffCommoditiesConnector(appConfig, httpClient: HttpClient, exportsMetrics)

    val commodityCode = "1234567890"

    implicit val hc: HeaderCarrier = mock[HeaderCarrier]

    when {
      appConfig.tariffCommoditiesUri
    } thenReturn {
      "https://testUrl/"
    }

    when {
      httpClient.GET(any[URL], any[Seq[(String, String)]])(
        any[HttpReads[TariffCommoditiesResponse]],
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    } thenReturn {
      Future.successful(response)
    }

  }

  "TariffCommoditiesConnector" when {

    "getCommodity" should {
      "respond with json" in new SetUpWithResponse(TariffCommoditiesResponse(Status.OK, Some(TariffCommoditiesConnectorSpec.exampleSuccessResponse))) {

        whenReady(testObj.getCommodity(commodityCode)) { result =>
          result mustBe TariffCommoditiesConnectorSpec.exampleSuccessResponse
        }

      }

      "respond with error" when {
        "server responds with 4xx" in new SetUpWithResponse(TariffCommoditiesResponse(Status.NOT_FOUND, None)) {

          whenReady(testObj.getCommodity(commodityCode).failed) { result =>
            result mustBe an[InternalServerException]
          }

        }

        "server responds with 5xx" in new SetUpWithResponse(TariffCommoditiesResponse(Status.SERVICE_UNAVAILABLE, None)) {

          whenReady(testObj.getCommodity(commodityCode).failed) { result =>
            result mustBe an[InternalServerException]
          }

        }

        "server responds with something else" in new SetUpWithResponse(TariffCommoditiesResponse(Status.NO_CONTENT, None)) {

          whenReady(testObj.getCommodity(commodityCode).failed) { result =>
            result mustBe an[InternalServerException]
          }

        }
      }

    }

  }
}

object TariffCommoditiesConnectorSpec {
  val exampleSuccessResponse: String =
    """{
                                 |  “data”: {
                                 |    “id”: “27777”,
                                 |    “type”: “commodity”,
                                 |    “attributes”: {
                                 |      “producline_suffix”: “80”,
                                 |      “description”: “Lambs(uptoayearold)”,
                                 |      “number_indents”: 3,
                                 |      “goods_nomenclature_item_id”: “0104103000”,
                                 |      “bti_url”: “http: //ec.europa.eu/taxation_customs/dds2/ebti/bti_consultation.jsp?Lang=en&nomenc=0104103000&Expand=true”,
                                 |      “formatted_description”: “Lambs(uptoayearold)”,
                                 |      “description_plain”: “Lambs(uptoayearold)”,
                                 |      “consigned”: false,
                                 |      “consigned_from”: null,
                                 |      “basic_duty_rate”: “80.50EUR/100kg”,
                                 |      “meursing_code”: false,
                                 |      “declarable”: true
                                 |    },
                                 |    “relationships”: {
                                 |      “footnotes”: {
                                 |        “data”: [
                                 |          {
                                 |            “id”: “701”,
                                 |            “type”: “footnote”
                                 |          }
                                 |        ]
                                 |      },
                                 |      “section”: {
                                 |        “data”: {
                                 |          “id”: “1”,
                                 |          “type”: “section”
                                 |        }
                                 |      },
                                 |      “chapter”: {
                                 |        “data”: {
                                 |          “id”: “27623”,
                                 |          “type”: “chapter”
                                 |        }
                                 |      },
                                 |      “heading”: {
                                 |        “data”: {
                                 |          “id”: “27773”,
                                 |          “type”: “heading”
                                 |        }
                                 |      },
                                 |      “ancestors”: {
                                 |        “data”: [
                                 |          {
                                 |            “id”: “27774”,
                                 |            “type”: “commodity”
                                 |          },
                                 |          {
                                 |            “id”: “27776”,
                                 |            “type”: “commodity”
                                 |          }
                                 |        ]
                                 |      },
                                 |      “import_measures”: {
                                 |        “data”: [
                                 |          {
                                 |            “id”: “-480792”,
                                 |            “type”: “measure”
                                 |          },
                                 |          {
                                 |            “id”: “3619955”,
                                 |            “type”: “measure”
                                 |          },
                                 |          {
                                 |            “id”: “3563227”,
                                 |            “type”: “measure”
                                 |          },
                                 |          {
                                 |            “id”: “2046830”,
                                 |            “type”: “measure”
                                 |          }
                                 |        ]
                                 |      },
                                 |      “export_measures”: {
                                 |        “data”: [
                                 |          {
                                 |            “id”: “3540076”,
                                 |            “type”: “measure”
                                 |          },
                                 |          {
                                 |            “id”: “2982602”,
                                 |            “type”: “measure”
                                 |          }
                                 |        ]
                                 |      }
                                 |    }
                                 |  },
                                 |  “included”: [
                                 |    {
                                 |      “id”: “27774”,
                                 |      “type”: “commodity”,
                                 |      “attributes”: {
                                 |        “producline_suffix”: “80”,
                                 |        “description”: “Sheep”,
                                 |        “number_indents”: 1,
                                 |        “goods_nomenclature_item_id”: “0104100000”,
                                 |        “formatted_description”: “Sheep”,
                                 |        “description_plain”: “Sheep”
                                 |      }
                                 |    },
                                 |    {
                                 |      “id”: “701”,
                                 |      “type”: “footnote”,
                                 |      “attributes”: {
                                 |        “code”: “TR037”,
                                 |        “description”: “Guillotineblades”,
                                 |        “formatted_description”: “Guillotineblades”
                                 |      }
                                 |    },
                                 |    {
                                 |      “id”: “1”,
                                 |      “type”: “section”,
                                 |      “attributes”: {
                                 |        “numeral”: “I”,
                                 |        “title”: “Liveanimals;animalproducts”,
                                 |        “position”: “1”
                                 |      }
                                 |    },
                                 |    {
                                 |      “id”: “27773”,
                                 |      “type”: “heading”,
                                 |      “attributes”: {
                                 |        “goods_nomenclature_item_id”: “9950000000”,
                                 |        “description”: “Codeusedonlyintradingof…islessthan€|200”,
                                 |        “formatted_description”: “Codeusedonlyintradingof…islessthan€200”,
                                 |        “description_plain”: “Codeusedonlyintradingof…islessthan€200”
                                 |      }
                                 |    },
                                 |    {
                                 |      “id”: “27623”,
                                 |      “type”: “chapter”,
                                 |      “attributes”: {
                                 |        “goods_nomenclature_item_id”: “0100000000”,
                                 |        “description”: “LIVEANIMALS”,
                                 |        “formatted_description”: “Liveanimals”,
                                 |        “chapter_note”: “*1\.Thischaptercoversallliveanimalsexcept: \r\n*(a)fishandcrustaceans,
                                 |        molluscsandotheraquaticinvertebrates,
                                 |        ofheading0301,
                                 |        0306,
                                 |        0307or0308;\r\n*(b)culturesofmicro-organismsandotherproductsofheading3002;and\r\n*©animalsofheading9508.”
                                 |      },
                                 |      “relationships”: {
                                 |        “guides”: {
                                 |          “data”: [
                                 |            {
                                 |              “id”: “23”,
                                 |              “type”: “guide”
                                 |            }
                                 |          ]
                                 |        }
                                 |      }
                                 |    },
                                 |    {
                                 |      “id”: “3540076”,
                                 |      “type”: “measure”,
                                 |      “attributes”: {
                                 |        “id”: 3540076,
                                 |        “origin”: “eu”,
                                 |        “effective_start_date”: “2017-02-04T00: 00: 00.000Z”,
                                 |        “effective_end_date”: null,
                                 |        “import”: false,
                                 |        “excise”: false,
                                 |        “vat”: false,
                                 |        “reduction_indicator”: null
                                 |      },
                                 |      “relationships”: {
                                 |        “duty_expression”: {
                                 |          “data”: {
                                 |            “id”: “3540076-duty_expression”,
                                 |            “type”: “duty_expression”
                                 |          }
                                 |        },
                                 |        “measure_type”: {
                                 |          “data”: {
                                 |            “id”: “715”,
                                 |            “type”: “measure_type”
                                 |          }
                                 |        },
                                 |        “legal_acts”: {
                                 |          “data”: [
                                 |            {
                                 |              “id”: “R1701600”,
                                 |              “type”: “legal_act”
                                 |            },
                                 |            {
                                 |              “id”: “R9703380”,
                                 |              “type”: “legal_act”
                                 |            }
                                 |          ]
                                 |        },
                                 |        “measure_conditions”: {
                                 |          “data”: [
                                 |            {
                                 |              “id”: “1134192”,
                                 |              “type”: “measure_condition”
                                 |            },
                                 |            {
                                 |              “id”: “1134193”,
                                 |              “type”: “measure_condition”
                                 |            },
                                 |            {
                                 |              “id”: “1134194”,
                                 |              “type”: “measure_condition”
                                 |            }
                                 |          ]
                                 |        },
                                 |        “measure_components”: {
                                 |          “data”: [
                                 |            {
                                 |              “id”: “3540076-01”,
                                 |              “type”: “measure_component”
                                 |            },
                                 |            {
                                 |              “id”: “3540076-02”,
                                 |              “type”: “measure_component”
                                 |            }
                                 |          ]
                                 |        },
                                 |        “national_measurement_units”: {
                                 |          “data”: null
                                 |        },
                                 |        “geographical_area”: {
                                 |          “data”: {
                                 |            “id”: “1008”,
                                 |            “type”: “geographical_area”
                                 |          }
                                 |        },
                                 |        “excluded_countries”: {
                                 |          “data”: null
                                 |        },
                                 |        “footnotes”: {
                                 |          “data”: {
                                 |            “id”: “CD371”,
                                 |            “type”: “footnote”
                                 |          }
                                 |        },
                                 |        “order_number”: {
                                 |          “data”: null
                                 |        }
                                 |      }
                                 |    },
                                 |    {
                                 |      “id”: “1134192”,
                                 |      “type”: “measure_condition”,
                                 |      “attributes”: {
                                 |        “id”: “1134192”,
                                 |        “condition_code”: “Y”,
                                 |        “condition”: “Y: Otherconditions”,
                                 |        “document_code”: “N853”,
                                 |        “requirement”: “UN/EDIFACTcertificates: CommonVeterinaryEntryDocument(CVED)inaccordancewithRegulation(EC)No136/2004,
                                 |        usedforveterinarycheckonproducts”,
                                 |        “action”: “Import/exportallowedaftercontrol”,
                                 |        “duty_expression”: “”
                                 |      }
                                 |    },
                                 |    {
                                 |      “id”: “3540076-01”,
                                 |      “type”: “measure_component”,
                                 |      “attributes”: {
                                 |        “id”: “3540076-01”,
                                 |        “duty_expression_id”: “01”,
                                 |        “duty_amount”: 20.2,
                                 |        “monetary_unit_code”: “EUR”,
                                 |        “monetary_unit_abbreviation”: null,
                                 |        “measurement_unit_code”: “DTN”,
                                 |        “duty_expression_description”: “+%oramount”,
                                 |        “duty_expression_abbreviation”: “+”
                                 |      }
                                 |    },
                                 |    {
                                 |      “id”: “”,
                                 |      “type”: “duty_expression”,
                                 |      “attributes”: null
                                 |    }
                                 |  ]
                                 |}""".stripMargin
}
