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
import util.ExternalServicesConfig.{Host, Port}
import integration.uk.gov.hmrc.exports.util.TestModule
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.CustomsDeclarationsResponse
import uk.gov.hmrc.http.HeaderCarrier
import util.stubs.CustomsDeclarationsAPIService
import util.{CustomsDeclarationsAPIConfig, ExportsTestData}

import scala.concurrent.Future
import scala.xml.XML

class CustomsDeclarationsConnectorSpec
    extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar with CustomsDeclarationsAPIService
    with ExportsTestData {

  private lazy val connector = app.injector.instanceOf[CustomsDeclarationsConnector]
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule))
      .configure(
        Map(
          "microservice.services.customs-declarations.host" -> Host,
          "microservice.services.customs-declarations.port" -> Port,
          "microservice.services.customs-declarations.submit-uri" -> CustomsDeclarationsAPIConfig.submitDeclarationServiceContext,
          "microservice.services.customs-declarations.bearer-token" -> authToken,
          "microservice.services.customs-declarations.api-version" -> CustomsDeclarationsAPIConfig.apiVersion
        )
      )
      .build()

  // TODO: added couple of tests for having convId or not, seems like there is logic which even if we got 202 but not convId
  // TODO: it will be wrapped into 500 (to be confirmed)
  // TODO: do we handle cancellations now ? as if so we would need to add tests here
  "Customs Declarations Connector" should {

    "return response with specific status" when {

      "it fail to connect to external service - 500" in {

        stopMockServer()

        val response = await(sendValidXml(randomSubmitDeclaration.toXml))
        response.status should be(INTERNAL_SERVER_ERROR)

        startMockServer()
      }

      "request is processed successfully - 202" in {

        val payload = randomSubmitDeclaration

        startSubmissionService(ACCEPTED)
        await(sendValidXml(payload.toXml))

        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(payload.declaration.get.functionalReferenceId.get),
          expectedEori = declarantEoriValue
        )
      }

      "request is processed successfully (external 202), but does not have conversationId - 500" in {

        startSubmissionService(ACCEPTED, conversationId = false)
        val response = await(sendValidXml(randomSubmitDeclaration.toXml))

        response.status should be(INTERNAL_SERVER_ERROR)
        response.conversationId should be(None)
      }

      "request is not processed - 500" in {

        startSubmissionService(INTERNAL_SERVER_ERROR)
        val response = await(sendValidXml(randomSubmitDeclaration.toXml))

        response.status should be(INTERNAL_SERVER_ERROR)
      }

      "request is not processed (external 401) - 500" in {

        startSubmissionService(UNAUTHORIZED)
        val response = await(sendValidXml(randomSubmitDeclaration.toXml))

        response.status should be(INTERNAL_SERVER_ERROR)
      }

      "request is not processed (external 404) - 500" in {

        startSubmissionService(NOT_FOUND)
        val response = await(sendValidXml(randomSubmitDeclaration.toXml))

        response.status should be(INTERNAL_SERVER_ERROR)
      }

      "request is not processed (external 400) and does not have conversationId - 500" in {

        startSubmissionService(BAD_REQUEST, conversationId = false)
        val response = await(sendValidXml(randomSubmitDeclaration.toXml))

        response.status should be(INTERNAL_SERVER_ERROR)
        response.conversationId should be(Some("No conversation ID found"))
      }

      "request is not processed (external 400) and does have conversationId - 500" in {

        startSubmissionService(BAD_REQUEST)
        val response = await(sendValidXml("<xml><element>test</element></xml>"))

        response.status should be(INTERNAL_SERVER_ERROR)
        response.conversationId should not be Some("No conversation ID found")
      }
    }
  }

  private def sendValidXml(xml: String): Future[CustomsDeclarationsResponse] =
    connector.submitDeclaration(declarantEoriValue, XML.loadString(xml))


}
