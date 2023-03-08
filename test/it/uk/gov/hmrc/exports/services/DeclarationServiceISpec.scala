package uk.gov.hmrc.exports.services

import org.scalatest.exceptions.TestFailedException
import uk.gov.hmrc.exports.base.{IntegrationTestSpec, MockMetrics}
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}

import scala.concurrent.ExecutionContext.global

class DeclarationServiceISpec extends IntegrationTestSpec with MockMetrics {

  private val declarationRepository = instanceOf[DeclarationRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]

  private val declarationService = new DeclarationService(declarationRepository, submissionRepository)

  override def beforeEach(): Unit = {
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
}
