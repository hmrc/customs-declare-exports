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

package integration.uk.gov.hmrc.exports.repositories

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, CancellationRequest}
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import util.testdata.ExportsTestData._
import util.testdata.SubmissionTestData._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionRepositorySpec
    extends WordSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with ScalaFutures with MustMatchers
    with OptionValues {

  override lazy val app: Application = GuiceApplicationBuilder().build()
  private val repo: SubmissionRepository = app.injector.instanceOf[SubmissionRepository]

  implicit val ec: ExecutionContext = global

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.removeAll().futureValue
  }

  override def afterEach(): Unit = {
    repo.removeAll().futureValue
    super.afterEach()
  }

  "Submission Repository on save" when {

    "the operation was successful" should {
      "return true" in {
        repo.save(submission).futureValue must be(submission)

        val submissionInDB = repo.findSubmissionByMrn(mrn).futureValue
        submissionInDB must be(defined)
      }
    }

    "trying to save Submission with the same conversationId twice" should {
      "throw DatabaseException" in {
        repo.save(submission).futureValue must be(submission)
        val secondSubmission = submission_2.copy(actions = submission.actions)

        val exc = repo.save(secondSubmission).failed.futureValue

        exc mustBe an[DatabaseException]
        exc.getMessage must include(
          "E11000 duplicate key error collection: customs-declare-exports.submissions index: conversationIdIdx dup key"
        )
      }

      "result in having only the first Submission persisted" in {
        repo.save(submission).futureValue must be(submission)
        val secondSubmission = submission_2.copy(actions = submission.actions)

        repo.save(secondSubmission).failed.futureValue

        val submissionsInDB = repo.findAllSubmissionsForEori(eori).futureValue
        submissionsInDB.length must be(1)
        submissionsInDB.head must equal(submission)
      }
    }
  }

  "Submission Repository on updateMrn" should {

    "return empty Option" when {
      "there is no Submission with given ConversationId" in {
        val newMrn = mrn_2
        repo.updateMrn(conversationId, newMrn).futureValue mustNot be(defined)
      }
    }

    "return Submission updated" when {
      "there is a Submission containing Action with given ConversationId" in {
        repo.save(submission).futureValue
        val newMrn = mrn_2
        val expectedUpdatedSubmission = submission.copy(mrn = Some(newMrn))

        val updatedSubmission = repo.updateMrn(conversationId, newMrn).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }

      "new MRN is the same as the old one" in {
        repo.save(submission).futureValue

        val updatedSubmission = repo.updateMrn(conversationId, mrn).futureValue

        updatedSubmission.value must equal(submission)
      }
    }
  }

  "Submission Repository on addAction" when {

    "there is no Submission with given MRN" should {
      "return empty Option" in {
        val newAction = Action(CancellationRequest, conversationId_2)
        repo.addAction(mrn, newAction).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given MRN" should {
      "return Submission updated" in {
        repo.save(submission).futureValue
        val newAction = Action(CancellationRequest, conversationId_2)
        val expectedUpdatedSubmission = submission.copy(actions = submission.actions :+ newAction)

        val updatedSubmission = repo.addAction(mrn, newAction).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }
    }
  }

  "Submission Repository on findAllSubmissionsByEori" when {

    "there is no Submission associated with this EORI" should {
      "return empty List" in {
        repo.findAllSubmissionsForEori(eori).futureValue must equal(Seq.empty)
      }
    }

    "there is single Submission associated with this EORI" should {
      "return this Submission only" in {
        repo.save(submission).futureValue

        val retrievedSubmissions = repo.findAllSubmissionsForEori(eori).futureValue

        retrievedSubmissions.size must equal(1)
        retrievedSubmissions.headOption.value must equal(submission)
      }
    }

    "there are multiple Submissions associated with this EORI" should {
      "return all the Submissions" in {
        repo.save(submission).futureValue
        repo.save(submission_2).futureValue
        repo.save(submission_3).futureValue

        val retrievedSubmissions = repo.findAllSubmissionsForEori(eori).futureValue

        retrievedSubmissions.size must equal(3)
        retrievedSubmissions must contain(submission)
        retrievedSubmissions must contain(submission_2)
        retrievedSubmissions must contain(submission_3)
        retrievedSubmissions mustBe inOrder(submission, submission_3, submission_2)
      }
    }
  }

  "Submission Repository on findSubmissionByMrn" when {

    "there is no Submission with given MRN" should {
      "return empty Option" in {
        repo.findSubmissionByMrn(mrn).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given MRN" should {
      "return this Submission" in {
        repo.save(submission).futureValue

        val retrievedSubmission = repo.findSubmissionByMrn(mrn).futureValue

        retrievedSubmission.value must equal(submission)
      }
    }
  }

  "Submission Repository on findSubmissionByConversationId" when {

    "there is no Submission containing Action with given ConversationId" should {
      "return empty Option" in {
        repo.findSubmissionByConversationId(conversationId).futureValue mustNot be(defined)
      }
    }

    "there is a Submission containing Action with given ConversationId" should {
      "return this Submission" in {
        repo.save(submission).futureValue

        val retrievedSubmission = repo.findSubmissionByConversationId(conversationId).futureValue

        retrievedSubmission.value must equal(submission)
      }
    }
  }

  "findSubmissionByUuid" when {

    "no matching submission exists" should {
      "return None" in {
        repo.findSubmissionByUuid(eori, uuid).futureValue mustBe None
      }
    }

    "part matching submission exists" should {
      "return None" in {
        repo.save(submission).futureValue
        repo.findSubmissionByUuid("other", uuid).futureValue mustBe None
        repo.findSubmissionByUuid(eori, "other").futureValue mustBe None
      }
    }

    "matching submission exists" should {
      "return this Some" in {
        repo.save(submission).futureValue

        repo.findSubmissionByUuid(eori, uuid).futureValue mustBe Some(submission)
      }
    }
  }

}
