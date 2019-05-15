package util

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.repositories.SubmissionRepository

import scala.concurrent.Future

trait ComponentTestSpec
    extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach
    with Eventually with MockitoSugar with ExportsTestData {

  private val mockSubmissionRepository = mock[SubmissionRepository]

  def withSubmissionRepository(saveResponse: Boolean): OngoingStubbing[Future[Boolean]] = {
    when(mockSubmissionRepository.save(any())).thenReturn(Future.successful(saveResponse))
  }

  val dateTime = 1546344000000L // 01/01/2019 12:00:00

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[SubmissionRepository].toInstance(mockSubmissionRepository))
    .configure(
      Map(
        "microservice.services.auth.host" -> ExternalServicesConfig.Host,
        "microservice.services.auth.port" -> ExternalServicesConfig.Port,
        "microservice.services.customs-declarations.host" -> ExternalServicesConfig.Host,
        "microservice.services.customs-declarations.port" -> ExternalServicesConfig.Port,
        "microservice.services.customs-declarations.submit-uri" -> CustomsDeclarationsAPIConfig.submitDeclarationServiceContext,
        "microservice.services.customs-declarations.bearer-token" -> dummyToken,
        "microservice.services.customs-declarations.api-version" -> CustomsDeclarationsAPIConfig.apiVersion,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> ExternalServicesConfig.Host,
        "auditing.consumer.baseUri.port" -> ExternalServicesConfig.Port
      )
    )
    .build()

}
