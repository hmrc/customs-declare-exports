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

package uk.gov.hmrc.exports.models.ead.parsers

import java.time.ZonedDateTime

import uk.gov.hmrc.exports.models.ead.XmlTags._
import uk.gov.hmrc.exports.models.ead.{MrnStatus, PreviousDocument, XmlTags}

import scala.xml.NodeSeq

class MrnStatusParser {

  def getDucr(documents: NodeSeq): Option[String] =
    documents
      .find(doc => (doc \ XmlTags.typeCode).text == "DCR")
      .map(doc => (doc \ XmlTags.id).text)

  def parse(responseXml: NodeSeq): MrnStatus = {
    val mainUcr = getDucr(((responseXml \ declarationStatusDetails \ declaration)(1) \ goodsShipment) \ previousDocument)
    MrnStatus(
      mrn = (responseXml \ XmlTags.declarationStatusDetails \ declaration \ id).text,
      versionId = (responseXml \ declarationStatusDetails \ declaration \ versionId).text,
      eori = (responseXml \ declarationStatusDetails \ declaration \ submitter \ id).text,
      declarationType = ((responseXml \ declarationStatusDetails \ declaration)(1) \ typeCode).text,
      ucr = mainUcr,
      receivedDateTime = DateParser.zonedDateTime((responseXml \ declarationStatusDetails \ declaration \ receivedDateTime \ dateTimeString).text),
      releasedDateTime =
        DateParser.optionZonedDateTime((responseXml \ declarationStatusDetails \ declaration \ goodsReleasedDateTime \ dateTimeString).text),
      acceptanceDateTime =
        DateParser.optionZonedDateTime((responseXml \ declarationStatusDetails \ declaration \ acceptanceDateTime \ dateTimeString).text),
      createdDateTime = ZonedDateTime.now(),
      roe = (responseXml \ declarationStatusDetails \ declaration \ roe).text,
      ics = (responseXml \ declarationStatusDetails \ declaration \ ics).text,
      irc = StringOption((responseXml \ declarationStatusDetails \ declaration \ irc).text),
      totalPackageQuantity = ((responseXml \ declarationStatusDetails \ declaration)(1) \ totalPackageQuantity).text,
      goodsItemQuantity = ((responseXml \ declarationStatusDetails \ declaration)(1) \ goodsItemQuantity).text,
      previousDocuments = previousDocuments(((responseXml \ declarationStatusDetails \ declaration)(1) \ goodsShipment) \ previousDocument, mainUcr)
    )
  }

  private def previousDocuments(documents: NodeSeq, mainUcr: Option[String]): Seq[PreviousDocument] =
    documents
      .filter(doc => (doc \ XmlTags.id).text != mainUcr.getOrElse())
      .map { doc =>
        val id = (doc \ XmlTags.id).text
        val typeCode = (doc \ XmlTags.typeCode).text
        PreviousDocument(id, typeCode)
      }

}
