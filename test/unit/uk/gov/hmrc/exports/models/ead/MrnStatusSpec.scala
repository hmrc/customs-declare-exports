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

package uk.gov.hmrc.exports.models.ead

import java.time.{LocalDateTime, ZonedDateTime}

import uk.gov.hmrc.exports.models.ead.parsers.DateParser.{formatter304, zoneUTC}

object MrnStatusSpec {
  val completeMrnStatus = MrnStatus(
    mrn = "18GB9JLC3CU1LFGVR2",
    versionId = "1",
    eori = "GB123456789012000",
    declarationType = "IMZ",
    ucr = Some("20GBAKZ81EQJ2WXYZ"),
    receivedDateTime = ZonedDateTime.of(LocalDateTime.parse("20190702110700Z", formatter304), zoneUTC),
    releasedDateTime = Some(ZonedDateTime.of(LocalDateTime.parse("20190702110700Z", formatter304), zoneUTC)),
    acceptanceDateTime = Some(ZonedDateTime.of(LocalDateTime.parse("20190702110700Z", formatter304), zoneUTC)),
    createdDateTime = ZonedDateTime.of(LocalDateTime.parse("20200310011300Z", formatter304), zoneUTC),
    roe = "6",
    ics = "15",
    irc = Some("000"),
    totalPackageQuantity = "10",
    goodsItemQuantity = "100",
    previousDocuments = Seq(
      PreviousDocument("18GBAKZ81EQJ2FGVR", "DCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVA", "MCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVB", "MCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVC", "DCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVD", "MCR"),
      PreviousDocument("18GBAKZ81EQJ2FGVE", "MCR")
    )
  )
}
