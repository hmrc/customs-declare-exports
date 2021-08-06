/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.repositories

import java.util.UUID

import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.core.errors.DatabaseException
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import testdata.ExportsTestData._
import testdata.SubmissionTestData._
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, CancellationRequest, SubmissionRequest}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionRepositorySpec extends IntegrationTestBaseSpec {

  private val repo: SubmissionRepository = GuiceApplicationBuilder().configure(mongoConfiguration).injector.instanceOf[SubmissionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.removeAll().futureValue
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
        exc.getMessage must include(s"E11000 duplicate key error collection: ${TestMongoDB.DatabaseName}.submissions index: actionIdIdx dup key")
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

    "allow save two submissions with empty actions" in {
      repo.save(emptySubmission_1).futureValue must be(emptySubmission_1)
      repo.save(emptySubmission_2).futureValue must be(emptySubmission_2)
      repo.findAllSubmissionsForEori(eori).futureValue must have length 2
    }
  }

  "Submission Repository on updateMrn" should {

    "return empty Option" when {
      "there is no Submission with given ConversationId" in {
        val newMrn = mrn_2
        repo.updateMrn(actionId, newMrn).futureValue mustNot be(defined)
      }
    }

    "return Submission updated" when {
      "there is a Submission containing Action with given ConversationId" in {
        repo.save(submission).futureValue
        val newMrn = mrn_2
        val expectedUpdatedSubmission = submission.copy(mrn = Some(newMrn))

        val updatedSubmission = repo.updateMrn(actionId, newMrn).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }

      "new MRN is the same as the old one" in {
        repo.save(submission).futureValue

        val updatedSubmission = repo.updateMrn(actionId, mrn).futureValue

        updatedSubmission.value must equal(submission)
      }
    }
  }

  "Submission Repository on addAction" when {

    "there is no Submission with given MRN" should {
      "return empty Option" in {
        val newAction = Action(actionId_2, CancellationRequest)
        repo.addAction(mrn, newAction).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given MRN" should {
      "return Submission updated" in {
        repo.save(submission).futureValue
        val newAction = Action(actionId_2, CancellationRequest)
        val expectedUpdatedSubmission = submission.copy(actions = submission.actions :+ newAction)

        val updatedSubmission = repo.addAction(mrn, newAction).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }
    }
  }

  "Submission Repository on addAction" when {
    val action = Action(UUID.randomUUID().toString, SubmissionRequest)
    "there is no submission" should {
      "return failed future with IllegalStateException" in {
        an[IllegalStateException] mustBe thrownBy {
          Await.result(repo.addAction(submission, action), patienceConfig.timeout)
        }
      }
    }
    "there is submission" should {
      "add action at end of sequence" in {
        val savedSubmission = repo.save(submission).futureValue
        repo.addAction(savedSubmission, action).futureValue
        val result = repo.findSubmissionById(savedSubmission.eori, savedSubmission.uuid).futureValue.value
        result.actions.map(_.id) must contain(action.id)
      }
    }
  }

  "Submission Repository on findOrCreate" when {
    "there is submission" should {
      "return existing submission" in {
        repo.save(submission_2).futureValue
        val result = repo.findOrCreate(Eori(submission_2.eori), submission_2.uuid, submission).futureValue
        result.actions mustEqual submission_2.actions
      }
    }
    "there no submission" should {
      "insert provided submission" in {
        val result = repo.findOrCreate(Eori(submission_2.eori), submission_2.uuid, submission).futureValue
        result.actions mustEqual submission.actions
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
        retrievedSubmissions must contain inOrder (submission, submission_3, submission_2)
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

  "Submission Repository on findSubmissionById" when {

    "no matching submission exists" should {
      "return None" in {
        repo.findSubmissionById(eori, uuid).futureValue mustBe None
      }
    }

    "part matching submission exists" should {
      "return None" in {
        repo.save(submission).futureValue
        repo.findSubmissionById("other", uuid).futureValue mustBe None
        repo.findSubmissionById(eori, "other").futureValue mustBe None
      }
    }

    "matching submission exists" should {
      "return this Some" in {
        repo.save(submission).futureValue

        repo.findSubmissionById(eori, uuid).futureValue mustBe Some(submission)
      }
    }
  }

  "Submission Repository on findSubmissionByDucr" when {

    "there is no Submission with given DUCR for the given EORI" should {
      "return empty Option" in {
        repo.findSubmissionByDucr(eori, ducr).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given DUCR for the given EORI" should {
      "return this Submission" in {
        repo.save(submission).futureValue

        val retrievedSubmission = repo.findSubmissionByDucr(eori, ducr).futureValue

        retrievedSubmission.value must equal(submission)
      }
    }
  }
}
