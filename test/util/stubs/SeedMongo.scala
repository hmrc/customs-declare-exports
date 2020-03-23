/*
 * Copyright 2020 HM Revenue & Customs
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

package stubs

import java.util.UUID

import reactivemongo.api.{MongoConnection, MongoDriver}
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.models.declaration._
import unit.uk.gov.hmrc.exports.services.mapping.ExportsItemBuilder

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object SeedMongo extends App with ExportsDeclarationBuilder with ExportsItemBuilder {

  import ExportsDeclaration.Mongo._

  import scala.concurrent.ExecutionContext.Implicits._

  val declaration = aDeclaration(
    withDispatchLocation(),
    withAdditionalDeclarationType(),
    withConsignmentReferences(),
    withDepartureTransport(ModeOfTransportCode.Maritime, "11", "SHIP1"),
    withBorderTransport(),
    withContainerData(),
    withExporterDetails(Some("GB717572504502801")),
    withConsigneeDetails(None, Some(Address("Bags Export", "1 Bags Avenue", "New York", "NA", "United States of America"))),
    withDeclarantDetails(Some("GB717572504502811")),
    withRepresentativeDetails(Some("GB717572504502809"), None, Some("3")),
    withDeclarationHolders(DeclarationHolder(Some("AEOC"), Some("GB717572504502811"))),
    withCarrierDetails(None, Some(Address("XYZ Carrier", "School Road", "London", "WS1 2AB", "United Kingdom"))),
    withOriginationCountry(),
    withDestinationCountry(),
    withoutRoutingCountries(),
    withGoodsLocation(
      GoodsLocation(country = "GB", typeOfLocation = "B", qualifierOfIdentification = "Y", identificationOfLocation = Some("FXTFXTFXT"))
    ),
    withWarehouseIdentification("RGBLBA001"),
    withSupervisingCustomsOffice("Belfast"),
    withInlandModeOfTransport(ModeOfTransportCode.Maritime),
    withOfficeOfExit(Some("GB000054"), Some("GBLBA003"), Some("No")),
    withItems(
      anItem(
        withProcedureCodes(Some("1040"), Seq("000")),
        withStatisticalValue(statisticalValue = "1000"),
        withCommodityDetails(CommodityDetails(combinedNomenclatureCode = Some("46021910"), descriptionOfGoods = Some("Straw for bottles"))),
        withPackageInformation(Some("PK"), Some(10), Some("RICH123")),
        withCommodityMeasure(CommodityMeasure(Some("10"), Some("500"), Some("700"))),
        withAdditionalInformation("00400", "EXPORTER"),
        withDocumentsProduced(DocumentProduced(Some("C501"), Some("GBAEOC71757250450281"), None, None, None, None, None))
      )
    ),
    withTotalNumberOfItems(Some("56764"), Some("1.49"), "1"),
    withPreviousDocuments(PreviousDocument("Y", "IF3", "101SHIP2", None)),
    withNatureOfTransaction("1")
  )

  val random = new scala.util.Random()

  val target = 20000

  def generateEori =
    "GB" + random.nextInt(Int.MaxValue).toString

  import reactivemongo.play.json.collection.JSONCollection

  def randomStatus =
    if (random.nextDouble() > 0.1) {
      DeclarationStatus.COMPLETE
    } else {
      DeclarationStatus.DRAFT
    }

  def job(connection: MongoConnection) =
    connection
      .database("customs-declare-exports")
      .map(db => db.collection[JSONCollection]("declarations"))
      .map { collection =>
        var now = 0
        while (now < target) {
          val count = random.nextInt(1000)
          val eori = generateEori
          val declarations = Range(0, count).map { _ =>
            declaration.copy(id = UUID.randomUUID.toString, eori = eori, status = randomStatus)
          }
          Await.ready(collection.insert.many(declarations), Duration.Inf)
          now += count
          println(s"Inserted $now - $count for $eori")
        }
      }

  override def main(args: Array[String]): Unit = {
    val driver = MongoDriver()
    val parsedUri = MongoConnection.parseURI("mongodb://localhost:27017/customs-declare-exports").get
    val connection = driver.connection(parsedUri, true).get

    Await.ready(job(connection), Duration.Inf)
    driver.close()
  }
}
