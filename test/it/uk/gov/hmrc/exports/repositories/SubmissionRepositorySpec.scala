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

import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.core.errors.DatabaseException
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import testdata.ExportsTestData._
import testdata.SubmissionTestData._
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec
import uk.gov.hmrc.exports.models.declaration.submissions._

import java.util.UUID
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

        val submissionInDB = repo.findBy(eori, SubmissionQueryParameters(Some(uuid))).futureValue
        submissionInDB.headOption must be(defined)
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

        val submissionsInDB = repo.findBy(eori, SubmissionQueryParameters()).futureValue
        submissionsInDB.length must be(1)
        submissionsInDB.head must equal(submission)
      }
    }

    "allow save two submissions with empty actions" in {
      repo.save(emptySubmission_1).futureValue must be(emptySubmission_1)
      repo.save(emptySubmission_2).futureValue must be(emptySubmission_2)
      repo.findBy(eori, SubmissionQueryParameters()).futureValue must have length 2
    }
  }
/*
  "Submission Repository on setMrnIfMissing" should {

    "return empty Option" when {
      "there is no Submission with given ConversationId" in {
        val newMrn = mrn_2
        repo.updateMrn(actionId, newMrn).futureValue mustNot be(defined)
      }
    }

    "return Submission updated" when {
      "there is a Submission containing Action with given ConversationId and MRN is None" in {
        repo.save(submission.copy(mrn = None)).futureValue
        val newMrn = mrn_2
        val expectedUpdatedSubmission = submission.copy(mrn = Some(newMrn))

        val updatedSubmission = repo.updateMrn(actionId, newMrn).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }
    }
  }
*/
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

        val result = repo.findBy(savedSubmission.eori, SubmissionQueryParameters(uuid = Some(savedSubmission.uuid))).futureValue

        result mustNot be(empty)
        result.head.actions.map(_.id) must contain(action.id)
      }
    }
  }

  "Submission Repository on findSubmissionByMrnAndEori" when {

    "there is no Submission with given MRN" should {
      "return empty Option" in {
        repo.findSubmissionByMrnAndEori(mrn, eori).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given MRN" that {
      "has different eori should return empty Option" in {
        repo.save(submission).futureValue

        repo.findSubmissionByMrnAndEori(mrn, "wrong").futureValue mustNot be(defined)
      }

      "has correct eori should return this Submission" in {
        repo.save(submission).futureValue

        val retrievedSubmission = repo.findSubmissionByMrnAndEori(mrn, eori).futureValue

        retrievedSubmission.value must equal(submission)
      }
    }
  }

  "SubmissionRepository on findBy" when {

    "querying with empty SubmissionQueryParameters" should {
      "return all Submissions for given EORI" in {

        repo.save(submission).futureValue
        repo.save(submission_2).futureValue

        val queryParams = SubmissionQueryParameters()

        val result = repo.findBy(submission.eori, queryParams).futureValue

        result mustBe Seq(submission, submission_2)
      }
    }

    "querying by UUID only" when {

      "there is no Submission with given UUID for the given EORI" should {
        "return empty Sequence" in {

          val queryParams = SubmissionQueryParameters(uuid = Some(uuid))

          repo.findBy(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with given UUID for the given EORI" should {
        "return this Submission" in {

          repo.save(submission).futureValue
          val queryParams = SubmissionQueryParameters(uuid = Some(submission.uuid))

          val retrievedSubmissions = repo.findBy(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }

    "querying by DUCR only" when {

      "there is no Submission with given DUCR for the given EORI" should {
        "return empty Sequence" in {

          val queryParams = SubmissionQueryParameters(ducr = Some(ducr))

          repo.findBy(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with given DUCR for the given EORI" should {
        "return this Submission" in {

          repo.save(submission).futureValue
          val queryParams = SubmissionQueryParameters(ducr = Some(submission.ducr))

          val retrievedSubmissions = repo.findBy(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }

    "querying by LRN only" when {

      "there is no Submission with given MRN for the given EORI" should {
        "return empty Sequence" in {

          val queryParams = SubmissionQueryParameters(lrn = Some(lrn))

          repo.findBy(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with given MRN for the given EORI" should {
        "return this Submission" in {

          repo.save(submission).futureValue
          val queryParams = SubmissionQueryParameters(lrn = Some(submission.lrn))

          val retrievedSubmissions = repo.findBy(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }
  }
}
