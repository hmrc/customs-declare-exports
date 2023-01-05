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

package testdata

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq}

object ReverseMappingTestData {

  def inputXmlMetaData(declarationXmlContent: NodeSeq): Elem =
    <MetaData xmlns:ns3="urn:wco:datamodel:WCO:DEC-DMS:2"
              xmlns:ns2="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
              xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      <ns3:WCODataModelVersionCode>3.6</ns3:WCODataModelVersionCode>
      <ns3:WCOTypeName>DEC</ns3:WCOTypeName>
      <ns3:ResponsibleCountryCode>GB</ns3:ResponsibleCountryCode>
      <ns3:ResponsibleAgencyName>HMRC</ns3:ResponsibleAgencyName>
      <ns3:AgencyAssignedCustomizationCode>v2.1</ns3:AgencyAssignedCustomizationCode>
      {addNamespace(declarationXmlContent)}
    </MetaData>

  def addNamespace(xml: NodeSeq): NodeSeq = {
    val ruleTransformer = new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case elem: Elem if elem.prefix != "ns3" => elem.copy(prefix = "ns3")
        case other                              => other
      }
    })

    xml.map(ruleTransformer)
  }

}
