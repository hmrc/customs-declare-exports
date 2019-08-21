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

import java.time.LocalDateTime

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyZeroInteractions, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.http.InternalServerException
import util._
import util.stubs.CustomsDeclarationsAPIService
import util.testdata.ExportsTestData._

import scala.concurrent.Future

trait ComponentTestSpec
    extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach
    with Eventually with MockitoSugar with Matchers with AuditService with OptionValues with AuthService
    with CustomsDeclarationsAPIService {

  private val mockSubmissionRepository = mock[SubmissionRepository]
  private val mockDeclarationRepository = mock[DeclarationRepository]
  private val mockNotificationsRepository = mock[NotificationRepository]

  override protected def beforeAll() {

    startMockServer()
  }

  override protected def beforeEach() {

    reset(mockSubmissionRepository)
    reset(mockDeclarationRepository)
    reset(mockNotificationsRepository)
    resetMockServer()
  }

  override protected def afterAll() {

    stopMockServer()
  }

  private def withFutureArg[T](index: Int): Answer[Future[T]] = new Answer[Future[T]] {
    override def answer(invocation: InvocationOnMock): Future[T] = Future.successful(invocation.getArgument(index))
  }

  def withSubmissionSuccess(): Unit = {
    when(mockSubmissionRepository.save(any())).thenAnswer(withFutureArg(0))
    when(mockSubmissionRepository.updateMrn(any(), any()))
    .thenReturn(Future.successful(Some(Submission(uuid = "uuid", eori = "eori-123", ducr = "ducr", lrn = "lrn"))))
  }

  def withSubmissionFailure(): Unit =
    when(mockSubmissionRepository.save(any())).thenThrow(new RuntimeException("Could not save to DB"))

  def withDeclarationRepositorySuccess(): Unit = {
    when(mockDeclarationRepository.create(any())).thenAnswer(withFutureArg(0))
  }

  def withDeclarationRepositoryFailure() =
    when(mockDeclarationRepository.create(any())).thenAnswer(new Answer[String] {
      def answer(invocation: InvocationOnMock): String =
        throw new InternalServerException("Could not save to DB")
    })

  def verifyDeclarationRepositoryIsCorrectlyCalled(eoriValue: String) {
    val submissionCaptor: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
    verify(mockDeclarationRepository).create(submissionCaptor.capture())
    submissionCaptor.getValue.eori shouldBe eoriValue
  }

  def withNotificationRepositorySuccess(): Unit =
    when(mockNotificationsRepository.findNotificationsByConversationId(any())).thenReturn(
      Future.successful(Seq(Notification("conversation-id", "mrn", LocalDateTime.now(), "", None, Seq.empty, "")))
    )

  def verifySubmissionRepositoryIsCorrectlyCalled(eoriValue: String) {
    val submissionCaptor: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
    verify(mockSubmissionRepository).save(submissionCaptor.capture())
    submissionCaptor.getValue.eori shouldBe eoriValue
  }

  def verifySubmissionRepositoryWasNotCalled(): Unit =
    verifyZeroInteractions(mockSubmissionRepository)

  val dateTime = 1546344000000L // 01/01/2019 12:00:00

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[SubmissionRepository].toInstance(mockSubmissionRepository))
    .overrides(bind[NotificationRepository].toInstance(mockNotificationsRepository))
    .overrides(bind[DeclarationRepository].toInstance(mockDeclarationRepository))
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
