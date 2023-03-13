package uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers.any
import play.api.libs.json.JsValue
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.exports.base.{IntegrationTestSpec, MockMetrics}
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.submissions.{AmendmentRequest, Submission, SubmissionAmendment}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.{AmendmentMetaDataBuilder, CancellationMetaDataBuilder, ExportsPointerToWCOPointer}
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubmissionServiceISpec extends IntegrationTestSpec with MockMetrics {

  private val customsDeclarationsConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
  private val metaDataBuilder: CancellationMetaDataBuilder = mock[CancellationMetaDataBuilder]
  private val amendmentMetaDataBuilder: AmendmentMetaDataBuilder = mock[AmendmentMetaDataBuilder]
  private val wcoMapperService: WcoMapperService = mock[WcoMapperService]
  private val exportsPointerToWCOPointer: ExportsPointerToWCOPointer = mock[ExportsPointerToWCOPointer]

  private val declarationRepository = instanceOf[DeclarationRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]
  private val mockSubmissionRepository = mock[SubmissionRepository]

  private val submissionService = new SubmissionService(
    customsDeclarationsConnector = customsDeclarationsConnector,
    submissionRepository = submissionRepository,
    declarationRepository = declarationRepository,
    exportsPointerToWCOPointer = exportsPointerToWCOPointer,
    cancelMetaDataBuilder = metaDataBuilder,
    amendmentMetaDataBuilder = amendmentMetaDataBuilder,
    wcoMapperService = wcoMapperService,
    metrics = exportsMetrics
  )(global)

  override def afterEach(): Unit = {
    reset(customsDeclarationsConnector, mockSubmissionRepository, exportsPointerToWCOPointer, metaDataBuilder, wcoMapperService)
    super.afterEach()
  }

  override def beforeEach(): Unit = {
    declarationRepository.removeAll.futureValue
    submissionRepository.removeAll.futureValue
    super.afterEach()
  }

  private val id = "id"
  private val xml = "xml"
  private val metadata = mock[MetaData]

  "SubmissionService.submit" should {
    "revert a declaration to a draft status" when {
      "the submission to the Dec API fails on submit" in {
        when(wcoMapperService.produceMetaData(any())).thenReturn(metadata)
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))
        when(wcoMapperService.toXml(any())).thenReturn(xml)
        when(customsDeclarationsConnector.submitDeclaration(any[String], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("Some error")))

        val declaration = aDeclaration()
        declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

        intercept[RuntimeException] {
          submissionService.submit(declaration)(HeaderCarrier()).futureValue
        }

        verify(mockSubmissionRepository, never).create(any[Submission])

        val resultingDeclaration = declarationRepository.findOne(id, declaration.id).futureValue.value
        resultingDeclaration.status mustBe DeclarationStatus.DRAFT
      }
    }
  }

  "SubmissionService.amend" should {
    val eori = "eori"
    val mrn = Some("mrn")
    val amendmentId = "amendmentId"
    val actionId = "actionId"
    val submissionAmendment = SubmissionAmendment(id, amendmentId, Seq(""))
    val submission = Submission(id, eori, "lrn", mrn, "ducr", latestDecId = Some(amendmentId))
    val declaration = aDeclaration(withId(amendmentId), withEori(eori), withConsignmentReferences(mrn = mrn))

    "add an Amendment action to the submission on amend" in {
      when(exportsPointerToWCOPointer.getWCOPointers(any())).thenReturn(Seq(""))
      when(amendmentMetaDataBuilder.buildRequest(any(), any())).thenReturn(metadata)
      when(wcoMapperService.toXml(any())).thenReturn(xml)
      when(customsDeclarationsConnector.submitAmendment(any(), any())(any())).thenReturn(Future.successful(actionId))
      await(submissionRepository.insertOne(submission))

      await(submissionService.amend(Eori(eori), submissionAmendment, declaration)(HeaderCarrier()))

      val expectedAction = await(submissionRepository.findById(eori, id)).get.actions.head
      expectedAction.decId.get mustBe amendmentId
      expectedAction.id mustBe actionId
      expectedAction.requestType mustBe AmendmentRequest
      expectedAction.versionNo mustBe submission.latestVersionNo
    }

    "revert submission to AMENDMENT_DRAFT on failure" in {
      when(mockSubmissionRepository.findOne(any[JsValue])).thenReturn(Future.successful(Some(submission)))
      when(amendmentMetaDataBuilder.buildRequest(any(), any())).thenReturn(metadata)
      when(wcoMapperService.toXml(any())).thenReturn(xml)
      when(customsDeclarationsConnector.submitDeclaration(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("Some error")))

      val declaration = aDeclaration(withId(amendmentId), withEori(eori), withConsignmentReferences(mrn = Some("mrn")))
      declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

      intercept[RuntimeException] {
        submissionService.amend(Eori(eori), submissionAmendment, declaration)(HeaderCarrier()).futureValue
      }

      val resultingDeclaration = declarationRepository.findOne(id, amendmentId).futureValue.value
      resultingDeclaration.status mustBe DeclarationStatus.AMENDMENT_DRAFT
    }
  }
}
