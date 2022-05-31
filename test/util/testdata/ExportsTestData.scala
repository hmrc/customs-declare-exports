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

package testdata

import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.{ContentTypes, HeaderNames}
import testdata.TestDataHelper.randomAlphanumericString
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames._
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.emails.Email
import uk.gov.hmrc.wco.dec.{MetaData, Declaration => WcoDeclaration}

object ExportsTestData {

  val eori: String = "GB167676"
  val ducr: String = randomAlphanumericString(16)
  val lrn: String = randomAlphanumericString(22)
  val mrn: String = "MRN87878797"
  val mrn_2: String = "MRN12341234"
  val actionId: String = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"
  val actionId_2: String = "b1c09f1b-7c94-4e90-b754-7c5c71c55e22"
  val actionId_3: String = "b1c09f1b-7c94-4e90-b754-7c5c71c55e33"
  val actionId_4: String = "b1c09f1b-7c94-4e90-b754-7c5c71c55e44"

  val authToken: String =
    "BXQ3/Treo4kQCZvVcCqKPlwxRN4RA9Mb5RF8fFxOuwG5WSg+S+Rsp9Nq998Fgg0HeNLXL7NGwEAIzwM6vuA6YYhRQnTRFaBhrp+1w+kVW8g1qHGLYO48QPWuxdM87VMCZqxnCuDoNxVn76vwfgtpNj0+NwfzXV2Zc12L2QGgF9H9KwIkeIPK/mMlBESjue4V]"
  val dummyToken: String = s"Bearer $authToken"
  val declarantEoriValue: String = "ZZ123456789000"
  val declarantEori: Eori = Eori(declarantEoriValue)
  val declarantLrnValue: String = "MyLrnValue1234"
  val declarantDucrValue: String = "MyDucrValue1234"
  val declarantMrnValue: String = "MyMucrValue1234"

  val contentTypeHeader: (String, String) = CONTENT_TYPE -> ContentTypes.JSON
  val Valid_X_EORI_IDENTIFIER_HEADER: (String, String) = XEoriIdentifierHeaderName -> declarantEoriValue
  val Valid_LRN_HEADER: (String, String) = XLrnHeaderName -> declarantLrnValue
  val Valid_AUTHORIZATION_HEADER: (String, String) = HeaderNames.AUTHORIZATION -> dummyToken
  val VALID_CONVERSATIONID_HEADER: (String, String) = XConversationIdName -> actionId
  val VALID_DUCR_HEADER: (String, String) = XDucrHeaderName -> declarantDucrValue
  val VALID_MRN_HEADER: (String, String) = XMrnHeaderName -> declarantMrnValue
  val eidrDateStamp = "20001231"

  val verifiedEmailAddress = Email("some@email.com", true)

  val ValidHeaders: Map[String, String] = Map(
    contentTypeHeader,
    Valid_AUTHORIZATION_HEADER,
    VALID_CONVERSATIONID_HEADER,
    Valid_X_EORI_IDENTIFIER_HEADER,
    Valid_LRN_HEADER,
    VALID_DUCR_HEADER,
    VALID_MRN_HEADER
  )

  def randomSubmitDeclaration: MetaData =
    MetaData(declaration = Option(WcoDeclaration(functionalReferenceId = Some(randomAlphanumericString(35)))))

  def generateSubmitDeclaration(lrnValue: String): MetaData =
    MetaData(declaration = Option(WcoDeclaration(functionalReferenceId = Some(lrnValue))))

  def expectedSubmissionRequestPayload(functionalReferenceId: String): String = {
    val returnXml =
      <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2"
              xmlns:ns2="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
              xmlns:ns3="urn:wco:datamodel:WCO:DEC-DMS:2">
      <WCODataModelVersionCode>3.6</WCODataModelVersionCode>
      <WCOTypeName>DEC</WCOTypeName>
      <ResponsibleCountryCode>GB</ResponsibleCountryCode>
      <ResponsibleAgencyName>HMRC</ResponsibleAgencyName>
      <AgencyAssignedCustomizationCode>v2.1</AgencyAssignedCustomizationCode>
      <ns3:Declaration>
        <ns3:FunctionCode>9</ns3:FunctionCode>
        <ns3:FunctionalReferenceID>{functionalReferenceId}</ns3:FunctionalReferenceID>
        <ns3:GoodsItemQuantity>0</ns3:GoodsItemQuantity>
        <ns3:Consignment/>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:TransportEquipment>
              <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
              <ns3:ID>container123</ns3:ID>
              <ns3:Seal>
                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
                <ns3:ID>seal1</ns3:ID>
              </ns3:Seal>
            </ns3:TransportEquipment>
          </ns3:Consignment>
          <ns3:PreviousDocument>
            <ns3:CategoryCode>Z</ns3:CategoryCode>
            <ns3:ID>5GB123456789000-123ABC456DEFIIIII</ns3:ID>
            <ns3:TypeCode>DCR</ns3:TypeCode>
            <ns3:LineNumeric>1</ns3:LineNumeric>
          </ns3:PreviousDocument>
          <ns3:UCR>
            <ns3:TraderAssignedReferenceID>5GB123456789000-123ABC456DEFIIIII</ns3:TraderAssignedReferenceID>
          </ns3:UCR>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </MetaData>
    returnXml.toString
  }
}
