package integration.uk.gov.hmrc.exports.services

import com.google.inject.AbstractModule
import integration.uk.gov.hmrc.exports.base.{IntegrationTestModule, IntegrationTestSpec}
import integration.uk.gov.hmrc.exports.stubs.CustomsDeclarationsAPIService
import integration.uk.gov.hmrc.exports.util.{CustomsDeclarationsAPIConfig, TestModule}
import integration.uk.gov.hmrc.exports.util.ExternalServicesConfig.{AuthToken, Host, Port}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.inject._
import play.api.mvc.Result
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.repositories.{NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.ExportsService
import uk.gov.hmrc.http.HeaderCarrier
import util.ExportsTestData
import play.api.test.Helpers._

import scala.xml.XML

class ExportsServiceSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar with CustomsDeclarationsAPIService
  with ExportsTestData {

  val mockSubmissionRepository: SubmissionRepository = mock[SubmissionRepository]

  def overrideModules: Seq[GuiceableModule] = Nil


  override implicit lazy val app: Application =
    GuiceApplicationBuilder().overrides(overrideModules: _*)
      .overrides(
      bind[SubmissionRepository].to(mockSubmissionRepository)
    ).configure(
      Map(
        "microservice.services.customs-declarations.host" -> Host,
        "microservice.services.customs-declarations.port" -> Port,
        "microservice.services.customs-declarations.submit-uri" -> CustomsDeclarationsAPIConfig.submitDeclarationServiceContext,
        "microservice.services.customs-declarations.bearer-token" -> AuthToken
      )
    )
      .build()

  private lazy val exportsService = app.injector.instanceOf[ExportsService]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def afterEach(): Unit =
    resetMockServer()

  override protected def afterAll() {
    stopMockServer()
  }



  "ExportService" should {
    "blah blah" in{
      val payload = randomSubmitDeclaration
      startSubmissionService(ACCEPTED)

      val result: Result = await(exportsService.handleSubmission(declarantEoriValue, Some(declarantDucrValue), declarantLrnValue, XML.loadString(payload.toXml)))

      contentAsString(result) should be("")
    }
  }

}
