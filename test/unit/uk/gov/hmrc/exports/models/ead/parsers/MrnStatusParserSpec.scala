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

import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class MrnStatusParserSpec extends WordSpec with MustMatchers with OptionValues {

  "MrnStatusParser" should {
    "create a MrnStatus instance once all data is provided" in {
      val mrnStatus = new MrnStatusParser().parse(MrnStatusParserTestData.mrnStatusWithAllData("20GB2A57QTFF8X8PA0"))
      mrnStatus.mrn mustBe "20GB2A57QTFF8X8PA0"
      mrnStatus.ucr mustBe Some("18GBAKZ81EQJ2FGVR")
      mrnStatus.eori mustBe "GB123456789012000"
      mrnStatus.versionId mustBe "1"
      mrnStatus.declarationType mustBe "IMZ"
      // 20190102110757Z
      mrnStatus.acceptanceDateTime mustBe Some(ZonedDateTime.of(2019, 1, 2, 11, 7, 57, 0, DateParser.zoneUTC))
      // 20190702110857Z
      mrnStatus.receivedDateTime mustBe ZonedDateTime.of(2019, 7, 2, 11, 8, 57, 0, DateParser.zoneUTC)
      // 20190702130957Z
      mrnStatus.releasedDateTime mustBe Some(ZonedDateTime.of(2019, 7, 2, 13, 9, 57, 0, DateParser.zoneUTC))
      mrnStatus.createdDateTime mustNot be(None)
      mrnStatus.roe mustBe "6"
      mrnStatus.ics mustBe "15"
      mrnStatus.irc mustBe Some("000")
      mrnStatus.goodsItemQuantity mustBe "100"
      mrnStatus.totalPackageQuantity mustBe "10"
      mrnStatus.previousDocuments.length mustBe 5
      mrnStatus.previousDocuments.head.id mustBe "18GBAKZ81EQJ2FGVA"
      mrnStatus.previousDocuments.head.typeCode mustBe "MCR"
      mrnStatus.previousDocuments(1).id mustBe "18GBAKZ81EQJ2FGVB"
      mrnStatus.previousDocuments(1).typeCode mustBe "MCR"
      mrnStatus.previousDocuments(2).id mustBe "18GBAKZ81EQJ2FGVC"
      mrnStatus.previousDocuments(2).typeCode mustBe "DCR"
      mrnStatus.previousDocuments(3).id mustBe "18GBAKZ81EQJ2FGVD"
      mrnStatus.previousDocuments(3).typeCode mustBe "MCR"
      mrnStatus.previousDocuments(4).id mustBe "18GBAKZ81EQJ2FGVE"
      mrnStatus.previousDocuments(4).typeCode mustBe "MCR"
    }

    "create a MrnStatus instance when partial data is provided" in {
      val mrnStatus = new MrnStatusParser().parse(MrnStatusParserTestData.mrnStatusWithSelectedFields("20GB2A57QTFF8X8PA0"))
      mrnStatus.mrn mustBe "20GB2A57QTFF8X8PA0"
      mrnStatus.ucr mustBe Some("8GB123456765080-101SHIP1")
      mrnStatus.eori mustBe "GB7172755049242"
      mrnStatus.versionId mustBe "1"
      mrnStatus.declarationType mustBe "EXD"
      mrnStatus.acceptanceDateTime mustBe None
      // 20200227114305Z
      mrnStatus.receivedDateTime mustBe ZonedDateTime.of(2020, 2, 27, 11, 43, 5, 0, DateParser.zoneUTC)
      mrnStatus.releasedDateTime mustBe None
      mrnStatus.createdDateTime mustNot be(None)
      mrnStatus.roe mustBe "H"
      mrnStatus.ics mustBe "14"
      mrnStatus.irc mustBe None
      mrnStatus.goodsItemQuantity mustBe "1"
      mrnStatus.totalPackageQuantity mustBe "1.0"
      mrnStatus.previousDocuments.length mustBe 0
    }

    "create a MrnStatus instance when partial data is provided with no previous documents" in {
      val mrnStatus = new MrnStatusParser().parse(MrnStatusParserTestData.mrnStatusWithNoPreviousDocuments("20GB2A57QTFF8X8PA0"))
      mrnStatus.mrn mustBe "20GB2A57QTFF8X8PA0"
      mrnStatus.ucr mustBe None
      mrnStatus.eori mustBe "GB7172755049242"
      mrnStatus.versionId mustBe "1"
      mrnStatus.declarationType mustBe "EXD"
      mrnStatus.acceptanceDateTime mustBe None
      // 20200227114305Z
      mrnStatus.receivedDateTime mustBe ZonedDateTime.of(2020, 2, 27, 11, 43, 5, 0, DateParser.zoneUTC)
      mrnStatus.releasedDateTime mustBe None
      mrnStatus.createdDateTime mustNot be(None)
      mrnStatus.roe mustBe "H"
      mrnStatus.ics mustBe "14"
      mrnStatus.irc mustBe None
      mrnStatus.goodsItemQuantity mustBe "1"
      mrnStatus.totalPackageQuantity mustBe "1.0"
      mrnStatus.previousDocuments.length mustBe 0
    }
  }
}
