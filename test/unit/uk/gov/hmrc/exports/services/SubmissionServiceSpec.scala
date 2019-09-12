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

package unit.uk.gov.hmrc.exports.services

import java.time.LocalDateTime

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.{Eori, LocalReferenceNumber, SubmissionRequestHeaders}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.MetaDataBuilder
import uk.gov.hmrc.exports.services.{SubmissionService, WcoMapperService}
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.exports.base.UnitTestMockBuilder.{buildCustomsDeclarationsConnectorMock, buildNotificationRepositoryMock, buildSubmissionRepositoryMock}
import util.testdata.ExportsTestData._
import util.testdata.SubmissionTestData._
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class SubmissionServiceSpec extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers {

  private trait Test {
    implicit val hc: HeaderCarrier = mock[HeaderCarrier]
    val customsDeclarationsConnectorMock: CustomsDeclarationsConnector = buildCustomsDeclarationsConnectorMock
    val submissionRepositoryMock: SubmissionRepository = buildSubmissionRepositoryMock
    val notificationRepositoryMock: NotificationRepository = buildNotificationRepositoryMock
    val metaDataBuilder: MetaDataBuilder = mock[MetaDataBuilder]
    val wcoMapperService: WcoMapperService = mock[WcoMapperService]
    val submissionService = new SubmissionService(
      customsDeclarationsConnector = customsDeclarationsConnectorMock,
      submissionRepository = submissionRepositoryMock,
      notificationRepository = notificationRepositoryMock,
      metaDataBuilder = metaDataBuilder,
      wcoMapperService = wcoMapperService
    )(ExecutionContext.global)
  }

  "cancel" should {
    val submission = Submission("id", "eori", "lrn", None, "ducr")
    val submissionCancelled = Submission("id", "eori", "lrn", None, "ducr", Seq(Action(CancellationRequest, "conv-id")))
    val cancellation = SubmissionCancellation("ref-id", "mrn", "description", "reason")

    "submit and delegate to repository" when {
      "submission exists" in new Test {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepositoryMock.addAction(any(), any())).thenReturn(Future.successful(Some(submission)))

        submissionService.cancel("eori", cancellation).futureValue mustBe CancellationRequested
      }

      "submission is missing" in new Test {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(None))

        submissionService.cancel("eori", cancellation).futureValue mustBe MissingDeclaration
      }

      "submission exists and previously cancelled" in new Test {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnectorMock.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepositoryMock.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submissionCancelled)))

        submissionService.cancel("eori", cancellation).futureValue mustBe CancellationRequestExists
      }
    }
  }

  "create" should {
    "delegate to the repository" when {
      "no notifications" in new Test {
        val submission: Submission =
          Submission(eori = "", lrn = "", ducr = "", actions = Seq(Action(SubmissionRequest, "conversation-id")))

        submissionService.create(submission).futureValue mustBe submission
      }

      "some notifications" in new Test {
        val submission: Submission =
          Submission(eori = "", lrn = "", ducr = "", actions = Seq(Action(SubmissionRequest, "conversation-id")))
        val notification = Notification("conversation-id", "mrn", LocalDateTime.now(), "", None, Seq.empty, "")
        when(notificationRepositoryMock.findNotificationsByConversationId("conversation-id"))
          .thenReturn(Future.successful(Seq(notification)))
        when(submissionRepositoryMock.updateMrn("conversation-id", "mrn"))
          .thenReturn(Future.successful(Some(submission)))

        submissionService.create(submission).futureValue mustBe submission

        verify(submissionRepositoryMock).updateMrn("conversation-id", "mrn")
      }
    }
  }

  "SubmissionService on getAllSubmissionsForUser" should {

    "return empty list if SubmissionRepository does not find any Submission" in new Test {
      submissionService.getAllSubmissionsForUser(eori).futureValue must equal(Seq.empty)
    }

    "return all submissions returned by SubmissionRepository" in new Test {
      when(submissionRepositoryMock.findAllSubmissionsForEori(meq(eori)))
        .thenReturn(Future.successful(Seq(submission, submission_2)))

      val returnedSubmissions = submissionService.getAllSubmissionsForUser(eori).futureValue

      returnedSubmissions.size must equal(2)
      returnedSubmissions must contain(submission)
      returnedSubmissions must contain(submission_2)
    }

    "call SubmissionRepository.findAllSubmissionsForUser, passing EORI provided" in new Test {
      submissionService.getAllSubmissionsForUser(eori).futureValue

      verify(submissionRepositoryMock, times(1)).findAllSubmissionsForEori(meq(eori))
    }
  }

  "SubmissionService on getSubmission" should {

    "return empty Option if SubmissionRepository does not find any Submission" in new Test {
      submissionService.getSubmission(eori, uuid).futureValue must equal(None)
    }

    "return submission returned by SubmissionRepository" in new Test {
      when(submissionRepositoryMock.findSubmissionByUuid(meq(eori), meq(uuid)))
        .thenReturn(Future.successful(Some(submission)))

      val returnedSubmission = submissionService.getSubmission(eori, uuid).futureValue

      returnedSubmission must be(defined)
      returnedSubmission.get must equal(submission)
    }

    "call SubmissionRepository.findSubmissionByUuid, passing UUID provided" in new Test {
      submissionService.getSubmission(eori, uuid).futureValue

      verify(submissionRepositoryMock, times(1)).findSubmissionByUuid(meq(eori), meq(uuid))
    }
  }
}

object SubmissionServiceSpec {
  val validatedHeadersSubmissionRequest: SubmissionRequestHeaders =
    SubmissionRequestHeaders(LocalReferenceNumber(lrn), Some(ducr))

  val submissionXml: NodeSeq = <submissionXml></submissionXml>
  val cancellationXml: NodeSeq = <cancellationXml></cancellationXml>
}
