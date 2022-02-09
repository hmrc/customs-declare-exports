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

package uk.gov.hmrc.exports.services.mapping.declaration

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.{ExportsDeclaration, Transport}
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.declaration_ds.dms._2._

class BorderTransportMeansBuilder @Inject()(countriesService: CountriesService) extends ModifyingBuilder[ExportsDeclaration, Declaration] {
  override def buildThenAdd(model: ExportsDeclaration, t: Declaration): Unit = {
    val transport = model.transport

    val hasTransportLeavingTheBorder = transport.hasTransportLeavingTheBorder
    val hasBorder = transport.hasBorderTransportDetails
    val hasDepartureTransport = transport.hasDepartureTransportDetails

    if (hasTransportLeavingTheBorder || hasBorder || hasDepartureTransport) {
      val transportMeans = new Declaration.BorderTransportMeans

      if (hasTransportLeavingTheBorder) appendTransportLeavingTheBorder(transport, transportMeans)

      if (hasBorder) appendBorderTransport(transport, transportMeans)
      else if (hasDepartureTransport) appendBorderTransportWithDepartureValue(transport, transportMeans)

      t.setBorderTransportMeans(transportMeans)
    }
  }

  private def appendBorderTransport(data: Transport, transportMeans: Declaration.BorderTransportMeans): Unit = {
    data.meansOfTransportCrossingTheBorderIDNumber.foreach { value =>
      val id = new BorderTransportMeansIdentificationIDType
      id.setValue(value)
      transportMeans.setID(id)
    }

    data.meansOfTransportCrossingTheBorderType.foreach { transportType =>
      val identificationTypeCode = new BorderTransportMeansIdentificationTypeCodeType
      identificationTypeCode.setValue(transportType)
      transportMeans.setIdentificationTypeCode(identificationTypeCode)
    }

    if (data.meansOfTransportCrossingTheBorderNationality.isDefined) {
      val registrationNationalityCode = new BorderTransportMeansRegistrationNationalityCodeType
      registrationNationalityCode.setValue(
        countriesService.allCountries
          .find(country => data.meansOfTransportCrossingTheBorderNationality.contains(country.countryName))
          .map(_.countryCode)
          .getOrElse("")
      )
      transportMeans.setRegistrationNationalityCode(registrationNationalityCode)
    }
  }

  private def appendBorderTransportWithDepartureValue(data: Transport, transportMeans: Declaration.BorderTransportMeans): Unit = {
    data.meansOfTransportOnDepartureIDNumber.foreach { value =>
      val id = new BorderTransportMeansIdentificationIDType
      id.setValue(value)
      transportMeans.setID(id)
    }

    data.meansOfTransportOnDepartureType.foreach { transportType =>
      val identificationTypeCode = new BorderTransportMeansIdentificationTypeCodeType
      identificationTypeCode.setValue(transportType)
      transportMeans.setIdentificationTypeCode(identificationTypeCode)
    }
  }

  private def appendTransportLeavingTheBorder(data: Transport, transportMeans: Declaration.BorderTransportMeans): Unit =
    for {
      transportLeavingTheBorder <- data.borderModeOfTransportCode
      modeOfTransportCode <- transportLeavingTheBorder.code
      if modeOfTransportCode.isValidCode
    } {
      val modeCode = new BorderTransportMeansModeCodeType
      modeCode.setValue(modeOfTransportCode.value)
      transportMeans.setModeCode(modeCode)
    }

}
