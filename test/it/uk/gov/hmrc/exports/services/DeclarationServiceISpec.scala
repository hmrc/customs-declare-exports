package uk.gov.hmrc.exports.services

import org.mongodb.scala.model.Filters
import org.scalatest.exceptions.TestFailedException
import play.api.test.Helpers.OK
import uk.gov.hmrc.exports.base.{IntegrationTestSpec, MockMetrics}
import uk.gov.hmrc.exports.connectors.ead.CustomsDeclarationsInformationConnector
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, ExternalAmendmentRequest, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.models.ead.parsers.MrnDeclarationParserTestData
import uk.gov.hmrc.exports.models.{Eori, Mrn}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.reversemapping.declaration.ExportsDeclarationXmlParser

import java.util.UUID
import scala.concurrent.ExecutionContext.global

class DeclarationServiceISpec extends IntegrationTestSpec with MockMetrics {

  private val declarationRepository = instanceOf[DeclarationRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]
  private val customsDeclarationsInformationConnector = instanceOf[CustomsDeclarationsInformationConnector]
  private val exportsDeclarationXmlParser = instanceOf[ExportsDeclarationXmlParser]

  private val declarationService =
    new DeclarationService(declarationRepository, submissionRepository, customsDeclarationsInformationConnector, exportsDeclarationXmlParser)

  override def beforeEach(): Unit = {
    submissionRepository.removeAll.futureValue
    declarationRepository.removeAll.futureValue
    super.afterEach()
  }

  "DeclarationService.findOrCreateDraftFromParent" should {
    "throw an exception" when {
      "a draft declaration with 'parentDeclarationId' equal to the given parentId was NOT found and" when {
        "a 'parent' declaration was NOT found too" in {
          intercept[TestFailedException] {
            declarationService.findOrCreateDraftFromParent(Eori("some eori"), "some id")(global).futureValue
          }.cause.get.isInstanceOf[NoSuchElementException]
        }
      }
    }
  }

  "fetchAndSave" should {
    "retrieve and parse a declaration from DIS using an MRN" which {
      "is persisted with a new `declarationId`" which {
        "has an updated action where decId is updated" which {
          "if having a matching action.versionNo to submission.latestVersionNo should update submission.latestDecId" in {

            val mrn = "MyMucrValue1234"
            val eori = "GB167676"
            val submissionActionId = "b1c09f1b-7c94-4e90-b754-7c5c71c44e10"
            val externalActionId = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"

            val id = "ID"

            val fetchMrnDeclarationUrl = "/mrn/" + id + "/full"
            val mrnDeclarationUrl = fetchMrnDeclarationUrl.replace(id, mrn)

            getFromDownstreamService(mrnDeclarationUrl, OK, Some(MrnDeclarationParserTestData.mrnDeclarationTestSample(mrn, None).toString))

            val declaration = aDeclaration(withId(UUID.randomUUID.toString))

            val submissionAction = Action(id = submissionActionId, requestType = SubmissionRequest, decId = Some(declaration.id), versionNo = 1)
            val externalAction = Action(id = externalActionId, requestType = ExternalAmendmentRequest, decId = None, versionNo = 2)

            val submission =
              Submission(declaration, "lrn", "ducr", submissionAction).copy(latestVersionNo = 2, actions = List(submissionAction, externalAction))

            submissionRepository.insertOne(submission).futureValue.isRight mustBe true

            val result: Submission = declarationService
              .fetchAndSave(mrn = Mrn(mrn), Eori(eori), externalActionId, submission.uuid)(global, hc)
              .futureValue
              .value

            val savedDec = declarationRepository
              .findOne(Filters.equal("eori", eori))
              .futureValue
              .value

            result.latestDecId.value mustBe savedDec.id
            result.actions(1).decId.value mustBe savedDec.id

          }
        }
      }
    }
  }

}
