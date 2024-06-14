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
import org.mongodb.scala.model.Filters.{and, equal, exists, not}
import play.api.Logging
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.exports.migrations.TimeUtils.jsonWriter
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationMeta.{ContainerKey, RoutingCountryKey, SealKey}
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.Mongo
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class AddSequencingOfMultipleItems extends MigrationDefinition with Logging {

  private val maxSequenceIds = "declarationMeta.maxSequenceIds"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = s"CEDS-4370 Add sequencing for RoutingCountry, Container and Seal fields", order = 18, author = "Lucio Biondi")

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val collection = db.getCollection("declarations")

    val batchSize = 100

    val result = collection
      .find(not(exists(maxSequenceIds)))
      .batchSize(batchSize)
      .asScala
      .foldLeft(0) { (counter, document) =>
        Try(Json.parse(document.toJson(jsonWriter)).as[ExportsDeclaration](AddSequencingOfMultipleItems.reads)) match {
          case Failure(exc) =>
            val id = document.get("_id")
            val error = exc.getMessage
            logger.error(s"Error parsing document with _id($id): $error")
            counter + 1

          case Success(declaration) =>
            val maxSequenceIds = Map(
              ContainerKey -> declaration.transport.containers.fold(0)(_.size),
              RoutingCountryKey -> declaration.locations.routingCountries.size,
              SealKey -> declaration.transport.containers.fold(0)(_.foldLeft(0)(_ + _.seals.size))
            )
            val meta = declaration.declarationMeta
            val declarationWithSequenceIds = declaration.copy(declarationMeta = meta.copy(maxSequenceIds = maxSequenceIds))

            val filter = and(equal("eori", declaration.eori), equal("id", declaration.id))
            collection.replaceOne(filter, Document.parse(Json.toJson(declarationWithSequenceIds)(Mongo.format).toString))
            counter
        }
      }

    if (result > 1) logger.error(s"$result documents could not be migrated due to parsing errors.")
    else if (result == 1) logger.error(s"$result document could not be migrated due to parsing errors.")
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
}

object AddSequencingOfMultipleItems {

  implicit val readsForDeclarationMeta: Reads[DeclarationMeta] =
    ((__ \ "parentDeclarationId").readNullable[String] and
      (__ \ "parentDeclarationEnhancedStatus").readNullable[EnhancedStatus] and
      (__ \ "status").read[DeclarationStatus] and
      (__ \ "createdDateTime").read[Instant] and
      (__ \ "updatedDateTime").read[Instant] and
      (__ \ "summaryWasVisited").readNullable[Boolean] and
      (__ \ "readyForSubmission").readNullable[Boolean]) {

      (parentDeclarationId, parentEnhancedStatus, status, createdDateTime, updatedDateTime, summaryWasVisited, readyForSubmission) =>
        DeclarationMeta(parentDeclarationId, parentEnhancedStatus, status, createdDateTime, updatedDateTime, summaryWasVisited, readyForSubmission)
    }

  implicit val readsForLocations: Reads[Locations] =
    ((__ \ "originationCountry").readNullable[Country] and
      (__ \ "destinationCountry").readNullable[Country] and
      (__ \ "hasRoutingCountries").readNullable[Boolean] and
      (__ \ "routingCountries").read[Seq[Country]] and
      (__ \ "goodsLocation").readNullable[GoodsLocation] and
      (__ \ "officeOfExit").readNullable[OfficeOfExit] and
      (__ \ "supervisingCustomsOffice").readNullable[SupervisingCustomsOffice] and
      (__ \ "warehouseIdentification").readNullable[WarehouseIdentification] and
      (__ \ "inlandOrBorder").readNullable[InlandOrBorder] and
      (__ \ "inlandModeOfTransportCode").readNullable[InlandModeOfTransportCode]) {

      (
        originationCountry,
        destinationCountry,
        hasRoutingCountries,
        countries,
        goodsLocation,
        officeOfExit,
        supervisingCustomsOffice,
        warehouseIdentification,
        inlandOrBorder,
        inlandModeOfTransportCode
      ) =>
        val routingCountries = countries.zipWithIndex.map { case (country, ix) => RoutingCountry(ix + 1, country) }

        Locations(
          originationCountry,
          destinationCountry,
          hasRoutingCountries,
          routingCountries,
          goodsLocation,
          officeOfExit,
          supervisingCustomsOffice,
          warehouseIdentification,
          inlandOrBorder,
          inlandModeOfTransportCode
        )
    }

  case class FormerSeal(id: String)
  implicit val sealReads: Reads[FormerSeal] = Json.reads[FormerSeal]

  case class FormerContainer(id: String, seals: Seq[FormerSeal])
  implicit val containerReads: Reads[FormerContainer] = Json.reads[FormerContainer]

  implicit val readsForTransport: Reads[Transport] =
    ((__ \ "expressConsignment").readNullable[YesNoAnswer] and
      (__ \ "transportPayment").readNullable[TransportPayment] and
      (__ \ "containers").readNullable[Seq[FormerContainer]] and
      (__ \ "borderModeOfTransportCode").readNullable[TransportLeavingTheBorder] and
      (__ \ "meansOfTransportOnDepartureType").readNullable[String] and
      (__ \ "meansOfTransportOnDepartureIDNumber").readNullable[String] and
      (__ \ "transportCrossingTheBorderNationality").readNullable[TransportCountry] and
      (__ \ "meansOfTransportCrossingTheBorderType").readNullable[String] and
      (__ \ "meansOfTransportCrossingTheBorderIDNumber").readNullable[String]) {

      (
        expressConsignment,
        transportPayment,
        maybeContainers,
        borderModeOfTransportCode,
        meansOfTransportOnDepartureType,
        meansOfTransportOnDepartureIDNumber,
        transportCrossingTheBorderNationality,
        meansOfTransportCrossingTheBorderType,
        meansOfTransportCrossingTheBorderIDNumber
      ) =>
        val containers = maybeContainers.map(_.foldLeft((List.empty[Container], 0, 0)) { case ((containers, containerIx, sealsIx), container) =>
          val seals = container.seals.zipWithIndex.map { case (seal, ix) => Seal(sealsIx + ix + 1, seal.id) }
          (containers :+ Container(containerIx + 1, container.id, seals), containerIx + 1, sealsIx + container.seals.size)
        }._1)

        Transport(
          expressConsignment,
          transportPayment,
          containers,
          borderModeOfTransportCode,
          meansOfTransportOnDepartureType,
          meansOfTransportOnDepartureIDNumber,
          transportCrossingTheBorderNationality,
          meansOfTransportCrossingTheBorderType,
          meansOfTransportCrossingTheBorderIDNumber
        )
    }

  implicit val reads: Reads[ExportsDeclaration] =
    ((__ \ "id").read[String] and
      (__ \ "declarationMeta").read[DeclarationMeta] and
      (__ \ "eori").read[String] and
      (__ \ "type").read[DeclarationType] and
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
