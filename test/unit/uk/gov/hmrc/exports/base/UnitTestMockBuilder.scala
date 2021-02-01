/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.base

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.{NotificationService, SubmissionService}

import scala.concurrent.Future

object UnitTestMockBuilder extends MockitoSugar {

  def buildCustomsDeclarationsConnectorMock: CustomsDeclarationsConnector = {
    val customsDeclarationsConnectorMock: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
    when(customsDeclarationsConnectorMock.submitDeclaration(any(), any())(any()))
      .thenReturn(Future.successful("conversation-id"))
    when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any()))
      .thenReturn(Future.successful("conversation-id"))
    customsDeclarationsConnectorMock
  }

  def buildExportsMetricsMock: ExportsMetrics = {
    val exportsMetricsMock: ExportsMetrics = mock[ExportsMetrics]
    when(exportsMetricsMock.startTimer(any())).thenCallRealMethod()
    exportsMetricsMock
  }

  def buildSubmissionRepositoryMock: SubmissionRepository = {
    val submissionRepositoryMock: SubmissionRepository = mock[SubmissionRepository]
    when(submissionRepositoryMock.findAllSubmissionsForEori(any())).thenReturn(Future.successful(Seq.empty))
    when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(None))
    when(submissionRepositoryMock.findSubmissionByUuid(any(), any())).thenReturn(Future.successful(None))
    when(submissionRepositoryMock.save(any())).thenAnswer(withFutureArg(0))
    when(submissionRepositoryMock.updateMrn(any(), any())).thenReturn(Future.successful(None))
    when(submissionRepositoryMock.addAction(any[String](), any())).thenReturn(Future.successful(None))
    when(submissionRepositoryMock.addAction(any[Submission](), any())).thenReturn(Future.successful(mock[Submission]))
    submissionRepositoryMock
  }

  def buildSubmissionServiceMock: SubmissionService = {
    val submissionServiceMock: SubmissionService = mock[SubmissionService]
    when(submissionServiceMock.getAllSubmissionsForUser(any())).thenReturn(Future.successful(Seq.empty))
    when(submissionServiceMock.getSubmission(any(), any())).thenReturn(Future.successful(None))
    submissionServiceMock
  }

  def buildNotificationRepositoryMock: NotificationRepository = {
    val notificationRepositoryMock: NotificationRepository = mock[NotificationRepository]
    when(notificationRepositoryMock.findNotificationsByActionId(any())).thenReturn(Future.successful(Seq.empty))
    when(notificationRepositoryMock.findNotificationsByActionIds(any())).thenReturn(Future.successful(Seq.empty))
    when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(false))
    notificationRepositoryMock
  }

  def buildNotificationServiceMock: NotificationService = {
    val notificationServiceMock: NotificationService = mock[NotificationService]
    when(notificationServiceMock.getAllNotificationsForUser(any())).thenReturn(Future.successful(Seq.empty))
    when(notificationServiceMock.getNotifications(any())).thenReturn(Future.successful(Seq.empty))
    when(notificationServiceMock.save(any())).thenReturn(Future.successful(Left("")))
    notificationServiceMock
  }

  private def withFutureArg[T](index: Int): Answer[Future[T]] = new Answer[Future[T]] {
    override def answer(invocation: InvocationOnMock): Future[T] = Future.successful(invocation.getArgument(index))
  }
}
