/*
 * Copyright 2019 HM Revenue & Customs
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

package util.testdata

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
    departureTransport = None,
    borderTransport = None,
    containerData = None,
    parties = Parties(),
    locations = Locations(),
    items = Set.empty[ExportItem],
    totalNumberOfItems = None,
    previousDocuments = None,
    natureOfTransaction = None
  )

  def aDeclaration(modifiers: ExportsDeclarationModifier*): ExportsDeclaration =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  def withId(id: String): ExportsDeclarationModifier = _.copy(id = id)

  // ************************************************* Builders ********************************************************

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

  def withoutDepartureTransport(): ExportsDeclarationModifier = _.copy(departureTransport = None)

  def withDepartureTransport(
    borderModeOfTransportCode: String = "",
    meansOfTransportOnDepartureType: String = "",
    meansOfTransportOnDepartureIDNumber: Option[String] = None
  ): ExportsDeclarationModifier =
    _.copy(
      departureTransport = Some(DepartureTransport(borderModeOfTransportCode, meansOfTransportOnDepartureType, meansOfTransportOnDepartureIDNumber))
    )

  def withoutContainerData(): ExportsDeclarationModifier = _.copy(containerData = None)

  def withContainerData(data: TransportInformationContainer*): ExportsDeclarationModifier =
    cache => cache.copy(containerData = Some(TransportInformationContainers(cache.containerData.map(_.containers).getOrElse(Seq.empty) ++ data)))

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

  def withoutDestinationCountries(): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(destinationCountries = None))

  def withDestinationCountries(
    countryOfDispatch: String = "GB",
    countriesOfRouting: Seq[String] = Seq.empty,
    countryOfDestination: String = "US"
  ): ExportsDeclarationModifier =
    m =>
      m.copy(
        locations = m.locations.copy(destinationCountries = Some(DestinationCountries(countryOfDispatch, countriesOfRouting, countryOfDestination)))
    )

  def withoutGoodsLocation(): ExportsDeclarationModifier =
    m => m.copy(locations = m.locations.copy(goodsLocation = None))

  def withGoodsLocation(goodsLocation: GoodsLocation): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(goodsLocation = Some(goodsLocation)))
  }

  def withoutWarehouseIdentification(): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(warehouseIdentification = None))

  def withWarehouseIdentification(warehouseIdentification: WarehouseIdentification): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(warehouseIdentification = Some(warehouseIdentification)))

  def withWarehouseIdentification(
    supervisingCustomsOffice: Option[String] = None,
    identificationType: Option[String] = None,
    identificationNumber: Option[String] = None,
    inlandModeOfTransportCode: Option[String] = None
  ): ExportsDeclarationModifier =
    cache =>
      cache.copy(
        locations = cache.locations.copy(
          warehouseIdentification =
            Some(WarehouseIdentification(supervisingCustomsOffice, identificationType, identificationNumber, inlandModeOfTransportCode))
        )
    )

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

  private def uuid: String = UUID.randomUUID().toString

  def withoutTotalNumberOfItems(): ExportsDeclarationModifier = _.copy(totalNumberOfItems = None)

  def withTotalNumberOfItems(
    totalAmountInvoiced: Option[String] = None,
    exchangeRate: Option[String] = None,
    totalPackage: String = "1"
  ): ExportsDeclarationModifier =
    _.copy(totalNumberOfItems = Some(TotalNumberOfItems(totalAmountInvoiced, exchangeRate, totalPackage)))

  def withoutNatureOfTransaction(): ExportsDeclarationModifier = _.copy(natureOfTransaction = None)

  def withNatureOfTransaction(natureType: String): ExportsDeclarationModifier =
    _.copy(natureOfTransaction = Some(NatureOfTransaction(natureType)))

  def withoutBorderTransport(): ExportsDeclarationModifier = _.copy(borderTransport = None)

  def withBorderTransport(details: BorderTransport): ExportsDeclarationModifier =
    _.copy(borderTransport = Some(details))

  def withBorderTransport(
    meansOfTransportCrossingTheBorderNationality: Option[String] = None,
    container: Boolean = false,
    meansOfTransportCrossingTheBorderType: String = "",
    meansOfTransportCrossingTheBorderIDNumber: Option[String] = None,
    paymentMethod: Option[String] = None
  ): ExportsDeclarationModifier =
    _.copy(
      borderTransport = Some(
        BorderTransport(
          meansOfTransportCrossingTheBorderNationality = meansOfTransportCrossingTheBorderNationality,
          container = container,
          meansOfTransportCrossingTheBorderType = meansOfTransportCrossingTheBorderType,
          meansOfTransportCrossingTheBorderIDNumber = meansOfTransportCrossingTheBorderIDNumber,
          paymentMethod = paymentMethod
        )
      )
    )

  def withUpdateDate(year: Int, month: Int, dayOfMonth: Int): ExportsDeclarationModifier =
    _.copy(updatedDateTime = LocalDateTime.of(year, month, dayOfMonth, 10, 0, 0).toInstant(ZoneOffset.UTC))

}
