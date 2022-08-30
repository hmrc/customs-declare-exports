/*
 * Copyright 2022 HM Revenue & Customs
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

import repositories.DuplicateKey
import testdata.ExportsTestData._
import testdata.SubmissionTestData._
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models.declaration.submissions._

class SubmissionRepositoryISpec extends IntegrationTestSpec {

  private val repository = instanceOf[SubmissionRepository]

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
    }

    "allow to insert two submissions with empty actions" in {
      repository.insertOne(emptySubmission_1).futureValue.isRight mustBe true
      repository.insertOne(emptySubmission_2).futureValue.isRight mustBe true
      repository.findAll(eori, SubmissionQueryParameters()).futureValue must have length 2
    }
  }

  "Submission Repository on addAction" when {

    "there is no Submission with the given MRN" should {
      "return an empty Option" in {
        val newAction = Action(actionId_2, CancellationRequest)
        repository.addAction(mrn, newAction).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with the given MRN" should {
      "return the Submission updated" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        val newAction = Action(actionId_2, CancellationRequest)
        val expectedUpdatedSubmission = submission.copy(actions = submission.actions :+ newAction)

        val updatedSubmission = repository.addAction(mrn, newAction).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
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

  "SubmissionRepository on find" when {
    "there is no Submission with given Id for the given EORI" should {
      "return None" in {

        repository.find(eori, "123").futureValue mustBe None
      }
    }

    "there is a Submission with given Id for the given EORI" should {
      "return Some submission entity" in {
        repository.insertOne(submission).futureValue.isRight mustBe true

        repository.find(submission.eori, submission.uuid).futureValue mustBe Some(submission)
      }
    }
  }
}
