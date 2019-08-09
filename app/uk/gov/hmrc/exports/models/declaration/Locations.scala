package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class DestinationCountries(
  countryOfDispatch: String,
  countriesOfRouting: Seq[String],
  countryOfDestination: String
)
object DestinationCountries {
  implicit val format: OFormat[DestinationCountries] = Json.format[DestinationCountries]
}

case class GoodsLocation(
  country: String,
  typeOfLocation: String,
  qualifierOfIdentification: String,
  identificationOfLocation: Option[String],
  additionalQualifier: Option[String],
  addressLine: Option[String],
  postCode: Option[String],
  city: Option[String]
)
object GoodsLocation {
  implicit val format: OFormat[GoodsLocation] = Json.format[GoodsLocation]
}

case class WarehouseIdentification(
  supervisingCustomsOffice: Option[String],
  identificationType: Option[String],
  identificationNumber: Option[String],
  inlandModeOfTransportCode: Option[String]
)
object WarehouseIdentification {
  implicit val format: OFormat[WarehouseIdentification] = Json.format[WarehouseIdentification]
}

case class OfficeOfExit(officeId: String, presentationOfficeId: Option[String], circumstancesCode: Option[String])
object OfficeOfExit {
  implicit val format: OFormat[OfficeOfExit] = Json.format[OfficeOfExit]
}

case class Locations(
  destinationCountries: Option[DestinationCountries] = None,
  goodsLocation: Option[GoodsLocation] = None,
  warehouseIdentification: Option[WarehouseIdentification] = None,
  officeOfExit: Option[OfficeOfExit] = None
)
object Locations {
  implicit val format: OFormat[Locations] = Json.format[Locations]
}
