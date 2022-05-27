/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.JsValue
import uk.gov.hmrc.exports.base.{MockMetrics, UnitSpec}
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.CancellationMetaDataBuilder
import uk.gov.hmrc.exports.services.notifications.receiptactions.SendEmailForDmsDocAction
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import java.time.{ZoneOffset, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

class SubmissionServiceSpec extends UnitSpec with ExportsDeclarationBuilder with MockMetrics {

  private implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  private val customsDeclarationsConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
  private val submissionRepository: SubmissionRepository = mock[SubmissionRepository]
  private val declarationRepository: DeclarationRepository = mock[DeclarationRepository]
  private val metaDataBuilder: CancellationMetaDataBuilder = mock[CancellationMetaDataBuilder]
  private val wcoMapperService: WcoMapperService = mock[WcoMapperService]
  private val sendEmailForDmsDocAction: SendEmailForDmsDocAction = mock[SendEmailForDmsDocAction]

  private val submissionService = new SubmissionService(
    customsDeclarationsConnector = customsDeclarationsConnector,
    submissionRepository = submissionRepository,
    declarationRepository = declarationRepository,
    metaDataBuilder = metaDataBuilder,
    wcoMapperService = wcoMapperService,
    metrics = exportsMetrics
  )(ExecutionContext.global)

  override def afterEach(): Unit = {
    reset(customsDeclarationsConnector, submissionRepository, declarationRepository, metaDataBuilder, wcoMapperService, sendEmailForDmsDocAction)
    super.afterEach()
  }

  "SubmissionService on cancel" should {
    val submission = Submission("id", "eori", "lrn", None, "ducr")
    val submissionCancelled = Submission("id", "eori", "lrn", None, "ducr", Seq(Action("conv-id", CancellationRequest)))
    val cancellation = SubmissionCancellation("ref-id", "mrn", "description", "reason")

    "submit and delegate to repository" when {
      "submission exists" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submission)))
        when(submissionRepository.addAction(any[String](), any())).thenReturn(Future.successful(Some(submission)))

        submissionService.cancel("eori", cancellation).futureValue mustBe CancellationRequestSent
      }

      "submission is missing" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(None))

        submissionService.cancel("eori", cancellation).futureValue mustBe MrnNotFound
      }

      "submission exists and previously cancelled" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submissionCancelled)))

        submissionService.cancel("eori", cancellation).futureValue mustBe CancellationAlreadyRequested
      }
    }
  }

  "SubmissionService on submit" should {

    def theSubmissionCreated(): Submission = {
      val captor: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
      verify(submissionRepository).create(captor.capture())
      captor.getValue
    }

    "submit to the Dec API" when {
      val declaration = aDeclaration()

      val dateTimeIssued = ZonedDateTime.now(ZoneOffset.UTC)

      val newAction = Action(id = "conv-id", requestType = SubmissionRequest, requestTimestamp = dateTimeIssued)

      val submission = Submission(declaration, "lrn", "mrn", newAction)

      "declaration is valid" in {
        // Given
        when(wcoMapperService.produceMetaData(any())).thenReturn(mock[MetaData])
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(submissionRepository.create(any())).thenReturn(Future.successful(submission))
        when(customsDeclarationsConnector.submitDeclaration(any(), any())(any())).thenReturn(Future.successful("conv-id"))

        // When
        submissionService.submit(declaration).futureValue mustBe submission

        // Then
        val submissionCreated = theSubmissionCreated()
        val actionGenerated = submissionCreated.actions.head
        submissionCreated mustBe Submission(declaration, "lrn", "ducr", newAction.copy(requestTimestamp = actionGenerated.requestTimestamp))

        actionGenerated.id mustBe "conv-id"
        actionGenerated.requestType mustBe SubmissionRequest

        verify(submissionRepository, never).updateMrn(any[String], any[String])
        verify(sendEmailForDmsDocAction, never).execute(any[String])
      }
    }

    "revert declaration to draft" when {
      "submission to the Dec API fails" in {
        // Given
        when(wcoMapperService.produceMetaData(any())).thenReturn(mock[MetaData])
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(declarationRepository.revertStatusToDraft(any())).thenReturn(Future.successful(Some(mock[ExportsDeclaration])))
        when(submissionRepository.create(any())).thenReturn(Future.successful(mock[Submission]))
        when(customsDeclarationsConnector.submitDeclaration(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Some error")))

        val declaration = aDeclaration()

        // When
        intercept[RuntimeException] {
          submissionService.submit(declaration).futureValue
        }

        // Then
        verify(submissionRepository, never).create(any[Submission])
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

}
