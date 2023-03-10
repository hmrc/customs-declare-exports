package uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers.any
import org.mongodb.scala.model.Filters
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.{IntegrationTestSpec, MockMetrics}
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.connectors.ead.CustomsDeclarationsInformationConnector
import uk.gov.hmrc.exports.models.{Eori, Mrn}
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, ExternalAmendmentRequest, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.models.ead.parsers.MrnDeclarationParserTestData
import uk.gov.hmrc.exports.models.ead.parsers.MrnDeclarationParserTestData.{badTestSample, mrnDeclarationTestSample}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.CancellationMetaDataBuilder
import uk.gov.hmrc.exports.services.reversemapping.declaration.ExportsDeclarationXmlParser
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import java.util.UUID
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubmissionServiceISpec extends IntegrationTestSpec with MockMetrics {

  private val customsDeclarationsConnector: CustomsDeclarationsConnector = mock[CustomsDeclarationsConnector]
  private val metaDataBuilder: CancellationMetaDataBuilder = mock[CancellationMetaDataBuilder]
  private val wcoMapperService: WcoMapperService = mock[WcoMapperService]

  private val declarationRepository = instanceOf[DeclarationRepository]
  private val mockSubmissionRepository = mock[SubmissionRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]

  private val customsDeclarationsInformationConnector = instanceOf[CustomsDeclarationsInformationConnector]
  private val exportsDeclarationXmlParser = instanceOf[ExportsDeclarationXmlParser]

  private val submissionService = new SubmissionService(
    customsDeclarationsConnector = customsDeclarationsConnector,
    customsDeclarationsInformationConnector = customsDeclarationsInformationConnector,
    exportsDeclarationXmlParser = exportsDeclarationXmlParser,
    submissionRepository = mockSubmissionRepository,
    declarationRepository = declarationRepository,
    metaDataBuilder = metaDataBuilder,
    wcoMapperService = wcoMapperService,
    metrics = exportsMetrics
  )(global)

  override def afterEach(): Unit = {
    reset(customsDeclarationsConnector, mockSubmissionRepository, metaDataBuilder, wcoMapperService)
    super.afterEach()
  }

  override def beforeEach(): Unit = {
    declarationRepository.removeAll.futureValue
    submissionRepository.removeAll.futureValue
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

        verify(mockSubmissionRepository, never).create(any[Submission])

        val resultingDeclaration = declarationRepository.findOne("id", declaration.id).futureValue.value
        resultingDeclaration.status mustBe DeclarationStatus.DRAFT
      }
    }
  }

  "fetchExternalAmendmentToUpdateSubmission" should {

    val submissionService = new SubmissionService(
      customsDeclarationsConnector = customsDeclarationsConnector,
      customsDeclarationsInformationConnector = customsDeclarationsInformationConnector,
      exportsDeclarationXmlParser = exportsDeclarationXmlParser,
      submissionRepository = submissionRepository,
      declarationRepository = declarationRepository,
      metaDataBuilder = metaDataBuilder,
      wcoMapperService = wcoMapperService,
      metrics = exportsMetrics
    )(global)

    val mrn = "MyMucrValue1234"
    val eori = "GB167676"
    val submissionActionId = "b1c09f1b-7c94-4e90-b754-7c5c71c44e01"
    val externalActionId2 = "b1c09f1b-7c94-4e90-b754-7c5c71c44e02"
    val externalActionId3 = "b1c09f1b-7c94-4e90-b754-7c5c71c44e03"
    val id = "ID"

    val declaration = aDeclaration(withId(UUID.randomUUID.toString))

    val submissionAction = Action(id = submissionActionId, requestType = SubmissionRequest, decId = Some(declaration.id), versionNo = 1)
    val externalActionVersion2 = Action(id = externalActionId2, requestType = ExternalAmendmentRequest, decId = None, versionNo = 2)
    val externalActionVersion3 = Action(id = externalActionId3, requestType = ExternalAmendmentRequest, decId = None, versionNo = 3)

    val fetchMrnDeclarationUrl = "/mrn/" + id + "/full"
    val mrnDeclarationUrl = fetchMrnDeclarationUrl.replace(id, mrn)

    "retrieve and parse a declaration from DIS using an MRN" which {

      "is persisted with a new `declarationId`" which {
        "has an updated action where decId is updated" which {
          "update submission.latestDecId" when {
            "action.versionNo equals submission.latestVersionNo" when {
              "there is a single ExternalAmendment" in {

                getFromDownstreamService(mrnDeclarationUrl, OK, Some(MrnDeclarationParserTestData.mrnDeclarationTestSample(mrn, None).toString))

                val submission =
                  Submission(declaration, "lrn", "ducr", submissionAction).copy(
                    latestVersionNo = 2,
                    latestDecId = None,
                    actions = List(submissionAction, externalActionVersion2)
                  )

                submissionRepository.insertOne(submission).futureValue.isRight mustBe true

                val result: Submission = submissionService
                  .fetchExternalAmendmentToUpdateSubmission(mrn = Mrn(mrn), Eori(eori), externalActionId2, submission.uuid)(hc)
                  .futureValue
                  .value

                val savedDec = declarationRepository
                  .findOne(Filters.equal("eori", eori))
                  .futureValue
                  .value

                result.latestDecId.value mustBe savedDec.id
                result.actions(1).decId.value mustBe savedDec.id

              }
              "there are more ExternalAmendments" in {

                getFromDownstreamService(mrnDeclarationUrl, OK, Some(MrnDeclarationParserTestData.mrnDeclarationTestSample(mrn, None).toString))

                val submission =
                  Submission(declaration, "lrn", "ducr", submissionAction).copy(
                    latestVersionNo = 3,
                    latestDecId = None,
                    actions = List(submissionAction, externalActionVersion2, externalActionVersion3)
                  )

                submissionRepository.insertOne(submission).futureValue.isRight mustBe true

                val result: Submission = submissionService
                  .fetchExternalAmendmentToUpdateSubmission(mrn = Mrn(mrn), Eori(eori), externalActionId3, submission.uuid)(hc)
                  .futureValue
                  .value

                val savedDec = declarationRepository
                  .findOne(Filters.equal("eori", eori))
                  .futureValue
                  .value

                result.latestDecId.value mustBe savedDec.id
                result.actions(2).decId.value mustBe savedDec.id

              }
            }
          }

          "does not update submission.latestDecId" when {
            "action.versionNo is not submission.latestVersionNo" in {

              getFromDownstreamService(mrnDeclarationUrl, OK, Some(mrnDeclarationTestSample(mrn, None).toString))

              val submission =
                Submission(declaration, "lrn", "ducr", submissionAction).copy(
                  latestVersionNo = 3,
                  latestDecId = None,
                  actions = List(submissionAction, externalActionVersion2, externalActionVersion3)
                )

              submissionRepository.insertOne(submission).futureValue.isRight mustBe true

              val result: Submission = submissionService
                .fetchExternalAmendmentToUpdateSubmission(mrn = Mrn(mrn), Eori(eori), externalActionId2, submission.uuid)(hc)
                .futureValue
                .value

              val savedDec = declarationRepository
                .findOne(Filters.equal("eori", eori))
                .futureValue
                .value

              result.latestDecId mustBe None
              result.actions(1).decId.value mustBe savedDec.id

            }
          }
        }
      }
    }

    "return without submission" when {
      "declaration cannot be parsed from DIS" in {

        getFromDownstreamService(mrnDeclarationUrl, OK, Some(badTestSample.toString))

        val submission =
          Submission(declaration, "lrn", "ducr", submissionAction).copy(
            latestVersionNo = 3,
            latestDecId = None,
            actions = List(submissionAction, externalActionVersion2, externalActionVersion3)
          )

        submissionRepository.insertOne(submission).futureValue.isRight mustBe true

        val result: Option[Submission] = submissionService
          .fetchExternalAmendmentToUpdateSubmission(mrn = Mrn(mrn), Eori(eori), externalActionId3, submission.uuid)(hc)
          .futureValue

        result mustBe None

      }
      "submission cannot be updated" in {

        getFromDownstreamService(mrnDeclarationUrl, OK, Some(MrnDeclarationParserTestData.mrnDeclarationTestSample(mrn, None).toString))

        val submission =
          Submission(declaration, "lrn", "ducr", submissionAction).copy(
            latestVersionNo = 3,
            latestDecId = None,
            actions = List(submissionAction, externalActionVersion2, externalActionVersion3)
          )

        submissionRepository.insertOne(submission).futureValue.isRight mustBe true

        val result: Option[Submission] = submissionService
          .fetchExternalAmendmentToUpdateSubmission(mrn = Mrn(mrn), Eori(eori), externalActionId3, "noId")(hc)
          .futureValue

        result mustBe None

      }
    }

    "throw exception" when {
      "no declaration is returned from DIS" in {

        getFromDownstreamService(mrnDeclarationUrl, NOT_FOUND)

        val submission =
          Submission(declaration, "lrn", "ducr", submissionAction).copy(
            latestVersionNo = 3,
            latestDecId = None,
            actions = List(submissionAction, externalActionVersion2, externalActionVersion3)
          )

        intercept[InternalServerException] {
          await(submissionService.fetchExternalAmendmentToUpdateSubmission(mrn = Mrn(mrn), Eori(eori), externalActionId3, submission.uuid)(hc))
        }
      }
    }

  }
}
