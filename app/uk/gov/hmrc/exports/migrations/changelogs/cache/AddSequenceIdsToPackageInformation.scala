/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.exports.migrations.changelogs.cache

import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.mongodb.scala.model.Filters._
import play.api.Logging
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.migrations.TimeUtils.jsonWriter
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationMeta.{sequenceIdPlaceholder, PackageInformationKey}
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.Mongo
import uk.gov.hmrc.exports.models.declaration._

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class AddSequenceIdsToPackageInformation extends MigrationDefinition with Logging {

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = s"CEDS-4445 Add a 'sequenceId' field to 'PackageInformation' sub-documents", order = 20, author = "Lucio Biondi")

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val collection = db.getCollection("declarations")

    val batchSize = 100

    val filter =
      elemMatch("items", elemMatch("packageInformation", not(exists("sequenceId"))))

    val result = collection
      .find(filter)
      .batchSize(batchSize)
      .asScala
      .foldLeft(0) { (counter, document) =>
        Try(Json.parse(document.toJson(jsonWriter)).as[ExportsDeclaration](AddSequenceIdsToPackageInformation.reads)) match {
          case Failure(exc) =>
            val id = document.get("_id")
            val error = exc.getMessage
            logger.error(s"Error parsing document with _id($id): $error")
            counter + 1

          case Success(declaration) =>
            val (items, maxSequenceId) = updateSequenceIdsOfPackageInfos(declaration.items)

            val meta = declaration.declarationMeta
            val declarationWithSequenceIds = declaration.copy(
              declarationMeta = meta.copy(maxSequenceIds = meta.maxSequenceIds + (PackageInformationKey -> maxSequenceId)),
              items = items
            )

            val filter = and(equal("eori", declaration.eori), equal("id", declaration.id))
            collection.replaceOne(filter, Document.parse(Json.toJson(declarationWithSequenceIds)(Mongo.format).toString))
            counter
        }
      }

    if (result > 1) logger.error(s"$result documents could not be migrated due to parsing errors.")
    else if (result == 1) logger.error(s"$result document could not be migrated due to parsing errors.")
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")

  private def updateSequenceIdsOfPackageInfos(items: Seq[ExportItem]): (Seq[ExportItem], Int) =
    items.foldLeft((List.empty[ExportItem], 0)) { case ((itemAccum, sequenceIdAccum), item) =>
      val (maybeListOfPackageInformation, lastSequenceId) = item.packageInformation.map { listOfPackageInformation =>
        val updListOfPackageInformation = listOfPackageInformation.zipWithIndex.map { case (packageInformation, index) =>
          packageInformation.copy(sequenceIdAccum + index + 1)
        }
        (Some(updListOfPackageInformation), sequenceIdAccum + updListOfPackageInformation.size)
      }.getOrElse((None, sequenceIdAccum))

      (itemAccum :+ item.copy(packageInformation = maybeListOfPackageInformation), lastSequenceId)
    }
}

object AddSequenceIdsToPackageInformation {

  case class FormerPackageInformation(id: String, typesOfPackages: Option[String], numberOfPackages: Option[Int], shippingMarks: Option[String])
  implicit val packageInformationReads: Reads[FormerPackageInformation] = Json.reads[FormerPackageInformation]

  implicit val readsForExportItem: Reads[ExportItem] =
    ((__ \ "id").read[String] and
      (__ \ "sequenceId").read[Int] and
      (__ \ "procedureCodes").readNullable[ProcedureCodes] and
      (__ \ "fiscalInformation").readNullable[FiscalInformation] and
      (__ \ "additionalFiscalReferencesData").readNullable[AdditionalFiscalReferences] and
      (__ \ "statisticalValue").readNullable[StatisticalValue] and
      (__ \ "commodityDetails").readNullable[CommodityDetails] and
      (__ \ "dangerousGoodsCode").readNullable[UNDangerousGoodsCode] and
      (__ \ "cusCode").readNullable[CUSCode] and
      (__ \ "taricCodes").readNullable[List[TaricCode]] and
      (__ \ "nactCodes").readNullable[List[NactCode]] and
      (__ \ "nactExemptionCode").readNullable[NactCode] and
      (__ \ "packageInformation").readNullable[List[FormerPackageInformation]] and
      (__ \ "commodityMeasure").readNullable[CommodityMeasure] and
      (__ \ "additionalInformation").readNullable[AdditionalInformations] and
      (__ \ "additionalDocuments").readNullable[AdditionalDocuments] and
      (__ \ "isLicenceRequired").readNullable[Boolean]) {

      (
        id,
        sequenceId,
        procedureCodes,
        fiscalInformation,
        additionalFiscalReferencesData,
        statisticalValue,
        commodityDetails,
        dangerousGoodsCode,
        cusCode,
        taricCodes,
        nactCodes,
        nactExemptionCode,
        maybePackageInformation,
        commodityMeasure,
        additionalInformation,
        additionalDocuments,
        isLicenceRequired
      ) =>
        val packageInformation = maybePackageInformation.map(
          _.map(pkg => PackageInformation(sequenceIdPlaceholder, pkg.id, pkg.typesOfPackages, pkg.numberOfPackages, pkg.shippingMarks))
        )

        ExportItem(
          id,
          sequenceId,
          procedureCodes,
          fiscalInformation,
          additionalFiscalReferencesData,
          statisticalValue,
          commodityDetails,
          dangerousGoodsCode,
          cusCode,
          taricCodes,
          nactCodes,
          nactExemptionCode,
          packageInformation,
          commodityMeasure,
          additionalInformation,
          additionalDocuments,
          isLicenceRequired
        )
    }

  implicit val reads: Reads[ExportsDeclaration] =
    ((__ \ "id").read[String] and
      (__ \ "declarationMeta").read[DeclarationMeta] and
      (__ \ "eori").read[String] and
      (__ \ "type").read[DeclarationType] and
      (__ \ "dispatchLocation").readNullable[DispatchLocation] and
      (__ \ "additionalDeclarationType").readNullable[AdditionalDeclarationType] and
      (__ \ "consignmentReferences").readNullable[ConsignmentReferences] and
      (__ \ "linkDucrToMucr").readNullable[YesNoAnswer] and
      (__ \ "mucr").readNullable[MUCR] and
      (__ \ "transport").read[Transport] and
      (__ \ "parties").read[Parties] and
      (__ \ "locations").read[Locations] and
      (__ \ "items").read[Seq[ExportItem]] and
      (__ \ "totalNumberOfItems").readNullable[TotalNumberOfItems] and
      (__ \ "previousDocuments").readNullable[PreviousDocuments] and
      (__ \ "natureOfTransaction").readNullable[NatureOfTransaction] and
      (__ \ "statementDescription").readNullable[String])(ExportsDeclaration.apply _)
}
