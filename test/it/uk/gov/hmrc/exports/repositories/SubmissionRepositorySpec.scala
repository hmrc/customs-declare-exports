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
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.RECEIVED
import uk.gov.hmrc.exports.models.declaration.submissions._

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionRepositorySpec extends IntegrationTestBaseSpec {

  private val repository: SubmissionRepository =
    GuiceApplicationBuilder().configure(mongoConfiguration).injector.instanceOf[SubmissionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll().futureValue
  }

  "Submission Repository on save" when {

    "the operation was successful" should {
      "return true" in {
        repository.save(submission).futureValue must be(submission)

        val submissionInDB = repository.findBy(eori, SubmissionQueryParameters(Some(uuid))).futureValue
        submissionInDB.headOption must be(defined)
      }
    }

    "trying to save Submission with the same conversationId twice" should {
      "throw DatabaseException" in {
        repository.save(submission).futureValue must be(submission)
        val secondSubmission = submission_2.copy(actions = submission.actions)

        val exc = repository.save(secondSubmission).failed.futureValue

        exc mustBe an[DatabaseException]
        exc.getMessage must include(s"E11000 duplicate key error collection: ${TestMongoDB.DatabaseName}.submissions index: actionIdIdx dup key")
      }

      "result in having only the first Submission persisted" in {
        repository.save(submission).futureValue must be(submission)
        val secondSubmission = submission_2.copy(actions = submission.actions)

        repository.save(secondSubmission).failed.futureValue

        val submissionsInDB = repository.findBy(eori, SubmissionQueryParameters()).futureValue
        submissionsInDB.length must be(1)
        submissionsInDB.head must equal(submission)
      }
    }

    "allow save two submissions with empty actions" in {
      repository.save(emptySubmission_1).futureValue must be(emptySubmission_1)
      repository.save(emptySubmission_2).futureValue must be(emptySubmission_2)
      repository.findBy(eori, SubmissionQueryParameters()).futureValue must have length 2
    }
  }

  "Submission Repository on updateAfterNotificationParsing" should {

    "return an empty Option" when {
      "there is no Submission with the given UUID" in {
        repository.updateAfterNotificationParsing(submission).futureValue mustNot be(defined)
      }
    }

    "return Submission updated" when {
      "there is a Submission containing Action with the given actionId and MRN is None" in {
        repository.save(submission.copy(mrn = None)).futureValue

        val dateTimeIssued = ZonedDateTime.now(ZoneId.of("UTC"))

        val updatedSubmission = repository
          .updateAfterNotificationParsing(
            submission
              .copy(latestEnhancedStatus = Some(RECEIVED), enhancedStatusLastUpdated = Some(dateTimeIssued), actions = List(action_2, action_3))
          )
          .futureValue
          .head

        updatedSubmission.mrn.value mustBe submission.mrn.value
        updatedSubmission.latestEnhancedStatus.value mustBe RECEIVED
        updatedSubmission.enhancedStatusLastUpdated.value mustBe dateTimeIssued
        updatedSubmission.actions.size mustBe 2
        updatedSubmission.actions.head mustBe action_2
        updatedSubmission.actions.last mustBe action_3
      }
    }
  }

  "Submission Repository on addAction" when {

    "there is no Submission with the given MRN" should {
      "return empty Option" in {
        val newAction = Action(actionId_2, CancellationRequest)
        repository.addAction(mrn, newAction).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with the given MRN" should {
      "return Submission updated" in {
        repository.save(submission).futureValue
        val newAction = Action(actionId_2, CancellationRequest)
        val expectedUpdatedSubmission = submission.copy(actions = submission.actions :+ newAction)

        val updatedSubmission = repository.addAction(mrn, newAction).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }
    }
  }

  "Submission Repository on addAction" when {
    val action = Action(UUID.randomUUID.toString, SubmissionRequest)
    "there is no submission" should {
      "return failed future with IllegalStateException" in {
        an[IllegalStateException] mustBe thrownBy {
          Await.result(repository.addAction(submission, action), patienceConfig.timeout)
        }
      }
    }
    "there is submission" should {
      "add action at end of sequence" in {
        val savedSubmission = repository.save(submission).futureValue
        repository.addAction(savedSubmission, action).futureValue

        val result = repository.findBy(savedSubmission.eori, SubmissionQueryParameters(uuid = Some(savedSubmission.uuid))).futureValue

        result mustNot be(empty)
        result.head.actions.map(_.id) must contain(action.id)
      }
    }
  }

  "Submission Repository on findSubmissionByMrnAndEori" when {

    "there is no Submission with the given MRN" should {
      "return empty Option" in {
        repository.findSubmissionByMrnAndEori(mrn, eori).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with the given MRN" that {
      "has different eori should return empty Option" in {
        repository.save(submission).futureValue

        repository.findSubmissionByMrnAndEori(mrn, "wrong").futureValue mustNot be(defined)
      }

      "has correct eori should return this Submission" in {
        repository.save(submission).futureValue

        val retrievedSubmission = repository.findSubmissionByMrnAndEori(mrn, eori).futureValue

        retrievedSubmission.value must equal(submission)
      }
    }
  }

  "SubmissionRepository on findBy" when {

    "querying with empty SubmissionQueryParameters" should {
      "return all Submissions for given EORI" in {

        repository.save(submission).futureValue
        repository.save(submission_2).futureValue

        val queryParams = SubmissionQueryParameters()

        val result = repository.findBy(submission.eori, queryParams).futureValue

        result mustBe Seq(submission, submission_2)
      }
    }

    "querying by UUID only" when {

      "there is no Submission with the given UUID for the given EORI" should {
        "return empty Sequence" in {

          val queryParams = SubmissionQueryParameters(uuid = Some(uuid))

          repository.findBy(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with the given UUID for the given EORI" should {
        "return this Submission" in {

          repository.save(submission).futureValue
          val queryParams = SubmissionQueryParameters(uuid = Some(submission.uuid))

          val retrievedSubmissions = repository.findBy(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }

    "querying by DUCR only" when {

      "there is no Submission with the given DUCR for the given EORI" should {
        "return empty Sequence" in {

          val queryParams = SubmissionQueryParameters(ducr = Some(ducr))

          repository.findBy(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with the given DUCR for the given EORI" should {
        "return this Submission" in {

          repository.save(submission).futureValue
          val queryParams = SubmissionQueryParameters(ducr = Some(submission.ducr))

          val retrievedSubmissions = repository.findBy(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }

    "querying by LRN only" when {

      "there is no Submission with the given MRN for the given EORI" should {
        "return empty Sequence" in {

          val queryParams = SubmissionQueryParameters(lrn = Some(lrn))

          repository.findBy(eori, queryParams).futureValue mustBe Seq.empty[Submission]
        }
      }

      "there is a Submission with the given MRN for the given EORI" should {
        "return this Submission" in {

          repository.save(submission).futureValue
          val queryParams = SubmissionQueryParameters(lrn = Some(submission.lrn))

          val retrievedSubmissions = repository.findBy(submission.eori, queryParams).futureValue

          retrievedSubmissions.length mustBe 1
          retrievedSubmissions.head mustBe submission
        }
      }
    }
  }
}
