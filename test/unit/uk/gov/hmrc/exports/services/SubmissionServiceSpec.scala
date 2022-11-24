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
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.invocation.InvocationOnMock
import org.scalatest.Assertion
import play.api.libs.json.JsValue
import uk.gov.hmrc.exports.base.{MockMetrics, UnitSpec}
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.FetchSubmissionPageData.DEFAULT_LIMIT
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.{cancelledStatuses_text, CUSTOMS_POSITION_GRANTED, WITHDRAWN}
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup.CancelledStatuses
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.{FetchSubmissionPageData, PageOfSubmissions}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.CancellationMetaDataBuilder
import uk.gov.hmrc.exports.services.notifications.receiptactions.SendEmailForDmsDocAction
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID
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

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(submissionRepository.countSubmissionsInGroup(any(), any[Set[String]])).thenReturn(Future.successful(1))
  }
  private val eori = "eori"
  private val submission = Submission("id", eori, "lrn", None, "ducr")

  "SubmissionService.cancel" should {
    val notification = Some(Seq(new NotificationSummary(UUID.randomUUID(), ZonedDateTime.now(), CUSTOMS_POSITION_GRANTED)))
    val submissionCancelled = Submission(
      "id",
      eori,
      "lrn",
      None,
      "ducr",
      None,
      None,
      List(Action(id = "conv-id", requestType = CancellationRequest, notifications = notification))
    )
    val cancellation = SubmissionCancellation("id", "ref-id", "mrn", "description", "reason")

    "submit and delegate to repository" when {
      "submission exists" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submission)))
        when(submissionRepository.addAction(any[String](), any())).thenReturn(Future.successful(Some(submission)))

        submissionService.cancel(eori, cancellation).futureValue mustBe CancellationRequestSent
      }

      "submission is missing" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(None))

        submissionService.cancel(eori, cancellation).futureValue mustBe NotFound
      }

      "submission exists and previously cancelled" in {
        when(metaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(mock[MetaData])
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submissionCancelled)))

        submissionService.cancel(eori, cancellation).futureValue mustBe CancellationAlreadyRequested
      }
    }
  }

  private val cancelledSubmissions = List(submission.copy(latestEnhancedStatus = Some(WITHDRAWN)))

  "SubmissionService.fetchFirstPage" should {

    "fetch the 1st page of the 1st StatusGroup containing submissions" in {
      val captor: ArgumentCaptor[Set[String]] = ArgumentCaptor.forClass(classOf[Set[String]])

      def isCancelledGroup(invocation: InvocationOnMock) =
        invocation.getArgument(1).asInstanceOf[Set[String]].contains(WITHDRAWN.toString)

      when(submissionRepository.countSubmissionsInGroup(any(), captor.capture())).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(if (isCancelledGroup(invocation)) 1 else 0)
      }

      when(submissionRepository.fetchFirstPage(any(), captor.capture(), any[Int])).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(if (isCancelledGroup(invocation)) cancelledSubmissions else Seq.empty)
      }

      verifPageOfSubmissions(submissionService.fetchFirstPage(eori, DEFAULT_LIMIT).futureValue)

      val numberOfInvocations = 4
      verify(submissionRepository, times(numberOfInvocations)).fetchFirstPage(any(), any(), any())
    }

    "fetch the first page of a specific StatusGroup" in {
      when(submissionRepository.fetchFirstPage(any(), any[Set[String]], any[Int])).thenReturn(Future.successful(cancelledSubmissions))

      verifPageOfSubmissions(submissionService.fetchFirstPage(eori, CancelledStatuses, DEFAULT_LIMIT).futureValue)
    }
  }

  "SubmissionService.fetchPage" should {

    val now = ZonedDateTime.now

    def fetchSubmissionPageData(
      datetimeForPreviousPage: Option[ZonedDateTime] = Some(now),
      datetimeForNextPage: Option[ZonedDateTime] = Some(now.plusSeconds(1L)),
      page: Option[Int] = Some(2)
    ): FetchSubmissionPageData =
      FetchSubmissionPageData(DEFAULT_LIMIT, Some(CancelledStatuses), datetimeForPreviousPage, datetimeForNextPage, page)

    "call submissionRepository.fetchNextPage" when {
      "in FetchSubmissionPageData, statusGroup and datetimeForNextPage are provided and" when {
        "datetimeForPreviousPage is not provided" in {
          when(submissionRepository.fetchNextPage(any(), any[Set[String]], any[ZonedDateTime], any[Int]))
            .thenReturn(Future.successful(cancelledSubmissions))

          verifPageOfSubmissions(submissionService.fetchPage(eori, CancelledStatuses, fetchSubmissionPageData(None)).futureValue)

          verify(submissionRepository).fetchNextPage(any(), eqTo(cancelledStatuses_text), eqTo(now.plusSeconds(1L)), eqTo(DEFAULT_LIMIT))

          verify(submissionRepository, never).fetchLastPage(any(), any(), any())
          verify(submissionRepository, never).fetchLoosePage(any(), any(), any(), any())
          verify(submissionRepository, never).fetchPreviousPage(any(), any(), any(), any())
        }
      }
    }

    "call submissionRepository.fetchPreviousPage" when {
      "in FetchSubmissionPageData, statusGroup and datetimeForPreviousPage are provided" in {
        when(submissionRepository.fetchPreviousPage(any(), any[Set[String]], any[ZonedDateTime], any[Int]))
          .thenReturn(Future.successful(cancelledSubmissions))

        verifPageOfSubmissions(submissionService.fetchPage(eori, CancelledStatuses, fetchSubmissionPageData()).futureValue)

        verify(submissionRepository).fetchPreviousPage(any(), eqTo(cancelledStatuses_text), eqTo(now), eqTo(DEFAULT_LIMIT))

        verify(submissionRepository, never).fetchLastPage(any(), any(), any())
        verify(submissionRepository, never).fetchLoosePage(any(), any(), any(), any())
        verify(submissionRepository, never).fetchNextPage(any(), any(), any(), any())
      }
    }

    "call submissionRepository.fetchLoosePage" when {
      "in FetchSubmissionPageData, statusGroup and page are provided and" when {
        "datetimeForNextPage and datetimeForPreviousPage are not provided" in {
          when(submissionRepository.fetchLoosePage(any(), any[Set[String]], any[Int], any[Int]))
            .thenReturn(Future.successful(cancelledSubmissions))

          verifPageOfSubmissions(submissionService.fetchPage(eori, CancelledStatuses, fetchSubmissionPageData(None, None)).futureValue)

          verify(submissionRepository).fetchLoosePage(any(), eqTo(cancelledStatuses_text), eqTo(2), eqTo(DEFAULT_LIMIT))

          verify(submissionRepository, never).fetchLastPage(any(), any(), any())
          verify(submissionRepository, never).fetchNextPage(any(), any(), any(), any())
          verify(submissionRepository, never).fetchPreviousPage(any(), any(), any(), any())
        }
      }
    }

    "call submissionRepository.fetchLastPage" when {
      "in FetchSubmissionPageData, only the statusGroup is provided" in {
        when(submissionRepository.fetchLastPage(any(), any[Set[String]], any[Int]))
          .thenReturn(Future.successful(cancelledSubmissions))

        verifPageOfSubmissions(submissionService.fetchPage(eori, CancelledStatuses, fetchSubmissionPageData(None, None, None)).futureValue)

        verify(submissionRepository).fetchLastPage(any(), eqTo(cancelledStatuses_text), eqTo(DEFAULT_LIMIT))

        verify(submissionRepository, never).fetchLoosePage(any(), any(), any(), any())
        verify(submissionRepository, never).fetchNextPage(any(), any(), any(), any())
        verify(submissionRepository, never).fetchPreviousPage(any(), any(), any(), any())
      }
    }
  }

  private def verifPageOfSubmissions(pageOfSubmissions: PageOfSubmissions): Assertion = {
    pageOfSubmissions.statusGroup mustBe CancelledStatuses
    pageOfSubmissions.submissions mustBe cancelledSubmissions
    pageOfSubmissions.totalSubmissionsInGroup mustBe 1
  }

  "SubmissionService.submit" should {

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

        verify(submissionRepository, never).findOne(any[String], any[String])
        verify(sendEmailForDmsDocAction, never).execute(any[String])
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
