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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.models.{LocalReferenceNumber, SubmissionRequestHeaders}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.MetaDataBuilder
import uk.gov.hmrc.exports.services.{SubmissionService, WcoMapperService}
import uk.gov.hmrc.http.HeaderCarrier
import util.testdata.ExportsDeclarationBuilder
import util.testdata.ExportsTestData._
import util.testdata.SubmissionTestData._
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class SubmissionServiceSpec
    extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers with ExportsDeclarationBuilder
    with Eventually with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  private val customsDeclarationsConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
  private val submissionRepository: SubmissionRepository = mock[SubmissionRepository]
  private val declarationRepository: DeclarationRepository = mock[DeclarationRepository]
  private val notificationRepository: NotificationRepository = mock[NotificationRepository]
  private val metaDataBuilder: MetaDataBuilder = mock[MetaDataBuilder]
  private val wcoMapperService: WcoMapperService = mock[WcoMapperService]
  private val submissionService = new SubmissionService(
    customsDeclarationsConnector = customsDeclarationsConnector,
    submissionRepository = submissionRepository,
    declarationRepository = declarationRepository,
    notificationRepository = notificationRepository,
    metaDataBuilder = metaDataBuilder,
    wcoMapperService = wcoMapperService
  )(ExecutionContext.global)

  override def afterEach(): Unit = {
    reset(
      customsDeclarationsConnector,
      submissionRepository,
      declarationRepository,
      notificationRepository,
      metaDataBuilder,
      wcoMapperService
    )
    super.afterEach()
  }

  "cancel" should {
    val submission = Submission("id", "eori", "lrn", None, "ducr")
    val submissionCancelled = Submission("id", "eori", "lrn", None, "ducr", Seq(Action("conv-id", CancellationRequest)))
    val cancellation = SubmissionCancellation("ref-id", "mrn", "description", "reason")

    "submit and delegate to repository" when {
      "submission exists" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any()))
          .thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findSubmissionByMrn(any())).thenReturn(Future.successful(Some(submission)))
        when(submissionRepository.addAction(any[String](), any())).thenReturn(Future.successful(Some(submission)))

        submissionService.cancel("eori", cancellation).futureValue mustBe CancellationRequested
      }

      "submission is missing" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any()))
          .thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findSubmissionByMrn(any())).thenReturn(Future.successful(None))

        submissionService.cancel("eori", cancellation).futureValue mustBe MissingDeclaration
      }

      "submission exists and previously cancelled" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any()))
          .thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findSubmissionByMrn(any()))
          .thenReturn(Future.successful(Some(submissionCancelled)))

        submissionService.cancel("eori", cancellation).futureValue mustBe CancellationRequestExists
      }
    }
  }

  "submit" should {
    def theActionAdded(): Action = {
      val captor: ArgumentCaptor[Action] = ArgumentCaptor.forClass(classOf[Action])
      verify(submissionRepository).addAction(any[Submission](), captor.capture())
      captor.getValue
    }

    def theDeclarationUpdated(index: Int = 0): ExportsDeclaration = {
      val captor: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
      verify(declarationRepository, atLeastOnce()).update(captor.capture())
      captor.getAllValues.get(index)
    }

    def theSubmissionCreated(): Submission = {
      val captor: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
      verify(submissionRepository).findOrCreate(any(), any(), captor.capture())
      captor.getValue
    }

    "submit to the Dec API" when {
      val declaration = aDeclaration()
      val notification = Notification("id", "mrn", LocalDateTime.now(), SubmissionStatus.ACCEPTED, Seq.empty, "xml")
      val submission = Submission(declaration, "lrn", "mrn")

      "valid declaration" in {
        // Given
        when(wcoMapperService.produceMetaData(any())).thenReturn(mock[MetaData])
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(declarationRepository.update(any())).thenReturn(Future.successful(Some(mock[ExportsDeclaration])))
        when(submissionRepository.findOrCreate(any(), any(), any())).thenReturn(Future.successful(mock[Submission]))
        when(customsDeclarationsConnector.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful("conv-id"))
        when(submissionRepository.addAction(any[Submission](), any())).thenReturn(Future.successful(submission))
        when(notificationRepository.findNotificationsByActionId(anyString())).thenReturn(Future.successful(Seq.empty))

        // When
        submissionService.submit(declaration).futureValue mustBe submission

        // Then
        theSubmissionCreated() mustBe Submission(declaration, "lrn", "ducr")

        val action = theActionAdded()
        action.id mustBe "conv-id"
        action.requestType mustBe SubmissionRequest

        theDeclarationUpdated().status mustEqual DeclarationStatus.COMPLETE
      }

      "existing notification is available" in {
        // Given
        when(wcoMapperService.produceMetaData(any())).thenReturn(mock[MetaData])
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(declarationRepository.update(any())).thenReturn(Future.successful(Some(mock[ExportsDeclaration])))
        when(submissionRepository.findOrCreate(any(), any(), any())).thenReturn(Future.successful(mock[Submission]))
        when(customsDeclarationsConnector.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.successful("conv-id"))
        when(submissionRepository.addAction(any[Submission](), any())).thenReturn(Future.successful(mock[Submission]))
        when(notificationRepository.findNotificationsByActionId(anyString()))
          .thenReturn(Future.successful(Seq(notification)))
        when(submissionRepository.updateMrn(any(), any())).thenReturn(Future.successful(Some(submission)))

        // When
        submissionService.submit(declaration).futureValue mustBe submission

        // Then
        theSubmissionCreated() mustBe Submission(declaration, "lrn", "ducr")

        val action = theActionAdded()
        action.id mustBe "conv-id"
        action.requestType mustBe SubmissionRequest

        theDeclarationUpdated().status mustEqual DeclarationStatus.COMPLETE
      }
    }

    "revert declaration to draft" when {
      "submission to the Dec API fails" in {
        // Given
        when(wcoMapperService.produceMetaData(any())).thenReturn(mock[MetaData])
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(declarationRepository.update(any())).thenReturn(Future.successful(Some(mock[ExportsDeclaration])))
        when(submissionRepository.findOrCreate(any(), any(), any())).thenReturn(Future.successful(mock[Submission]))
        when(customsDeclarationsConnector.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Some error")))

        val declaration = aDeclaration()

        // When
        intercept[RuntimeException] {
          submissionService.submit(declaration).futureValue
        }

        // Then
        theSubmissionCreated() mustBe Submission(declaration, "lrn", "ducr")

        verify(submissionRepository, never()).addAction(any[Submission], any[Action])

        theDeclarationUpdated(0).status mustEqual DeclarationStatus.COMPLETE
        theDeclarationUpdated(1).status mustEqual DeclarationStatus.DRAFT
      }
    }

    "throw exception" when {
      "missing LRN" in {
        when(wcoMapperService.produceMetaData(any())).thenReturn(mock[MetaData])
        when(wcoMapperService.declarationLrn(any())).thenReturn(None)
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))

        intercept[IllegalArgumentException] {
          submissionService.submit(aDeclaration()).futureValue
        }
      }

      "missing DUCR" in {
        when(wcoMapperService.produceMetaData(any())).thenReturn(mock[MetaData])
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(None)

        intercept[IllegalArgumentException] {
          submissionService.submit(aDeclaration()).futureValue
        }
      }
    }
  }

  "Get all Submissions for eori" should {
    "delegate to repository" in {
      val response = mock[Seq[Submission]]
      when(submissionRepository.findAllSubmissionsForEori(any())).thenReturn(Future.successful(response))

      submissionService.getAllSubmissionsForUser(eori).futureValue mustBe response

      verify(submissionRepository).findAllSubmissionsForEori(meq(eori))
    }
  }

  "Get Submission" should {
    "delegate to repository" in {
      val response = mock[Option[Submission]]
      when(submissionRepository.findSubmissionByUuid(any(), any())).thenReturn(Future.successful(response))

      submissionService.getSubmission(eori, uuid).futureValue mustBe response

      verify(submissionRepository).findSubmissionByUuid(meq(eori), meq(uuid))
    }
  }
}
