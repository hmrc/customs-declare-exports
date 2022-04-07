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

package uk.gov.hmrc.exports.util

import uk.gov.hmrc.exports.models.{DeclarationType, Eori}
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration._

import java.time.{Instant, ZoneOffset, ZonedDateTime}

//noinspection ScalaStyle
trait ExportsDeclarationBuilder extends ExportsItemBuilder {

  private type ExportsDeclarationModifier = ExportsDeclaration => ExportsDeclaration
  protected val VALID_PERSONAL_UCR = "5GB123456789000"
  protected val VALID_DUCR = "5GB123456789000-123ABC456DEFIIIII"
  protected val VALID_LRN = "FG7676767889"
  protected val VALID_MUCR = "GB/123452971100-101SHIP2"
  protected val VALID_EORI = "9GB1234567ABCDEF"
  protected val WAREHOUSE_ID = "RGBWKG001"
  protected val VALID_COUNTRY = "GB"

  def aDeclaration(modifiers: ExportsDeclarationModifier*): ExportsDeclaration =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  private def modelWithDefaults: ExportsDeclaration = ExportsDeclaration(
    id = uuid,
    eori = "eori",
    status = DeclarationStatus.COMPLETE,
    createdDateTime = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant,
    updatedDateTime = ZonedDateTime.of(2019, 2, 2, 0, 0, 0, 0, ZoneOffset.UTC).toInstant,
    sourceId = None,
    `type` = DeclarationType.STANDARD,
    dispatchLocation = None,
    additionalDeclarationType = None,
    consignmentReferences = None,
    linkDucrToMucr = None,
    mucr = None,
    transport = Transport(),
    parties = Parties(),
    locations = Locations(),
    items = Seq.empty,
    readyForSubmission = None,
    totalNumberOfItems = None,
    previousDocuments = None,
    natureOfTransaction = None
  )

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
    personalUcr: Option[String] = Some(VALID_DUCR),
    eidrDateStamp: Option[String] = None,
    mrn: Option[String] = None
  ): ExportsDeclarationModifier =
    _.copy(consignmentReferences = Some(ConsignmentReferences(DUCR(ducr), lrn, personalUcr, eidrDateStamp, mrn)))

  def withoutDepartureTransport(): ExportsDeclarationModifier =
    declaration =>
      declaration.copy(
        transport = declaration.transport
          .copy(borderModeOfTransportCode = None, meansOfTransportOnDepartureIDNumber = None, meansOfTransportOnDepartureType = None)
    )

  def withDepartureTransport(
    borderModeOfTransportCode: ModeOfTransportCode = ModeOfTransportCode.Empty,
    meansOfTransportOnDepartureType: String = "",
    meansOfTransportOnDepartureIDNumber: String = ""
  ): ExportsDeclarationModifier =
    withDepartureTransport(
      TransportLeavingTheBorder(Some(borderModeOfTransportCode)),
      meansOfTransportOnDepartureType,
      meansOfTransportOnDepartureIDNumber
    )

  def withDepartureTransport(
    borderModeOfTransportCode: TransportLeavingTheBorder,
    meansOfTransportOnDepartureType: String,
    meansOfTransportOnDepartureIDNumber: String
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

  def withConsignorDetails(eori: Option[String], address: Option[Address]): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(consignorDetails = Some(ConsignorDetails(EntityDetails(eori, address)))))

  def withoutIsEntryIntoDeclarantsRecords(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(isEntryIntoDeclarantsRecords = None))

  def withIsEntryIntoDeclarantsRecords(answer: String = "Yes"): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(isEntryIntoDeclarantsRecords = Some(YesNoAnswer(answer))))

  def withoutPersonPresentingGoodsDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(personPresentingGoodsDetails = None))

  def withPersonPresentingGoodsDetails(eori: Eori): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(personPresentingGoodsDetails = Some(PersonPresentingGoodsDetails(eori))))

  def withoutDeclarantDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarantDetails = None))

  def withDeclarantDetails(eori: Option[String] = None, address: Option[Address] = None): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarantDetails = Some(DeclarantDetails(EntityDetails(eori, address)))))

  def withDeclarantIsExporter(answer: String = "Yes"): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarantIsExporter = Some(DeclarantIsExporter(answer))))

  def withoutRepresentativeDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(representativeDetails = None))

  def withRepresentativeDetails(
    details: Option[EntityDetails],
    statusCode: Option[String],
    representingAnotherAgent: Option[String] = Some("Yes")
  ): ExportsDeclarationModifier =
    cache =>
      cache.copy(
        parties = cache.parties
          .copy(representativeDetails = Some(RepresentativeDetails(details, statusCode, representingAnotherAgent)))
    )

  def withDeclarationAdditionalActors(data: DeclarationAdditionalActor*): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarationAdditionalActorsData = Some(DeclarationAdditionalActors(data))))

  def withoutDeclarationHolders(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarationHoldersData = None))

  def withDeclarationHolders(holders: DeclarationHolder*): ExportsDeclarationModifier = {
    val isRequired = Some(YesNoAnswer(if (holders.isEmpty) "No" else "Yes"))
    cache =>
      cache.copy(parties = cache.parties.copy(declarationHoldersData = Some(DeclarationHolders(holders, isRequired))))
  }

  def withoutCarrierDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(carrierDetails = None))

  def withCarrierDetails(eori: Option[String] = None, address: Option[Address] = None): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(carrierDetails = Some(CarrierDetails(EntityDetails(eori, address)))))

  def withoutOriginationCountry(): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(originationCountry = None))

  def withOriginationCountry(country: Country = Country(Some("GB"))): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(originationCountry = Some(country)))

  def withoutDestinationCountry(): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(destinationCountry = None))

  def withEmptyDestinationCountry(): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(destinationCountry = Some(Country(None))))

  def withDestinationCountry(country: Country = Country(Some("GB"))): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(destinationCountry = Some(country)))

  def withoutRoutingCountries(): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(routingCountries = Seq.empty))

  def withRoutingCountries(countries: Seq[Country] = Seq(Country(Some("GB")), Country(Some("PL")))): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(routingCountries = countries))

  def withoutGoodsLocation(): ExportsDeclarationModifier =
    m => m.copy(locations = m.locations.copy(goodsLocation = None))

  def withGoodsLocation(goodsLocation: GoodsLocation): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(goodsLocation = Some(goodsLocation)))
  }

  def withWarehouseIdentification(warehouseIdentification: String): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(warehouseIdentification = Some(WarehouseIdentification(Some(warehouseIdentification)))))
  }

  def withInlandModeOfTransport(inlandModeOfTransportCode: ModeOfTransportCode = ModeOfTransportCode.Empty): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(inlandModeOfTransportCode = Some(InlandModeOfTransportCode(Some(inlandModeOfTransportCode)))))
  }

  def withSupervisingCustomsOffice(supervisingCustomsOffice: String): ExportsDeclarationModifier = { m =>
    m.copy(locations = m.locations.copy(supervisingCustomsOffice = Some(SupervisingCustomsOffice(Some(supervisingCustomsOffice)))))
  }

  def withoutOfficeOfExit(): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(officeOfExit = None))

  def withOfficeOfExit(officeId: Option[String] = None): ExportsDeclarationModifier =
    cache => cache.copy(locations = cache.locations.copy(officeOfExit = Some(OfficeOfExit(officeId))))

  def withoutItems(): ExportsDeclarationModifier = _.copy(items = Seq.empty)

  def withItem(item: ExportItem = ExportItem(uuid)): ExportsDeclarationModifier =
    m => m.copy(items = m.items :+ item)

  def withItems(item1: ExportItem, others: ExportItem*): ExportsDeclarationModifier =
    _.copy(items = Seq(item1) ++ others)

  private val itemModifiers = Seq(
    withProcedureCodes(Some("1041"), Seq("000")),
    withStatisticalValue(statisticalValue = "1000"),
    withCommodityDetails(CommodityDetails(combinedNomenclatureCode = Some("4602191000"), descriptionOfGoods = Some("Straw for bottles"))),
    withPackageInformation(Some("PK"), Some(10), Some("RICH123")),
    withCommodityMeasure(CommodityMeasure(Some("10"), Some(false), Some("500"), Some("700"))),
    withAdditionalInformation("00400", "EXPORTER"),
    withAdditionalDocuments(Some(YesNoAnswer.yes), AdditionalDocument(Some("C501"), Some("GBAEOC71757250450281"), None, None, None, None, None))
  )

  def withItems(count: Int): ExportsDeclarationModifier = {
    val items = (1 to count).map { idx =>
      itemModifiers.foldLeft(ExportItem(id = uuid, sequenceId = idx))((current, modifier) => modifier(current))
    }

    cache =>
      cache.copy(items = cache.items ++ items.toSet)
  }

  def withoutTotalNumberOfItems(): ExportsDeclarationModifier = _.copy(totalNumberOfItems = None)

  def withTotalNumberOfItems(
    totalAmountInvoiced: Option[String] = None,
    totalAmountInvoicedCurrency: Option[String] = None,
    agreedExchangeRate: Option[String] = None,
    exchangeRate: Option[String] = None,
    totalPackage: String = "1"
  ): ExportsDeclarationModifier =
    _.copy(
      totalNumberOfItems = Some(
        TotalNumberOfItems(
          totalAmountInvoiced,
          totalAmountInvoicedCurrency,
          agreedExchangeRate = agreedExchangeRate,
          exchangeRate,
          Some(totalPackage)
        )
      )
    )

  def withoutNatureOfTransaction(): ExportsDeclarationModifier = _.copy(natureOfTransaction = None)

  def withNatureOfTransaction(natureType: String): ExportsDeclarationModifier =
    _.copy(natureOfTransaction = Some(NatureOfTransaction(natureType)))

  val withoutBorderTransport: ExportsDeclarationModifier =
    declaration =>
      declaration.copy(
        transport = declaration.transport.copy(
          meansOfTransportCrossingTheBorderIDNumber = None,
          meansOfTransportCrossingTheBorderNationality = None,
          meansOfTransportCrossingTheBorderType = None
        )
    )

  def withBorderTransport(
    meansOfTransportCrossingTheBorderType: Option[String] = None,
    meansOfTransportCrossingTheBorderIDNumber: Option[String] = None
  ): ExportsDeclarationModifier =
    declaration =>
      declaration.copy(
        transport = declaration.transport.copy(
          meansOfTransportCrossingTheBorderNationality = None,
          meansOfTransportCrossingTheBorderType = meansOfTransportCrossingTheBorderType,
          meansOfTransportCrossingTheBorderIDNumber = meansOfTransportCrossingTheBorderIDNumber
        )
    )

  val withoutTransportCountry: ExportsDeclarationModifier =
    declaration => declaration.copy(transport = declaration.transport.copy(transportCrossingTheBorderNationality = None))

  def withTransportCountry(transportCountry: Option[String]): ExportsDeclarationModifier =
    declaration =>
      declaration.copy(transport = declaration.transport.copy(transportCrossingTheBorderNationality = Some(TransportCountry(transportCountry))))

  def withTransportPayment(payment: String): ExportsDeclarationModifier =
    declaration =>
      declaration.copy(
        transport = declaration.transport
          .copy(expressConsignment = Some(YesNoAnswer.yes), transportPayment = Some(TransportPayment(payment)))
    )

  def withUpdateDate(year: Int, month: Int, dayOfMonth: Int): ExportsDeclarationModifier =
    _.copy(updatedDateTime = ZonedDateTime.of(year, month, dayOfMonth, 10, 0, 0, 0, ZoneOffset.UTC).toInstant)

  def withMUCR(mucr: String): ExportsDeclarationModifier =
    cache => cache.copy(linkDucrToMucr = Some(YesNoAnswer.yes), mucr = Some(MUCR(mucr)))

  def withReadyForSubmission(): ExportsDeclarationModifier =
    declaration => declaration.copy(readyForSubmission = Some(true))

  def withUpdatedDateTime(updatedDateTime: Instant = Instant.now()): ExportsDeclarationModifier =
    declaration => declaration.copy(updatedDateTime = updatedDateTime)
}
