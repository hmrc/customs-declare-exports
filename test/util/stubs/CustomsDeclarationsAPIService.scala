package util.stubs

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import integration.uk.gov.hmrc.exports.base.WireMockRunner
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.Helpers.{ACCEPT, ACCEPTED, CONTENT_TYPE}
import uk.gov.hmrc.exports.controllers.CustomsHeaderNames
import util.{CustomsDeclarationsAPIConfig, ExportsTestData}

trait CustomsDeclarationsAPIService extends WireMockRunner with ExportsTestData{

  private val submissionURL = urlMatching(CustomsDeclarationsAPIConfig.submitDeclarationServiceContext)

  def startSubmissionService(status: Int = ACCEPTED): Unit = startService(status, submissionURL)

  private def startService(status: Int, url: UrlPattern) =
    stubFor(
      post(url).willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("X-Conversation-ID", UUID.randomUUID().toString)
      )
    )

  def verifyDecServiceWasCalledCorrectly(
                                          requestBody: String,
                                          expectedAuthToken: String = authToken,
                                          expectedEori: String,
                                          expectedApiVersion: String
  ) {

    verifyDecServiceWasCalledWith(
      CustomsDeclarationsAPIConfig.submitDeclarationServiceContext,
      requestBody,
      expectedAuthToken,
      expectedEori,
      expectedApiVersion
    )
  }

  private def verifyDecServiceWasCalledWith(
    requestPath: String,
    requestBody: String,
    expectedAuthToken: String,
    expectedEori: String,
    expectedVersion: String
  ) {
    verify(
      1,
      postRequestedFor(urlMatching(requestPath))
        .withHeader(CONTENT_TYPE, equalTo(ContentTypes.XML(Codec.utf_8)))
        .withHeader(ACCEPT, equalTo(s"application/vnd.hmrc.$expectedVersion+xml"))
        .withHeader(CustomsHeaderNames.XEoriIdentifierHeaderName, equalTo(expectedEori))
        .withRequestBody(equalToXml(requestBody))
    )
  }
}
