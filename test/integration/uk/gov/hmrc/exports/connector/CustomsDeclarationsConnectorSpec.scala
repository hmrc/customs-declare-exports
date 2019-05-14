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

package integration.uk.gov.hmrc.exports.connector

import integration.uk.gov.hmrc.exports.base.IntegrationTestSpec
import integration.uk.gov.hmrc.exports.stubs.CustomsDeclarationsAPIService
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import integration.uk.gov.hmrc.exports.util.ExternalServicesConfig.{AuthToken, Host, Port}
import integration.uk.gov.hmrc.exports.util.{CustomsDeclarationsAPIConfig, TestModule}
import play.api.test.Helpers.{ACCEPTED, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, NotFoundException, Upstream5xxResponse}
import util.ExportsTestData

import scala.xml.XML

class CustomsDeclarationsConnectorSpec
    extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar with CustomsDeclarationsAPIService with ExportsTestData {

  private lazy val connector = app.injector.instanceOf[CustomsDeclarationsConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.customs-declarations.host" -> Host,
      "microservice.services.customs-declarations.port" -> Port,
      "microservice.services.customs-declarations.submit-uri" -> CustomsDeclarationsAPIConfig.submitDeclarationServiceContext,
      "microservice.services.customs-declarations.bearer-token" -> AuthToken
    )).build()

  "CustomsDeclarationsConnector" should {

    "make a correct request" in {
      val payload = randomSubmitDeclaration
      startSubmissionService(ACCEPTED)
      await(sendValidXml(payload.toXml))
      verifyDecServiceWasCalledCorrectly(requestBody = expectedSubmissionRequestPayload(payload.declaration.get.functionalReferenceId.get), expectedEori = declarantEoriValue)
    }

    "return a failure response with internal server error when external service returns 404" in {
      startSubmissionService(NOT_FOUND)
      val response = await(sendValidXml(randomSubmitDeclaration.toXml))
      response.status should be(INTERNAL_SERVER_ERROR)
    }

    "return a failed future when external service returns 500" in {
      startSubmissionService(INTERNAL_SERVER_ERROR)
      val response = await(sendValidXml(randomSubmitDeclaration.toXml))
      response.status should be(INTERNAL_SERVER_ERROR)
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      val response = await(sendValidXml(randomSubmitDeclaration.toXml))
      response.status should be(INTERNAL_SERVER_ERROR)
      startMockServer()
    }

  }

  private def sendValidXml(xml: String) = {
    connector.submitDeclaration(declarantEoriValue, XML.loadString(xml))
  }

  private def expectedSubmissionRequestPayload(functionalReferenceId: String) = {
    val returnXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
        <wstxns1:Declaration xmlns:wstxns1="urn:wco:datamodel:WCO:DEC-DMS:2">
           <wstxns1:FunctionalReferenceID>{functionalReferenceId}</wstxns1:FunctionalReferenceID>
        </wstxns1:Declaration>
      </MetaData>
    returnXml.toString
  }
}