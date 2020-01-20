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

package testdata

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.{DeclarationType, Eori}

//noinspection ScalaStyle
trait ExportsDeclarationBuilder {

  private type ExportsDeclarationModifier = ExportsDeclaration => ExportsDeclaration
  protected val VALID_PERSONAL_UCR = "5GB123456789000"
  protected val VALID_DUCR = "5GB123456789000-123ABC456DEFIIIII"
  protected val VALID_LRN = "FG7676767889"

  def aDeclaration(modifiers: ExportsDeclarationModifier*): ExportsDeclaration =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  private def modelWithDefaults: ExportsDeclaration = ExportsDeclaration(
    id = uuid,
    eori = "eori",
    status = DeclarationStatus.COMPLETE,
    createdDateTime = LocalDateTime.of(2019, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC),
    updatedDateTime = LocalDateTime.of(2019, 2, 2, 0, 0, 0).toInstant(ZoneOffset.UTC),
    sourceId = None,
    `type` = DeclarationType.STANDARD,
    dispatchLocation = None,
    additionalDeclarationType = None,
    consignmentReferences = None,
    transport = Transport(),
    parties = Parties(),
    locations = Locations(),
    items = Set.empty[ExportItem],
    totalNumberOfItems = None,
    previousDocuments = None,
    natureOfTransaction = None
  )

  private def uuid: String = UUID.randomUUID().toString

  // ************************************************* Builders ********************************************************

  def withId(id: String): ExportsDeclarationModifier = _.copy(id = id)

  def withEori(eori: String): ExportsDeclarationModifier = _.copy(eori = eori)

  def withEori(eori: Eori): ExportsDeclarationModifier = _.copy(eori = eori.value)

  def withStatus(status: DeclarationStatus): ExportsDeclarationModifier = _.copy(status = status)

  def withType(`type`: DeclarationType): ExportsDeclarationModifier = _.copy(`type` = `type`)

  def withoutAdditionalDeclarationType(): ExportsDeclarationModifier = _.copy(additionalDeclarationType = None)

  def withAdditionalDeclarationType(decType: AdditionalDeclarationType = AdditionalDeclarationType.STANDARD_FRONTIER): ExportsDeclarationModifier =
    _.copy(additionalDeclarationType = Some(decType))

  def withoutDispatchLocation: ExportsDeclarationModifier = _.copy(dispatchLocation = None)

  def withDispatchLocation(location: String = "GB"): ExportsDeclarationModifier =
    _.copy(dispatchLocation = Some(DispatchLocation(location)))

  def withoutConsignmentReferences(): ExportsDeclarationModifier = _.copy(consignmentReferences = None)

  def withConsignmentReferences(
    ducr: String = VALID_DUCR,
    lrn: String = VALID_LRN,
    personalUcr: Option[String] = Some(VALID_DUCR)
  ): ExportsDeclarationModifier =
    _.copy(consignmentReferences = Some(ConsignmentReferences(DUCR(ducr), lrn, personalUcr)))

  def withoutDepartureTransport(): ExportsDeclarationModifier =
    declaration =>
      declaration.copy(
        transport = declaration.transport
          .copy(borderModeOfTransportCode = None, meansOfTransportOnDepartureIDNumber = None, meansOfTransportOnDepartureType = None)
    )

  def withDepartureTransport(
    borderModeOfTransportCode: String = "",
    meansOfTransportOnDepartureType: String = "",
    meansOfTransportOnDepartureIDNumber: String = ""
  ): ExportsDeclarationModifier =
    declaration =>
      declaration.copy(
        transport = declaration.transport.copy(
          borderModeOfTransportCode = Some(borderModeOfTransportCode),
          meansOfTransportOnDepartureIDNumber = Some(meansOfTransportOnDepartureIDNumber),
          meansOfTransportOnDepartureType = Some(meansOfTransportOnDepartureType)
        )
    )

  def withoutContainerData(): ExportsDeclarationModifier =
    declaration => declaration.copy(transport = declaration.transport.copy(containers = None))

  def withContainerData(data: Container*): ExportsDeclarationModifier =
    declaration => declaration.copy(transport = declaration.transport.copy(containers = Some(data)))

  def withPreviousDocuments(previousDocuments: PreviousDocument*): ExportsDeclarationModifier =
    _.copy(previousDocuments = Some(PreviousDocuments(previousDocuments)))

  def withoutExporterDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(exporterDetails = None))

  def withExporterDetails(eori: Option[String] = None, address: Option[Address] = None): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(exporterDetails = Some(ExporterDetails(EntityDetails(eori, address)))))

  def withoutConsigneeDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(consigneeDetails = None))

  def withConsigneeDetails(eori: Option[String], address: Option[Address]): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(consigneeDetails = Some(ConsigneeDetails(EntityDetails(eori, address)))))

  def withoutDeclarantDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarantDetails = None))

  def withDeclarantDetails(eori: Option[String] = None, address: Option[Address] = None): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarantDetails = Some(DeclarantDetails(EntityDetails(eori, address)))))

  def withoutRepresentativeDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(representativeDetails = None))

  def withRepresentativeDetails(eori: Option[String], address: Option[Address], statusCode: Option[String]): ExportsDeclarationModifier =
    withRepresentativeDetails(Some(EntityDetails(eori, address)), statusCode)

  def withRepresentativeDetails(details: Option[EntityDetails], statusCode: Option[String]): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(representativeDetails = Some(RepresentativeDetails(details, statusCode))))

  def withDeclarationAdditionalActors(data: DeclarationAdditionalActor*): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarationAdditionalActorsData = Some(DeclarationAdditionalActors(data))))

  def withoutDeclarationHolders(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarationHoldersData = None))

  def withDeclarationHolders(holders: DeclarationHolder*): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarationHoldersData = Some(DeclarationHolders(holders))))

  def withoutCarrierDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(carrierDetails = None))

  def withCarrierDetails(eori: Option[String] = None, address: Option[Address] = None): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(carrierDetails = Some(CarrierDetails(EntityDetails(eori, address)))))

  def withoutOriginationCountry(): ExportsDeclarationModifier = cache => cache.copy(locations = cache.locations.copy(originationCountry = None))

  def withOriginationCountry(country: String = "GB"): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(originationCountry = Some(country)))

  def withoutDestinationCountry(): ExportsDeclarationModifier = cache => cache.copy(locations = cache.locations.copy(destinationCountry = None))

  def withDestinationCountry(country: String = "GB"): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(destinationCountry = Some(country)))

  def withoutRoutingCountries(): ExportsDeclarationModifier = cache => cache.copy(locations = cache.locations.copy(routingCountries = Seq.empty))

  def withRoutingCountries(countries: Seq[String] = Seq("GB", "PL")): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(routingCountries = countries))

  def withoutGoodsLocation(): ExportsDeclarationModifier =
    m => m.copy(locations = m.locations.copy(goodsLocation = None))

  def withGoodsLocation(goodsLocation: GoodsLocation): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(goodsLocation = Some(goodsLocation)))
  }

  def withWarehouseIdentification(warehouseIdentification: String): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(warehouseIdentification = Some(WarehouseIdentification(Some(warehouseIdentification)))))
  }

  def withInlandModeOfTransport(inlandModeOfTransportCode: String): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(inlandModeOfTransportCode = Some(InlandModeOfTransportCode(Some(inlandModeOfTransportCode)))))
  }

  def withSupervisingCustomsOffice(supervisingCustomsOffice: String): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(supervisingCustomsOffice = Some(SupervisingCustomsOffice(Some(supervisingCustomsOffice)))))
  }

  def withoutOfficeOfExit(): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(officeOfExit = None))

  def withOfficeOfExit(
    officeId: String = "",
    presentationOfficeId: Option[String] = None,
    circumstancesCode: Option[String] = None
  ): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(officeOfExit = Some(OfficeOfExit(officeId, presentationOfficeId, circumstancesCode))))

  def withoutItems(): ExportsDeclarationModifier = _.copy(items = Set.empty)

  def withItem(item: ExportItem = ExportItem(uuid)): ExportsDeclarationModifier =
    m => m.copy(items = m.items + item)

  def withItems(item1: ExportItem, others: ExportItem*): ExportsDeclarationModifier =
    _.copy(items = Set(item1) ++ others)

  def withItems(count: Int): ExportsDeclarationModifier =
    cache => cache.copy(items = cache.items ++ (1 to count).map(_ => ExportItem(id = uuid)).toSet)

  def withoutTotalNumberOfItems(): ExportsDeclarationModifier = _.copy(totalNumberOfItems = None)

  def withTotalNumberOfItems(
    totalAmountInvoiced: Option[String] = None,
    exchangeRate: Option[String] = None,
    totalPackage: String = "1"
  ): ExportsDeclarationModifier =
    _.copy(totalNumberOfItems = Some(TotalNumberOfItems(totalAmountInvoiced, exchangeRate, Some(totalPackage))))

  def withoutNatureOfTransaction(): ExportsDeclarationModifier = _.copy(natureOfTransaction = None)

  def withNatureOfTransaction(natureType: String): ExportsDeclarationModifier =
    _.copy(natureOfTransaction = Some(NatureOfTransaction(natureType)))

  def withoutBorderTransport(): ExportsDeclarationModifier =
    declaration =>
      declaration.copy(
        transport = declaration.transport.copy(
          meansOfTransportCrossingTheBorderIDNumber = None,
          meansOfTransportCrossingTheBorderNationality = None,
          meansOfTransportCrossingTheBorderType = None
        )
    )

  def withBorderTransport(
    meansOfTransportCrossingTheBorderNationality: Option[String] = None,
    meansOfTransportCrossingTheBorderType: String = "",
    meansOfTransportCrossingTheBorderIDNumber: Option[String] = None
  ): ExportsDeclarationModifier =
    declaration =>
      declaration.copy(
        transport = declaration.transport.copy(
          meansOfTransportCrossingTheBorderNationality = meansOfTransportCrossingTheBorderNationality,
          meansOfTransportCrossingTheBorderType = Some(meansOfTransportCrossingTheBorderType),
          meansOfTransportCrossingTheBorderIDNumber = meansOfTransportCrossingTheBorderIDNumber
        )
    )

  def withTransportPayment(payment: Option[String]): ExportsDeclarationModifier =
    declaration => declaration.copy(transport = declaration.transport.copy(transportPayment = Some(TransportPayment(payment))))

  def withUpdateDate(year: Int, month: Int, dayOfMonth: Int): ExportsDeclarationModifier =
    _.copy(updatedDateTime = LocalDateTime.of(year, month, dayOfMonth, 10, 0, 0).toInstant(ZoneOffset.UTC))

}
