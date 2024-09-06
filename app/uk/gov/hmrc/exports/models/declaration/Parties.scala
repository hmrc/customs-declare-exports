/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.YesNo

case class EntityDetails(eori: Option[String], address: Option[Address])

object EntityDetails {
  implicit val format: OFormat[EntityDetails] = Json.format[EntityDetails]
}

case class Address(fullName: String, addressLine: String, townOrCity: String, postCode: String, country: String)

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}

case class ExporterDetails(details: EntityDetails)

object ExporterDetails {
  implicit val format: OFormat[ExporterDetails] = Json.format[ExporterDetails]
}

case class IsExs(isExs: String)
object IsExs {
  implicit val format: OFormat[IsExs] = Json.format[IsExs]
}

case class ConsigneeDetails(details: EntityDetails)

object ConsigneeDetails {
  implicit val format: OFormat[ConsigneeDetails] = Json.format[ConsigneeDetails]
}

case class ConsignorDetails(details: EntityDetails)

object ConsignorDetails {
  implicit val format: OFormat[ConsignorDetails] = Json.format[ConsignorDetails]
}

case class DeclarantDetails(details: EntityDetails)

object DeclarantDetails {
  implicit val format: OFormat[DeclarantDetails] = Json.format[DeclarantDetails]
}

case class DeclarantIsExporter(answer: String) {
  def isExporter: Boolean = answer == YesNo.yes
}

object DeclarantIsExporter {
  implicit val format: OFormat[DeclarantIsExporter] = Json.format[DeclarantIsExporter]
}

case class RepresentativeDetails(details: Option[EntityDetails], statusCode: Option[String], representingOtherAgent: Option[String]) {
  def isRepresentingOtherAgent: Boolean = representingOtherAgent.contains(YesNo.yes)
}

object RepresentativeDetails {

  implicit val format: OFormat[RepresentativeDetails] = Json.format[RepresentativeDetails]

  // Fot testing purpose only
  val Declarant = "1"
  val DirectRepresentative = "2"
  val IndirectRepresentative = "3"
}

case class DeclarationAdditionalActors(actors: Seq[DeclarationAdditionalActor])

object DeclarationAdditionalActors {
  implicit val format: OFormat[DeclarationAdditionalActors] = Json.format[DeclarationAdditionalActors]
}

case class DeclarationAdditionalActor(eori: Option[String], partyType: Option[String])

object DeclarationAdditionalActor {
  implicit val format: OFormat[DeclarationAdditionalActor] = Json.format[DeclarationAdditionalActor]
}

case class DeclarationHolders(holders: Seq[DeclarationHolder], isRequired: Option[YesNoAnswer])

object DeclarationHolders {
  implicit val format: OFormat[DeclarationHolders] = Json.format[DeclarationHolders]
}

case class DeclarationHolder(authorisationTypeCode: Option[String], eori: Option[String], eoriSource: Option[EoriSource])

object DeclarationHolder {
  implicit val format: OFormat[DeclarationHolder] = Json.format[DeclarationHolder]
}

case class CarrierDetails(details: EntityDetails)

object CarrierDetails {
  implicit val format: OFormat[CarrierDetails] = Json.format[CarrierDetails]
}

case class YesNoAnswer(answer: String)

object YesNoAnswer {
  implicit val format: OFormat[YesNoAnswer] = Json.format[YesNoAnswer]

  val yes = YesNoAnswer(YesNo.yes)
  val no = YesNoAnswer(YesNo.no)
}

case class PersonPresentingGoodsDetails(eori: Eori)

object PersonPresentingGoodsDetails {
  implicit val format: OFormat[PersonPresentingGoodsDetails] = Json.format[PersonPresentingGoodsDetails]
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
)

object Parties {
  implicit val format: OFormat[Parties] = Json.format[Parties]
}

object PartyType {
  val Consolidator = "CS"
  val Manufacturer = "MF"
  val FreightForwarder = "FW"
  val WarehouseKeeper = "WH"
}
