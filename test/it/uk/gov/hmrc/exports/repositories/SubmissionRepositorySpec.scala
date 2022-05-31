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

import org.scalatest.EitherValues
import play.api.libs.json.Json
import repositories.DuplicateKey
import testdata.ExportsTestData._
import testdata.SubmissionTestData._
import uk.gov.hmrc.exports.base.IntegrationTestMongoSpec
import uk.gov.hmrc.exports.models.declaration.submissions._

import scala.concurrent.Future

class SubmissionRepositorySpec extends IntegrationTestMongoSpec with EitherValues {

  private val repository = getRepository[SubmissionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
  }

  "Submission Repository on save" when {

    "the operation was successful" should {
      "return true" in {
        repository.insertOne(submission).futureValue.isRight mustBe true

        val submissionInDB = repository.findAll(eori, SubmissionQueryParameters(Some(submission.uuid))).futureValue
        submissionInDB.headOption must be(defined)
      }
    }

    "trying to save Submission with the same actionId twice" should {

      "return a 'DuplicateKey' error" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        val secondSubmission = submission_2.copy(actions = submission.actions)

        val result = repository.insertOne(secondSubmission).futureValue

        result.isLeft mustBe true
        result.left.value.isInstanceOf[DuplicateKey] mustBe true
      }

      "result in having only the first Submission persisted" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        val secondSubmission = submission_2.copy(actions = submission.actions)

        repository.insertOne(secondSubmission).futureValue

        val submissionsInDB = repository.findAll(eori, SubmissionQueryParameters()).futureValue
        submissionsInDB.length must be(1)
        submissionsInDB.head must equal(submission)
      }
    }

    "allow insertOne two submissions with empty actions" in {
      repository.insertOne(emptySubmission_1).futureValue.isRight mustBe true
      repository.insertOne(emptySubmission_2).futureValue.isRight mustBe true
      repository.findAll(eori, SubmissionQueryParameters()).futureValue must have length 2
    }
  }

  "Submission Repository on setMrnIfMissing" should {

    "return empty Option" when {
      "there is no Submission with given actionId" in {
        val newMrn = mrn_2
        repository.updateMrn(actionId, newMrn).futureValue mustNot be(defined)
      }
    }

    "return Submission updated" when {
      "there is a Submission containing Action with given actionId and MRN is None" in {
        repository.insertOne(submission.copy(mrn = None)).futureValue.isRight mustBe true
        val newMrn = mrn_2
        val expectedUpdatedSubmission = submission.copy(mrn = Some(newMrn))

        val updatedSubmission = repository.updateMrn(actionId, newMrn).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }
    }
  }

  "Submission Repository on addAction" when {

    "there is no Submission with given MRN" should {
      "return empty Option" in {
        val newAction = Action(actionId_2, CancellationRequest)
        repository.addAction(mrn, newAction).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given MRN" should {
      "return Submission updated" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        val newAction = Action(actionId_2, CancellationRequest)
        val expectedUpdatedSubmission = submission.copy(actions = submission.actions :+ newAction)

        val updatedSubmission = repository.addAction(mrn, newAction).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }
    }
  }

  "Submission Repository on findSubmissionByMrnAndEori" when {

    def find(eori: String = "wrong"): Future[Option[Submission]] = repository.findOne(Json.obj("eori" -> eori, "mrn" -> mrn))

    "there is no Submission with given MRN" should {
      "return empty Option" in {
        find(eori).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given MRN" that {
      "has different eori should return empty Option" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        find().futureValue mustNot be(defined)
      }

      "has correct eori should return this Submission" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        find(eori).futureValue.value must equal(submission)
      }
    }
  }

  "SubmissionRepository on findBy" when {

    "querying with empty SubmissionQueryParameters" should {
      "return all Submissions for given EORI" in {
        repository.insertOne(submission).futureValue
        repository.insertOne(submission_2).futureValue

        val queryParams = SubmissionQueryParameters()

        val result = repository.findAll(submission.eori, queryParams).futureValue

        result mustBe Seq(submission, submission_2)
      }
    }

    "querying by UUID only" when {

      "there is no Submission with given UUID for the given EORI" should {
        "return empty Sequence" in {
          val queryParams = SubmissionQueryParameters(uuid = Some(uuid))

          repository.findAll(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with given UUID for the given EORI" should {
        "return this Submission" in {
          repository.insertOne(submission).futureValue
          val queryParams = SubmissionQueryParameters(uuid = Some(submission.uuid))

          val retrievedSubmissions = repository.findAll(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }

    "querying by DUCR only" when {

      "there is no Submission with given DUCR for the given EORI" should {
        "return empty Sequence" in {
          val queryParams = SubmissionQueryParameters(ducr = Some(ducr))

          repository.findAll(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with given DUCR for the given EORI" should {
        "return this Submission" in {
          repository.insertOne(submission).futureValue
          val queryParams = SubmissionQueryParameters(ducr = Some(submission.ducr))

          val retrievedSubmissions = repository.findAll(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }

    "querying by LRN only" when {

      "there is no Submission with given MRN for the given EORI" should {
        "return empty Sequence" in {
          val queryParams = SubmissionQueryParameters(lrn = Some(lrn))

          repository.findAll(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with given MRN for the given EORI" should {
        "return this Submission" in {
          repository.insertOne(submission).futureValue
          val queryParams = SubmissionQueryParameters(lrn = Some(submission.lrn))

          val retrievedSubmissions = repository.findAll(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }
  }
}
