/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mongodb.scala.model.InsertOneModel
import org.mongodb.scala.{MongoClient, MongoCollection}
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.{COMPLETE, DRAFT}
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, ExportsItemBuilder}

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object SeedMongo extends ExportsDeclarationBuilder with ExportsItemBuilder {

  val declaration = aDeclaration(
    withAdditionalDeclarationType(),
    withConsignmentReferences(),
    withDepartureTransport(ModeOfTransportCode.Maritime, "11", "SHIP1"),
    withBorderTransport(),
    withTransportCountry(None),
    withContainerData(),
    withExporterDetails(Some("GB717572504502801")),
    withConsigneeDetails(None, Some(Address("Bags Export", "1 Bags Avenue", "New York", "NA", "United States of America"))),
    withDeclarantDetails(Some("GB717572504502811")),
    withRepresentativeDetails(Some(EntityDetails(Some("GB717572504502809"), None)), Some("3")),
    withDeclarationHolders(DeclarationHolder(Some("AEOC"), Some("GB717572504502811"), Some(EoriSource.OtherEori))),
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
    withOfficeOfExit(Some("GB000054")),
    withItems(
      anItem(
        withProcedureCodes(Some("1040"), Seq("000")),
        withStatisticalValue(statisticalValue = "1000"),
        withCommodityDetails(CommodityDetails(combinedNomenclatureCode = Some("4602191000"), descriptionOfGoods = Some("Straw for bottles"))),
        withPackageInformation(Some("PK"), Some(10), Some("RICH123")),
        withCommodityMeasure(CommodityMeasure(Some("10"), Some(false), Some("500"), Some("700"))),
        withAdditionalInformation("00400", "EXPORTER"),
        withAdditionalDocuments(Some(YesNoAnswer.yes), AdditionalDocument(Some("C501"), Some("GBAEOC71757250450281"), None, None, None, None, None))
      )
    ),
    withTotalNumberOfItems(Some("56764"), Some("GBP"), Some("yes"), Some("1.49"), "1"),
    withPreviousDocuments(PreviousDocument("IF3", "101SHIP2", None)),
    withNatureOfTransaction("1")
  )

  val random = new scala.util.Random

  val target = 20000
  val batchSize = 1000

  def generateEori: String = "GB" + random.nextInt(Int.MaxValue).toString

  def randomStatus: DeclarationStatus.Value = if (random.nextDouble() > 0.1) COMPLETE else DRAFT

  def job(collection: MongoCollection[ExportsDeclaration]): Unit =
    (1 to Math.ceil(target / batchSize).toInt).foreach { batchNumber =>
      val declarations = (1 to batchSize).map { _ =>
        InsertOneModel(declaration.copy(id = UUID.randomUUID.toString, eori = generateEori, status = randomStatus))
      }.toList
      Await.ready(collection.bulkWrite(declarations).toFuture(), Duration.Inf)
      print(s"Inserted ${batchSize * batchNumber} out of ${target} declarations\n")
    }

  def main(args: Array[String]): Unit = {
    val mongoClient = MongoClient()
    val collection = mongoClient
      .getDatabase("customs-declare-exports")
      .getCollection[ExportsDeclaration]("declarations")

    job(collection)
    mongoClient.close
  }
}
