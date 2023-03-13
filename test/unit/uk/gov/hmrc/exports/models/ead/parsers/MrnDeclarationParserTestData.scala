/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.xml.Elem

object MrnDeclarationParserTestData {

  def mrnDeclarationTestSample(mrn: String, version: Option[Int]): Elem =
    <p:DeclarationFullResponse xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService"
                               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                               xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
                               xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
                               xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
                               xmlns:p="http://gov.uk/customs/FullDeclarationDataRetrievalService">
      <p:FullDeclarationDataDetails>
        <p:HighLevelSummaryDetails>
          <p:MRN>{mrn}</p:MRN>
          <p:VersionID>{version getOrElse 2}</p:VersionID>
        </p:HighLevelSummaryDetails>
      </p:FullDeclarationDataDetails>
    </p:DeclarationFullResponse>
}
