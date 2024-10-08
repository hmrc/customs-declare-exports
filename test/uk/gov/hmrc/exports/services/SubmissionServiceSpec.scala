/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.invocation.InvocationOnMock
import org.scalatest.Assertion
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.exports.base.{MockMetrics, UnitSpec}
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.FetchSubmissionPageData.DEFAULT_LIMIT
import uk.gov.hmrc.exports.models.declaration.submissions.CancellationStatus.CancellationResult
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.{CUSTOMS_POSITION_GRANTED, PENDING, WITHDRAWN}
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup._
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.{Eori, FetchSubmissionPageData, PageOfSubmissions}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.{AmendmentMetaDataBuilder, CancellationMetaDataBuilder, ExportsPointerToWCOPointer}
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, TimeUtils}
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class SubmissionServiceSpec extends UnitSpec with ExportsDeclarationBuilder with MockMetrics {

  private implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  private val customsDeclarationsConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
  private val submissionRepository: SubmissionRepository = mock[SubmissionRepository]
  private val declarationRepository: DeclarationRepository = mock[DeclarationRepository]
  private val exportsPointerToWCOPointer: ExportsPointerToWCOPointer = mock[ExportsPointerToWCOPointer]
  private val cancelMetaDataBuilder: CancellationMetaDataBuilder = mock[CancellationMetaDataBuilder]
  private val amendMetaDataBuilder: AmendmentMetaDataBuilder = mock[AmendmentMetaDataBuilder]
  private val wcoMapperService: WcoMapperService = mock[WcoMapperService]
  private val metadata = mock[MetaData]

  private val submissionService = new SubmissionService(
    customsDeclarationsConnector = customsDeclarationsConnector,
    submissionRepository = submissionRepository,
    declarationRepository = declarationRepository,
    exportsPointerToWCOPointer = exportsPointerToWCOPointer,
    cancelMetaDataBuilder = cancelMetaDataBuilder,
    amendmentMetaDataBuilder = amendMetaDataBuilder,
    wcoMapperService = wcoMapperService,
    metrics = exportsMetrics
  )(ExecutionContext.global)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(
      customsDeclarationsConnector,
      submissionRepository,
      declarationRepository,
      exportsPointerToWCOPointer,
      cancelMetaDataBuilder,
      amendMetaDataBuilder,
      wcoMapperService
    )

    when(submissionRepository.countSubmissionsInGroup(any(), any())).thenReturn(Future.successful(1))
  }
  private val eori = "eori"
  private val id = "id"
  private val xml = "xml"
  private val submission = Submission(id, eori, "lrn", Some("mrn"), "ducr", latestDecId = Some(id))

  "SubmissionService.cancel" should {
    val now = TimeUtils.now()
    val notification = Some(Seq(new NotificationSummary(UUID.randomUUID(), now, CUSTOMS_POSITION_GRANTED)))
    val submissionCancelled = Submission(
      id,
      eori,
      "lrn",
      None,
      "ducr",
      PENDING,
      now,
      List(Action(id = "conv-id", requestType = CancellationRequest, notifications = notification, decId = None, versionNo = 1)),
      latestDecId = Some(id)
    )
    val cancellation = SubmissionCancellation(id, "ref-id", "mrn", "description", "reason")

    "submit and delegate to repository and iterates version number in cancel action from submission" when {

      "submission exists" which {
        "copies version number to cancel action from submission" in {
          when(cancelMetaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(metadata)
          when(wcoMapperService.toXml(any())).thenReturn(xml)
          when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
          when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submission)))

          val captor: ArgumentCaptor[Action] = ArgumentCaptor.forClass(classOf[Action])

          when(submissionRepository.addAction(any[String](), any[Action]())).thenReturn(Future.successful(Some(submission)))

          submissionService.cancelDeclaration(eori, cancellation).futureValue mustBe CancellationResult(CancellationRequestSent, Some("conv-id"))

          verify(submissionRepository).addAction(any[String](), captor.capture())

          captor.getValue.decId mustBe submission.latestDecId
          captor.getValue.versionNo mustBe submission.latestVersionNo
        }
      }

      "submission is missing" in {
        when(cancelMetaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(metadata)
        when(wcoMapperService.toXml(any())).thenReturn(xml)
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(None))

        submissionService.cancelDeclaration(eori, cancellation).futureValue mustBe CancellationResult(NotFound, None)
      }

      "submission exists and previously cancelled" in {
        when(cancelMetaDataBuilder.buildRequest(any(), any(), any(), any(), any())).thenReturn(metadata)
        when(wcoMapperService.toXml(any())).thenReturn(xml)
        when(customsDeclarationsConnector.submitCancellation(any(), any())(any())).thenReturn(Future.successful("conv-id"))
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submissionCancelled)))

        submissionService.cancelDeclaration(eori, cancellation).futureValue mustBe CancellationResult(CancellationAlreadyRequested, None)
      }
    }
  }

  private val cancelledSubmissions = List(submission.copy(latestEnhancedStatus = WITHDRAWN))

  "SubmissionService.fetchFirstPage" should {

    "fetch the 1st page of the 1st StatusGroup containing submissions" in {
      val captor: ArgumentCaptor[StatusGroup] = ArgumentCaptor.forClass(classOf[StatusGroup])

      def isCancelledGroup(invocation: InvocationOnMock) =
        invocation.getArgument(1).asInstanceOf[StatusGroup] == CancelledStatuses

      when(submissionRepository.countSubmissionsInGroup(any(), captor.capture())).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(if (isCancelledGroup(invocation)) 1 else 0)
      }

      when(submissionRepository.fetchFirstPage(any(), captor.capture(), any[FetchSubmissionPageData])).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(if (isCancelledGroup(invocation)) cancelledSubmissions else Seq.empty)
      }

      val statusGroups = List(ActionRequiredStatuses, RejectedStatuses, SubmittedStatuses, CancelledStatuses)
      val fetchData = FetchSubmissionPageData(statusGroups, reverse = false, limit = DEFAULT_LIMIT)
      verifPageOfSubmissions(submissionService.fetchFirstPage(eori, fetchData).futureValue)

      val numberOfInvocations = 4
      verify(submissionRepository, times(numberOfInvocations)).fetchFirstPage(any(), any(), any())
    }

    "fetch the first page of a specific StatusGroup" in {
      when(submissionRepository.fetchFirstPage(any(), any[StatusGroup], any[FetchSubmissionPageData]))
        .thenReturn(Future.successful(cancelledSubmissions))

      val fetchData = FetchSubmissionPageData(List(CancelledStatuses), reverse = false, limit = DEFAULT_LIMIT)
      verifPageOfSubmissions(submissionService.fetchFirstPage(eori, CancelledStatuses, fetchData).futureValue)
    }
  }

  "SubmissionService.fetchPage" should {

    val now = TimeUtils.now()

    def fetchSubmissionPageData(
      datetimeForPreviousPage: Option[ZonedDateTime] = Some(now),
      datetimeForNextPage: Option[ZonedDateTime] = Some(now.plusSeconds(1L)),
      page: Option[Int] = Some(2)
    ): FetchSubmissionPageData =
      FetchSubmissionPageData(List(CancelledStatuses), datetimeForPreviousPage, datetimeForNextPage, page, false, DEFAULT_LIMIT)

    "call submissionRepository.fetchNextPage" when {
      "in FetchSubmissionPageData, statusGroup and datetimeForNextPage are provided and" when {
        "datetimeForPreviousPage is not provided" in {
          when(submissionRepository.fetchNextPage(any(), any[StatusGroup], any[ZonedDateTime], any[FetchSubmissionPageData]))
            .thenReturn(Future.successful(cancelledSubmissions))

          verifPageOfSubmissions(submissionService.fetchPage(eori, CancelledStatuses, fetchSubmissionPageData(None)).futureValue)

          verify(submissionRepository).fetchNextPage(any(), eqTo(CancelledStatuses), eqTo(now.plusSeconds(1L)), any[FetchSubmissionPageData])

          verify(submissionRepository, never).fetchLastPage(any(), any(), any())
          verify(submissionRepository, never).fetchLoosePage(any(), any(), any(), any())
          verify(submissionRepository, never).fetchPreviousPage(any(), any(), any(), any())
        }
      }
    }

    "call submissionRepository.fetchPreviousPage" when {
      "in FetchSubmissionPageData, statusGroup and datetimeForPreviousPage are provided" in {
        when(submissionRepository.fetchPreviousPage(any(), any[StatusGroup], any[ZonedDateTime], any[FetchSubmissionPageData]))
          .thenReturn(Future.successful(cancelledSubmissions))

        verifPageOfSubmissions(submissionService.fetchPage(eori, CancelledStatuses, fetchSubmissionPageData()).futureValue)

        verify(submissionRepository).fetchPreviousPage(any(), eqTo(CancelledStatuses), eqTo(now), any[FetchSubmissionPageData])

        verify(submissionRepository, never).fetchLastPage(any(), any(), any())
        verify(submissionRepository, never).fetchLoosePage(any(), any(), any(), any())
        verify(submissionRepository, never).fetchNextPage(any(), any(), any(), any())
      }
    }

    "call submissionRepository.fetchLoosePage" when {
      "in FetchSubmissionPageData, statusGroup and page are provided and" when {
        "datetimeForNextPage and datetimeForPreviousPage are not provided" in {
          when(submissionRepository.fetchLoosePage(any(), any[StatusGroup], any[Int], any[FetchSubmissionPageData]))
            .thenReturn(Future.successful(cancelledSubmissions))

          verifPageOfSubmissions(submissionService.fetchPage(eori, CancelledStatuses, fetchSubmissionPageData(None, None)).futureValue)

          verify(submissionRepository).fetchLoosePage(any(), eqTo(CancelledStatuses), eqTo(2), any[FetchSubmissionPageData])

          verify(submissionRepository, never).fetchLastPage(any(), any(), any())
          verify(submissionRepository, never).fetchNextPage(any(), any(), any(), any())
          verify(submissionRepository, never).fetchPreviousPage(any(), any(), any(), any())
        }
      }
    }

    "call submissionRepository.fetchLastPage" when {
      "in FetchSubmissionPageData, only the statusGroup is provided" in {
        when(submissionRepository.fetchLastPage(any(), any[StatusGroup], any[FetchSubmissionPageData]))
          .thenReturn(Future.successful(cancelledSubmissions))

        verifPageOfSubmissions(submissionService.fetchPage(eori, CancelledStatuses, fetchSubmissionPageData(None, None, None)).futureValue)

        verify(submissionRepository).fetchLastPage(any(), eqTo(CancelledStatuses), any[FetchSubmissionPageData])

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

    "submit to the Dec API" when {
      "declaration is valid" in {
        val declaration = aDeclaration(withId(submission.uuid))

        when(wcoMapperService.produceMetaData(any())).thenReturn(metadata)
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))
        when(wcoMapperService.toXml(any())).thenReturn(xml)
        when(submissionRepository.create(any())).thenReturn(Future.successful(submission))
        when(customsDeclarationsConnector.submitDeclaration(any(), any())(any())).thenReturn(Future.successful("conv-id"))

        submissionService.submitDeclaration(declaration).futureValue.uuid mustBe declaration.id
      }
    }

    "throw exception" when {
      val declaration = aDeclaration()

      "missing LRN" in {
        when(wcoMapperService.produceMetaData(any())).thenReturn(metadata)
        when(wcoMapperService.declarationLrn(any())).thenReturn(None)
        when(declarationRepository.revertStatusToDraft(any())).thenReturn(Future.successful(Some(declaration)))

        whenReady(submissionService.submitDeclaration(declaration).failed) { throwable =>
          throwable mustBe a[IllegalArgumentException]
          throwable.getMessage mustBe "A LRN is required"
        }
      }

      "missing DUCR" in {
        when(wcoMapperService.produceMetaData(any())).thenReturn(metadata)
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(None)
        when(declarationRepository.revertStatusToDraft(any())).thenReturn(Future.successful(Some(declaration)))

        whenReady(submissionService.submitDeclaration(declaration).failed) { throwable =>
          throwable mustBe a[IllegalArgumentException]
          throwable.getMessage mustBe "A DUCR is required"
        }
      }
    }
  }

  "SubmissionService.submitAmendment" should {
    val fieldPointers = Seq("pointers")
    val wcoPointers = Seq("wco")
    val amendmentId = "amendmentId"
    val actionId = "actionId"
    val submissionAmendment = SubmissionAmendment(id, amendmentId, false, fieldPointers)
    val declaration = aDeclaration(withId(amendmentId), withEori(eori), withConsignmentReferences(mrn = Some("mrn")))

    "throw appropriate exception and revert the status of the amendment to AMENDMENT_DRAFT" when {

      "initial submission lookup does not return a submission" in {
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(None))
        when(declarationRepository.revertStatusToAmendmentDraft(any())).thenReturn(Future.successful(Some(declaration)))

        assertThrows[java.util.NoSuchElementException] {
          await(submissionService.submitAmendment(Eori(eori), submissionAmendment, declaration))
        }
        verify(declarationRepository).revertStatusToAmendmentDraft(meq(declaration))
      }

      "updating submission with amendment action fails" in {
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submission)))
        when(exportsPointerToWCOPointer.getWCOPointers(any())).thenReturn(Right(wcoPointers))
        when(amendMetaDataBuilder.buildRequest(any(), any(), any())).thenReturn(metadata)
        when(wcoMapperService.toXml(any())).thenReturn(xml)
        when(customsDeclarationsConnector.submitAmendment(any(), any())(any())).thenReturn(Future.successful(actionId))
        // The following triggers an error
        when(submissionRepository.addAction(any(), any())).thenReturn(Future.successful(None))
        when(declarationRepository.revertStatusToAmendmentDraft(any())).thenReturn(Future.successful(Some(declaration)))

        assertThrows[java.util.NoSuchElementException] {
          await(submissionService.submitAmendment(Eori(eori), submissionAmendment, declaration))
        }
        verify(declarationRepository).revertStatusToAmendmentDraft(meq(declaration))
      }
    }

    "call expected methods and return an actionId" in {
      when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submission)))
      when(exportsPointerToWCOPointer.getWCOPointers(any())).thenReturn(Right(wcoPointers))
      when(amendMetaDataBuilder.buildRequest(any(), any(), any())).thenReturn(metadata)
      when(wcoMapperService.toXml(any())).thenReturn(xml)
      when(customsDeclarationsConnector.submitAmendment(any(), any())(any())).thenReturn(Future.successful(actionId))
      when(submissionRepository.addAction(any(), any())).thenReturn(Future.successful(Some(submission)))

      await(submissionService.submitAmendment(Eori(eori), submissionAmendment, declaration)) mustBe actionId

      verify(submissionRepository).findOne(meq(Json.obj("eori" -> eori, "uuid" -> submission.uuid)))
      verify(exportsPointerToWCOPointer).getWCOPointers(meq(fieldPointers.head))
      verify(amendMetaDataBuilder).buildRequest(meq(submission.mrn), meq(declaration), meq(wcoPointers))
      verify(wcoMapperService).toXml(meq(metadata))
      verify(customsDeclarationsConnector).submitAmendment(meq(eori), meq(xml))(any())
      val captor: ArgumentCaptor[Action] = ArgumentCaptor.forClass(classOf[Action])
      verify(submissionRepository).addAction(meq(id), captor.capture())

      captor.getValue.id mustBe actionId
      captor.getValue.requestType mustBe AmendmentRequest
      captor.getValue.decId mustBe Some(amendmentId)
      captor.getValue.versionNo mustBe submission.latestVersionNo + 1
    }
  }

  "SubmissionService.resubmitAmendment" should {
    val fieldPointers = Seq("pointers")
    val wcoPointers = Seq("wco")
    val amendmentId = "amendmentId"
    val actionId = "actionId"
    val submissionAmendment = SubmissionAmendment(id, amendmentId, false, fieldPointers)
    val declaration = aDeclaration(withId(amendmentId), withEori(eori), withConsignmentReferences(mrn = Some("mrn")))

    "throw appropriate exception" when {

      "initial submission lookup does not return a submission" in {
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(None))

        assertThrows[java.util.NoSuchElementException] {
          await(submissionService.resubmitAmendment(Eori(eori), submissionAmendment, declaration))
        }
      }

      "updating submission with amendment action fails" in {
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submission)))
        when(exportsPointerToWCOPointer.getWCOPointers(any())).thenReturn(Right(wcoPointers))
        when(amendMetaDataBuilder.buildRequest(any(), any(), any())).thenReturn(metadata)
        when(wcoMapperService.toXml(any())).thenReturn(xml)
        when(customsDeclarationsConnector.submitAmendment(any(), any())(any())).thenReturn(Future.successful(actionId))
        // The following triggers an error
        when(submissionRepository.addAction(any(), any())).thenReturn(Future.successful(None))

        assertThrows[java.util.NoSuchElementException] {
          await(submissionService.resubmitAmendment(Eori(eori), submissionAmendment, declaration))
        }
      }
    }

    "call expected methods and return an actionId" in {
      when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submission)))
      when(exportsPointerToWCOPointer.getWCOPointers(any())).thenReturn(Right(wcoPointers))
      when(amendMetaDataBuilder.buildRequest(any(), any(), any())).thenReturn(metadata)
      when(wcoMapperService.toXml(any())).thenReturn(xml)
      when(customsDeclarationsConnector.submitAmendment(any(), any())(any())).thenReturn(Future.successful(actionId))
      when(submissionRepository.addAction(any(), any())).thenReturn(Future.successful(Some(submission)))

      await(submissionService.resubmitAmendment(Eori(eori), submissionAmendment, declaration)) mustBe actionId

      verify(submissionRepository).findOne(meq(Json.obj("eori" -> eori, "uuid" -> submission.uuid)))
      verify(exportsPointerToWCOPointer).getWCOPointers(meq(fieldPointers.head))
      verify(amendMetaDataBuilder).buildRequest(meq(submission.mrn), meq(declaration), meq(wcoPointers))
      verify(wcoMapperService).toXml(meq(metadata))
      verify(customsDeclarationsConnector).submitAmendment(meq(eori), meq(xml))(any())
      val captor: ArgumentCaptor[Action] = ArgumentCaptor.forClass(classOf[Action])
      verify(submissionRepository).addAction(meq(id), captor.capture())

      captor.getValue.id mustBe actionId
      captor.getValue.requestType mustBe AmendmentRequest
      captor.getValue.decId mustBe Some(amendmentId)
      captor.getValue.versionNo mustBe submission.latestVersionNo + 1
    }
  }

  "SubmissionService.cancelAmendment" should {
    "throw an exception" when {
      "the associate Submission cannot be found" in {
        when(submissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(None))

        assertThrows[java.util.NoSuchElementException] {
          await(submissionService.cancelAmendment(Eori(eori), id, aDeclaration()))
        }
      }
    }
  }
}
