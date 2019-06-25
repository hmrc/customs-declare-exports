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

package component.uk.gov.hmrc.exports.base

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyZeroInteractions, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import util._
import util.stubs.CustomsDeclarationsAPIService
import util.testdata.ExportsTestData._

import scala.concurrent.Future

trait ComponentTestSpec
    extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach
    with Eventually with MockitoSugar with Matchers with AuditService with OptionValues
    with AuthService with CustomsDeclarationsAPIService {

  private val mockSubmissionRepository = mock[SubmissionRepository]
  private val mockNotificationsRepository = mock[NotificationRepository]

  override protected def beforeAll() {

    startMockServer()
  }

  override protected def beforeEach() {

    reset(mockSubmissionRepository)
    reset(mockNotificationsRepository)
    resetMockServer()
  }

  override protected def afterAll() {

    stopMockServer()
  }

  def withSubmissionRepository(saveResponse: Boolean): OngoingStubbing[Future[Boolean]] =
    when(mockSubmissionRepository.save(any())).thenReturn(Future.successful(saveResponse))

  def verifySubmissionRepositoryIsCorrectlyCalled(eoriValue: String) {
    val submissionCaptor: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
    verify(mockSubmissionRepository).save(submissionCaptor.capture())
    submissionCaptor.getValue.eori shouldBe eoriValue
  }

  def verifySubmissionRepositoryWasNotCalled(): Unit = {
    verifyZeroInteractions(mockSubmissionRepository)
  }

  val dateTime = 1546344000000L // 01/01/2019 12:00:00

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[SubmissionRepository].toInstance(mockSubmissionRepository))
    .overrides(bind[NotificationRepository].toInstance(mockNotificationsRepository))
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
