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

import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration._

//noinspection ScalaStyle
trait ExportsDeclarationBuilder {

  protected val VALID_DUCR = "5GB123456789000-123ABC456DEFIIIII"
  protected val VALID_LRN = "FG7676767889"

  private def uuid: String = UUID.randomUUID().toString

  private val modelWithDefaults: ExportsDeclaration = ExportsDeclaration(
    id = uuid,
    eori = "eori",
    status = DeclarationStatus.COMPLETE,
    createdDateTime = LocalDateTime.of(2019, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC),
    updatedDateTime = LocalDateTime.of(2019, 2, 2, 0, 0, 0).toInstant(ZoneOffset.UTC),
    choice = "choice",
    dispatchLocation = None,
    additionalDeclarationType = None,
    consignmentReferences = None,
    borderTransport = None,
    transportDetails = None,
    containerData = None,
    parties = Parties()
  )

  private type ExportsDeclarationModifier = ExportsDeclaration => ExportsDeclaration

  def aDeclaration(modifiers: ExportsDeclarationModifier*): ExportsDeclaration =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  // ************************************************* Builders ********************************************************

  def withId(id: String): ExportsDeclarationModifier = _.copy(id = id)

  def withEori(eori: String): ExportsDeclarationModifier = _.copy(eori = eori)

  def withStatus(status: DeclarationStatus): ExportsDeclarationModifier = _.copy(status = status)

  def withChoice(choice: String): ExportsDeclarationModifier = _.copy(choice = choice)

  def withoutAdditionalDeclarationType(): ExportsDeclarationModifier = _.copy(additionalDeclarationType = None)

  def withAdditionalDeclarationType(decType: String = "dev-type"): ExportsDeclarationModifier =
    _.copy(additionalDeclarationType = Some(AdditionalDeclarationType(decType)))

  def withoutDispatchLocation: ExportsDeclarationModifier = _.copy(dispatchLocation = None)

  def withDispatchLocation(location: String = "GB"): ExportsDeclarationModifier =
    _.copy(dispatchLocation = Some(DispatchLocation(location)))

  def withoutConsignmentReferences(): ExportsDeclarationModifier = _.copy(consignmentReferences = None)

  def withConsignmentReferences(
    ducr: Option[String] = Some(VALID_DUCR),
    lrn: String = VALID_LRN
  ): ExportsDeclarationModifier =
    _.copy(consignmentReferences = Some(ConsignmentReferences(ducr.map(DUCR(_)), lrn)))

  def withoutBorderTransport(): ExportsDeclarationModifier = _.copy(borderTransport = None)

  def withBorderTransport(
    borderModeOfTransportCode: String = "",
    meansOfTransportOnDepartureType: String = "",
    meansOfTransportOnDepartureIDNumber: Option[String] = None
  ): ExportsDeclarationModifier =
    _.copy(
      borderTransport = Some(
        BorderTransport(borderModeOfTransportCode, meansOfTransportOnDepartureType, meansOfTransportOnDepartureIDNumber)
      )
    )

  def withoutContainerData(): ExportsDeclarationModifier = _.copy(containerData = None)

  def withContainerData(data: TransportInformationContainer*): ExportsDeclarationModifier =
    cache =>
      cache.copy(
        containerData =
          Some(TransportInformationContainers(cache.containerData.map(_.containers).getOrElse(Seq.empty) ++ data))
    )

  def withoutExporterDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(exporterDetails = None))

  def withExporterDetails(eori: Option[String] = None, address: Option[Address] = None): ExportsDeclarationModifier =
    cache =>
      cache.copy(parties = cache.parties.copy(exporterDetails = Some(ExporterDetails(EntityDetails(eori, address)))))

  def withoutConsigneeDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(consigneeDetails = None))

  def withConsigneeDetails(eori: Option[String], address: Option[Address]): ExportsDeclarationModifier =
    cache =>
      cache.copy(parties = cache.parties.copy(consigneeDetails = Some(ConsigneeDetails(EntityDetails(eori, address)))))

  def withoutDeclarantDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarantDetails = None))

  def withDeclarantDetails(eori: Option[String] = None, address: Option[Address] = None): ExportsDeclarationModifier =
    cache =>
      cache.copy(parties = cache.parties.copy(declarantDetails = Some(DeclarantDetails(EntityDetails(eori, address)))))

  def withoutRepresentativeDetails(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(representativeDetails = None))

  def withRepresentativeDetails(details: Option[EntityDetails], statusCode: Option[String]): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(representativeDetails = Some(RepresentativeDetails(details, statusCode))))

  def withDeclarationAdditionalActors(data: DeclarationAdditionalActor*): ExportsDeclarationModifier =
    cache =>
      cache.copy(
        parties = cache.parties.copy(declarationAdditionalActorsData = Some(DeclarationAdditionalActors(data)))
      )

  def withoutDeclarationHolders(): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarationHoldersData = None))

  def withDeclarationHolders(holders: DeclarationHolder*): ExportsDeclarationModifier =
    cache => cache.copy(parties = cache.parties.copy(declarationHoldersData = Some(DeclarationHolders(holders))))

  def withoutCarrierDetails(): ExportsDeclarationModifier = cache => cache.copy(parties = cache.parties.copy(carrierDetails = None))

  def withCarrierDetails(eori: Option[String] = None, address: Option[Address] = None): ExportsDeclarationModifier =
    cache =>
      cache.copy(parties = cache.parties.copy(carrierDetails = Some(CarrierDetails(EntityDetails(eori, address)))))


}
