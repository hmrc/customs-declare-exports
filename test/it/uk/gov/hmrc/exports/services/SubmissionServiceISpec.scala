package uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers.any
import play.api.Logging
import uk.gov.hmrc.exports.base.{IntegrationTestMongoSpec, MockMetrics}
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.CancellationMetaDataBuilder
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubmissionServiceISpec extends IntegrationTestMongoSpec with MockMetrics with Logging {

  private val customsDeclarationsConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
  private val metaDataBuilder: CancellationMetaDataBuilder = mock[CancellationMetaDataBuilder]
  private val wcoMapperService: WcoMapperService = mock[WcoMapperService]

  private val declarationRepository = instanceOf[DeclarationRepository]
  private val submissionRepository = mock[SubmissionRepository]

  private val submissionService = new SubmissionService(
    customsDeclarationsConnector = customsDeclarationsConnector,
    submissionRepository = submissionRepository,
    declarationRepository = declarationRepository,
    metaDataBuilder = metaDataBuilder,
    wcoMapperService = wcoMapperService,
    metrics = exportsMetrics
  )(global)

  override def afterEach(): Unit = {
    reset(customsDeclarationsConnector, submissionRepository, metaDataBuilder, wcoMapperService)
    super.afterEach()
  }

  override def beforeEach(): Unit = {
    declarationRepository.removeAll.futureValue
    super.afterEach()
  }

  "SubmissionService.submit" should {
    "revert a declaration to DRAFT" when {
      "the submission to the Dec API fails" in {
        when(wcoMapperService.produceMetaData(any())).thenReturn(mock[MetaData])
        when(wcoMapperService.declarationLrn(any())).thenReturn(Some("lrn"))
        when(wcoMapperService.declarationDucr(any())).thenReturn(Some("ducr"))
        when(wcoMapperService.toXml(any())).thenReturn("xml")
        when(customsDeclarationsConnector.submitDeclaration(any[String], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("Some error")))

        val declaration = aDeclaration()
        declarationRepository.insertOne(declaration).futureValue.isRight mustBe true

        intercept[RuntimeException] {
          submissionService.submit(declaration)(HeaderCarrier()).futureValue
        }

        verify(submissionRepository, never).create(any[Submission])

        val resultingDeclaration = declarationRepository.findOne("id", declaration.id).futureValue.value
        resultingDeclaration.status mustBe DeclarationStatus.DRAFT
      }
    }
  }
}
