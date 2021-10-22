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
import org.mockito.MockitoSugar
import reactivemongo.api.commands.WriteResult
import testdata.RepositoryTestData.dummyWriteResultFailure
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationService

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
    when(submissionRepositoryMock.addAction(any[String](), any())).thenReturn(Future.successful(None))
    when(submissionRepositoryMock.addAction(any[Submission](), any())).thenReturn(Future.successful(mock[Submission]))
    when(submissionRepositoryMock.findBy(any(), any())).thenReturn(Future.successful(Seq.empty))
    when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(None))
    when(submissionRepositoryMock.save(any())).thenReturn(Future.successful(mock[Submission]))
    when(submissionRepositoryMock.updateMrn(any(), any())).thenReturn(Future.successful(None))
    submissionRepositoryMock
  }

  def buildNotificationRepositoryMock: ParsedNotificationRepository = {
    val notificationRepositoryMock: ParsedNotificationRepository = mock[ParsedNotificationRepository]
    when(notificationRepositoryMock.findNotificationsByActionId(any())).thenReturn(Future.successful(Seq.empty))
    when(notificationRepositoryMock.findNotificationsByActionIds(any())).thenReturn(Future.successful(Seq.empty))
    when(notificationRepositoryMock.insert(any())(any())).thenReturn(Future.failed[WriteResult](dummyWriteResultFailure()))
    notificationRepositoryMock
  }

  def buildNotificationServiceMock: NotificationService = {
    val notificationServiceMock: NotificationService = mock[NotificationService]
    when(notificationServiceMock.findAllNotificationsForUser(any())).thenReturn(Future.successful(Seq.empty))
    when(notificationServiceMock.findAllNotifications(any())).thenReturn(Future.successful(Seq.empty))
    notificationServiceMock
  }

}
