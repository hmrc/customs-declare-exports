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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.exports.models.{Eori, FieldMapping}
import uk.gov.hmrc.exports.models.ExportsFieldPointer.ExportsFieldPointer
import uk.gov.hmrc.exports.services.DiffTools
import uk.gov.hmrc.exports.services.DiffTools._

case class EntityDetails(eori: Option[String], address: Option[Address]) extends DiffTools[EntityDetails] {
  override def createDiff(original: EntityDetails, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(original.eori, eori, combinePointers(pointerString, EntityDetails.eoriPointer, sequenceNbr)),
      createDiffOfOptions(original.address, address, combinePointers(pointerString, Address.pointer, sequenceNbr))
    ).flatten
}

object EntityDetails extends FieldMapping {
  implicit val format: OFormat[EntityDetails] = Json.format[EntityDetails]

  val pointer: ExportsFieldPointer = "details"
  val eoriPointer: ExportsFieldPointer = "eori"
}

case class Address(fullName: String, addressLine: String, townOrCity: String, postCode: String, country: String) extends DiffTools[Address] {
  override def createDiff(original: Address, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(original.fullName, fullName, combinePointers(pointerString, Address.fullNamePointer, sequenceNbr)),
      compareStringDifference(original.addressLine, addressLine, combinePointers(pointerString, Address.addressLinePointer, sequenceNbr)),
      compareStringDifference(original.townOrCity, townOrCity, combinePointers(pointerString, Address.townOrCityPointer, sequenceNbr)),
      compareStringDifference(original.postCode, postCode, combinePointers(pointerString, Address.postCodePointer, sequenceNbr)),
      compareStringDifference(original.country, country, combinePointers(pointerString, Address.countryPointer, sequenceNbr))
    ).flatten
}
object Address extends FieldMapping {
  implicit val format: OFormat[Address] = Json.format[Address]

  val pointer: ExportsFieldPointer = "address"
  val fullNamePointer: ExportsFieldPointer = "fullName"
  val addressLinePointer: ExportsFieldPointer = "addressLine"
  val townOrCityPointer: ExportsFieldPointer = "townOrCity"
  val postCodePointer: ExportsFieldPointer = "postCode"
  val countryPointer: ExportsFieldPointer = "country"
}

case class ExporterDetails(details: EntityDetails) extends DiffTools[ExporterDetails] {
  override def createDiff(original: ExporterDetails, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(details.createDiff(original.details, combinePointers(pointerString, EntityDetails.pointer, sequenceNbr))).flatten
}

object ExporterDetails extends FieldMapping {
  implicit val format: OFormat[ExporterDetails] = Json.format[ExporterDetails]

  val pointer: ExportsFieldPointer = "exporterDetails"
}

case class IsExs(isExs: String)
object IsExs {
  implicit val format: OFormat[IsExs] = Json.format[IsExs]
}

case class ConsigneeDetails(details: EntityDetails) extends DiffTools[ConsigneeDetails] {
  override def createDiff(original: ConsigneeDetails, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(details.createDiff(original.details, combinePointers(pointerString, ConsigneeDetails.pointer, sequenceNbr))).flatten
}
object ConsigneeDetails extends FieldMapping {
  implicit val format: OFormat[ConsigneeDetails] = Json.format[ConsigneeDetails]

  val pointer: ExportsFieldPointer = "consigneeDetails"
}

case class ConsignorDetails(details: EntityDetails) extends DiffTools[ConsignorDetails] {
  override def createDiff(original: ConsignorDetails, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(details.createDiff(original.details, combinePointers(pointerString, ConsignorDetails.pointer, sequenceNbr))).flatten
}

object ConsignorDetails extends FieldMapping {
  implicit val format: OFormat[ConsignorDetails] = Json.format[ConsignorDetails]

  val pointer: ExportsFieldPointer = "consignorDetails"
}

case class DeclarantDetails(details: EntityDetails) extends DiffTools[DeclarantDetails] {
  override def createDiff(original: DeclarantDetails, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(details.createDiff(original.details, combinePointers(pointerString, DeclarantDetails.pointer, sequenceNbr))).flatten
}

object DeclarantDetails extends FieldMapping {
  implicit val format: OFormat[DeclarantDetails] = Json.format[DeclarantDetails]

  val pointer: ExportsFieldPointer = "declarantDetails"
}

case class DeclarantIsExporter(answer: String) {
  def isExporter: Boolean = answer == "Yes"
}
object DeclarantIsExporter {
  implicit val format: OFormat[DeclarantIsExporter] = Json.format[DeclarantIsExporter]
}

case class RepresentativeDetails(details: Option[EntityDetails], statusCode: Option[String], representingOtherAgent: Option[String])
    extends DiffTools[RepresentativeDetails] {
  def isRepresentingOtherAgent = representingOtherAgent.contains("Yes")

  // representingOtherAgent field is not used to generate WCO XML
  override def createDiff(
    original: RepresentativeDetails,
    pointerString: ExportsFieldPointer,
    sequenceNbr: Option[Int] = None
  ): ExportsDeclarationDiff =
    Seq(
      createDiffOfOptions(original.details, details, combinePointers(pointerString, RepresentativeDetails.detailsPointer, sequenceNbr)),
      compareStringDifference(original.statusCode, statusCode, combinePointers(pointerString, RepresentativeDetails.statusCodePointer, sequenceNbr))
    ).flatten
}
object RepresentativeDetails extends FieldMapping {

  implicit val format: OFormat[RepresentativeDetails] = Json.format[RepresentativeDetails]

  // Fot testing purpose only
  val Declarant = "1"
  val DirectRepresentative = "2"
  val IndirectRepresentative = "3"

  val pointer: ExportsFieldPointer = "representativeDetails"
  val detailsPointer: ExportsFieldPointer = "details"
  val statusCodePointer: ExportsFieldPointer = "statusCode"
}

case class DeclarationAdditionalActors(actors: Seq[DeclarationAdditionalActor]) extends DiffTools[DeclarationAdditionalActors] {
  def createDiff(original: DeclarationAdditionalActors, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    createDiff(original.actors, actors, combinePointers(pointerString, DeclarationAdditionalActor.pointer, None))
      .map(
        removeTrailingSequenceNbr(_)
      ) // This entity is unique in being a sequence of items but not having a SequenceNumber (so we strip off the numeric part)
}
object DeclarationAdditionalActors extends FieldMapping {
  implicit val format: OFormat[DeclarationAdditionalActors] = Json.format[DeclarationAdditionalActors]

  val pointer: ExportsFieldPointer = "declarationAdditionalActorsData"
}

case class DeclarationAdditionalActor(eori: Option[String], partyType: Option[String]) extends DiffTools[DeclarationAdditionalActor] {
  def createDiff(original: DeclarationAdditionalActor, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(original.eori, eori, combinePointers(pointerString, DeclarationAdditionalActor.eoriPointer, None)),
      compareStringDifference(original.partyType, partyType, combinePointers(pointerString, DeclarationAdditionalActor.partyTypePointer, None))
    ).flatten
}

object DeclarationAdditionalActor extends FieldMapping {
  implicit val format: OFormat[DeclarationAdditionalActor] = Json.format[DeclarationAdditionalActor]

  val pointer: ExportsFieldPointer = "actors"
  val eoriPointer: ExportsFieldPointer = "eori"
  val partyTypePointer: ExportsFieldPointer = "partyType"
}

case class DeclarationHolders(holders: Seq[DeclarationHolder], isRequired: Option[YesNoAnswer]) extends DiffTools[DeclarationHolders] {
  // isRequired field is not used to generate the WCO XML
  def createDiff(original: DeclarationHolders, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    createDiff(original.holders, holders, combinePointers(pointerString, DeclarationHolder.pointer, None))
}

object DeclarationHolders extends FieldMapping {
  implicit val format: OFormat[DeclarationHolders] = Json.format[DeclarationHolders]

  val pointer: ExportsFieldPointer = "declarationHoldersData"
}

case class DeclarationHolder(authorisationTypeCode: Option[String], eori: Option[String], eoriSource: Option[EoriSource])
    extends DiffTools[DeclarationHolder] {

  // eoriSource is not used to generate the WCO XML
  def createDiff(original: DeclarationHolder, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(
        original.authorisationTypeCode,
        authorisationTypeCode,
        combinePointers(pointerString, DeclarationHolder.authorisationTypeCodePointer, sequenceNbr)
      ),
      compareStringDifference(original.eori, eori, combinePointers(pointerString, DeclarationHolder.eoriPointer, sequenceNbr))
    ).flatten
}

object DeclarationHolder extends FieldMapping {
  implicit val format: OFormat[DeclarationHolder] = Json.format[DeclarationHolder]

  val pointer: ExportsFieldPointer = "holders"
  val authorisationTypeCodePointer: ExportsFieldPointer = "authorisationTypeCode"
  val eoriPointer: ExportsFieldPointer = "eori"
}

case class CarrierDetails(details: EntityDetails) extends DiffTools[CarrierDetails] {
  override def createDiff(original: CarrierDetails, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(details.createDiff(original.details, combinePointers(pointerString, CarrierDetails.pointer, sequenceNbr))).flatten
}

object CarrierDetails extends FieldMapping {
  implicit val format: OFormat[CarrierDetails] = Json.format[CarrierDetails]

  val pointer: ExportsFieldPointer = "carrierDetails"
}

case class YesNoAnswer(answer: String) extends Ordered[YesNoAnswer] {
  override def compare(that: YesNoAnswer): Int = answer.compare(that.answer)
}

object YesNoAnswer {
  implicit val format: OFormat[YesNoAnswer] = Json.format[YesNoAnswer]

  object YesNoStringAnswers {
    val yes = "Yes"
    val no = "No"
  }

  val yes = YesNoAnswer(YesNoStringAnswers.yes)
  val no = YesNoAnswer(YesNoStringAnswers.no)
}

case class PersonPresentingGoodsDetails(eori: Eori) extends DiffTools[PersonPresentingGoodsDetails] {
  override def createDiff(
    original: PersonPresentingGoodsDetails,
    pointerString: ExportsFieldPointer,
    sequenceNbr: Option[Int] = None
  ): ExportsDeclarationDiff =
    Seq(compareDifference(original.eori, eori, combinePointers(pointerString, Eori.pointer, sequenceNbr))).flatten
}

object PersonPresentingGoodsDetails extends FieldMapping {
  implicit val format: OFormat[PersonPresentingGoodsDetails] = Json.format[PersonPresentingGoodsDetails]

  val pointer: ExportsFieldPointer = "personPresentingGoodsDetails"
}

case class Parties(
  exporterDetails: Option[ExporterDetails] = None,
  isExs: Option[IsExs] = None,
  consigneeDetails: Option[ConsigneeDetails] = None,
  consignorDetails: Option[ConsignorDetails] = None,
  declarantDetails: Option[DeclarantDetails] = None,
  declarantIsExporter: Option[DeclarantIsExporter] = None,
  representativeDetails: Option[RepresentativeDetails] = None,
  declarationAdditionalActorsData: Option[DeclarationAdditionalActors] = None,
  declarationHoldersData: Option[DeclarationHolders] = None,
  authorisationProcedureCodeChoice: Option[AuthorisationProcedureCodeChoice] = None,
  carrierDetails: Option[CarrierDetails] = None,
  isEntryIntoDeclarantsRecords: Option[YesNoAnswer] = None,
  personPresentingGoodsDetails: Option[PersonPresentingGoodsDetails] = None
) extends DiffTools[Parties] {

  // isExs, declarantIsExporter & authorisationProcedureCodeChoice fields are not used to create WCO XML
  override def createDiff(original: Parties, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareDifference(
        original.isEntryIntoDeclarantsRecords,
        isEntryIntoDeclarantsRecords,
        combinePointers(pointerString, Parties.isEntryIntoDeclarantsRecordsPointer, sequenceNbr)
      )
    ).flatten ++
      createDiffOfOptions(original.exporterDetails, exporterDetails, combinePointers(pointerString, ExporterDetails.pointer, sequenceNbr)) ++
      createDiffOfOptions(original.consigneeDetails, consigneeDetails, combinePointers(pointerString, ConsigneeDetails.pointer, sequenceNbr)) ++
      createDiffOfOptions(original.consignorDetails, consignorDetails, combinePointers(pointerString, ConsignorDetails.pointer, sequenceNbr)) ++
      createDiffOfOptions(original.declarantDetails, declarantDetails, combinePointers(pointerString, DeclarantDetails.pointer, sequenceNbr)) ++
      createDiffOfOptions(
        original.representativeDetails,
        representativeDetails,
        combinePointers(pointerString, RepresentativeDetails.pointer, sequenceNbr)
      ) ++
      createDiffOfOptions(
        original.declarationAdditionalActorsData,
        declarationAdditionalActorsData,
        combinePointers(pointerString, DeclarationAdditionalActors.pointer, sequenceNbr)
      ) ++
      createDiffOfOptions(original.carrierDetails, carrierDetails, combinePointers(pointerString, CarrierDetails.pointer, sequenceNbr)) ++
      createDiffOfOptions(
        original.personPresentingGoodsDetails,
        personPresentingGoodsDetails,
        combinePointers(pointerString, PersonPresentingGoodsDetails.pointer, sequenceNbr)
      )
}
object Parties extends FieldMapping {
  implicit val format: OFormat[Parties] = Json.format[Parties]

  val pointer: ExportsFieldPointer = "parties"
  val isEntryIntoDeclarantsRecordsPointer: ExportsFieldPointer = "personPresentingGoodsDetails.eori"
}

object PartyType {
  val Consolidator = "CS"
  val Manufacturer = "MF"
  val FreightForwarder = "FW"
  val WarehouseKeeper = "WH"
}
