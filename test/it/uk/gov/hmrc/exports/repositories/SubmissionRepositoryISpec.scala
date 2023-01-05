/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup._
import uk.gov.hmrc.exports.models.declaration.submissions._

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID
import scala.util.Random

class SubmissionRepositoryISpec extends IntegrationTestSpec {

  private val repository = instanceOf[SubmissionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
  }

  "SubmissionRepository.addAction" when {

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

  "SubmissionRepository.find" when {

    "there is no Submission with given Id for the given EORI" should {
      "return None" in {
        repository.findById(eori, "123").futureValue mustBe None
      }
    }

    "there is a Submission with given Id for the given EORI" should {
      "return Some submission entity" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        repository.findById(submission.eori, submission.uuid).futureValue mustBe Some(submission)
      }
    }
  }

  "SubmissionRepository.findAll" should {
    "return all Submissions for the given EORI" in {
      repository.insertOne(submission).futureValue
      repository.insertOne(submission_2).futureValue
      repository.insertOne(submission_3.copy(eori = "GB1234567")).futureValue

      val result = repository.findAll(submission.eori).futureValue
      result mustBe Seq(submission, submission_2)
    }
  }

  "SubmissionRepository.findByLrn" should {

    "return an empty sequence" when {

      "there is no Submission with given LRN" in {
        repository.findByLrn(eori, "123").futureValue mustBe Seq.empty
      }

      "there is no Submission with given LRN & EORI combination" in {
        repository.insertOne(submission.copy(eori = "1234567")).futureValue.isRight mustBe true
        repository.findByLrn(eori, submission.lrn).futureValue mustBe Seq.empty
      }
    }

    "return a sequence of matching Submission entities" when {
      "there are Submissions with given LRN & EORI combination" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        repository.findByLrn(eori, submission.lrn).futureValue mustBe Seq(submission)
      }
    }
  }

  "SubmissionRepository.save" when {

    "the operation was successful" should {
      "return true" in {
        repository.insertOne(submission).futureValue.isRight mustBe true

        val submissionInDB = repository.findAll(eori).futureValue
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
      repository.findAll(eori).futureValue must have length 2
    }
  }

  import uk.gov.hmrc.exports.repositories.SubmissionRepositoryISpecHelper._

  private def genSubmissions(statusGroup: StatusGroup, size: Int = itemsPerPage * 2): Seq[Submission] = {
    val lastStatusUpdate = ZonedDateTime.now(ZoneId.of("UTC"))
    val statuses = toEnhancedStatus(statusGroup).toList

    val submissions = (0 until size).map { seconds =>
      submission(lastStatusUpdate.minusSeconds(seconds.toLong), statuses(Random.nextInt(statuses.length)))
    }.toList

    repository.bulkInsert(submissions).futureValue mustBe size
    submissions.sortWith { case (s1, s2) => s1.enhancedStatusLastUpdated.get.isAfter(s2.enhancedStatusLastUpdated.get) }
  }

  "SubmissionRepository.fetchFirstPage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchFirstPage("eori", SubmittedStatuses, itemsPerPage).futureValue mustBe Seq.empty
    }

    "return the first page of Submissions for the given EORI, statusGroup and statusLastUpdated in descending order" in {
      genSubmissions(CancelledStatuses) // don't fetch from these
      val allSubmissions = genSubmissions(SubmittedStatuses) // fetch the 1st page from these instead
      val submissions = repository.fetchFirstPage("eori", SubmittedStatuses, itemsPerPage).futureValue
      (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(ix))
    }
  }

  "SubmissionRepository.fetchLastPage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchLastPage("eori", SubmittedStatuses, itemsPerPage).futureValue mustBe Seq.empty
    }

    "return the last page of Submissions for the given EORI, statusGroup and statusLastUpdated in descending order" in {
      genSubmissions(CancelledStatuses) // don't fetch from these
      val allSubmissions = genSubmissions(SubmittedStatuses, itemsPerPage * 3) // fetch the last page from these instead
      val submissions = repository.fetchLastPage("eori", SubmittedStatuses, itemsPerPage).futureValue
      (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(itemsPerPage * 2 + ix))
    }
  }

  "SubmissionRepository.fetchLoosePage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchLoosePage("eori", SubmittedStatuses, 2, itemsPerPage).futureValue mustBe Seq.empty
    }

    "return a loose page of Submissions for the given EORI, statusGroup and statusLastUpdated in descending order" in {
      genSubmissions(CancelledStatuses) // don't fetch from these
      val allSubmissions = genSubmissions(SubmittedStatuses, itemsPerPage * 4) // fetch a loose page from these instead
      val submissions = repository.fetchLoosePage("eori", SubmittedStatuses, 3, itemsPerPage).futureValue
      (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(itemsPerPage * 2 + ix))
    }
  }

  "SubmissionRepository.fetchNextPage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchNextPage("eori", SubmittedStatuses, ZonedDateTime.now, itemsPerPage).futureValue mustBe Seq.empty
    }

    "return the next page of Submissions for the given EORI, statusGroup and statusLastUpdated in descending order" in {
      genSubmissions(CancelledStatuses) // don't fetch from these
      val allSubmissions = genSubmissions(SubmittedStatuses, itemsPerPage * 4) // fetch the next page from these instead

      // Assume current page is 2
      val dateTimeOfLastSubmissionOfPage2 = allSubmissions(itemsPerPage * 2 - 1).enhancedStatusLastUpdated.get

      val submissions = repository.fetchNextPage("eori", SubmittedStatuses, dateTimeOfLastSubmissionOfPage2, itemsPerPage).futureValue
      (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(itemsPerPage * 2 + ix))
    }
  }

  "SubmissionRepository.fetchPreviousPage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchPreviousPage("eori", SubmittedStatuses, ZonedDateTime.now, itemsPerPage).futureValue mustBe Seq.empty
    }

    "return the previous page of Submissions for the given EORI, statusGroup and statusLastUpdated in descending order" in {
      genSubmissions(CancelledStatuses) // don't fetch from these
      val allSubmissions = genSubmissions(SubmittedStatuses, itemsPerPage * 4) // fetch the previous page from these instead

      // Assume current page is 3
      val dateTimeOfFirstSubmissionOfPage3 = allSubmissions(itemsPerPage * 2).enhancedStatusLastUpdated.get

      val submissions = repository.fetchPreviousPage("eori", SubmittedStatuses, dateTimeOfFirstSubmissionOfPage3, itemsPerPage).futureValue
      (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(itemsPerPage + ix))
    }
  }
}

object SubmissionRepositoryISpecHelper {

  val itemsPerPage = 4
  val lrnLength = 22

  val mrn = "mrn"
  val ducr = "ducr"

  val dateTime = ZonedDateTime.now(ZoneId.of("UTC")).withNano(111000000)

  def submission(lastStatusUpdate: ZonedDateTime, status: EnhancedStatus = RECEIVED): Submission = {
    val uuid = UUID.randomUUID.toString
    Submission(
      uuid = uuid,
      eori = "eori",
      lrn = Random.alphanumeric.take(lrnLength).mkString.toUpperCase,
      mrn = Some(mrn),
      ducr = ducr,
      latestEnhancedStatus = Some(status),
      enhancedStatusLastUpdated = Some(lastStatusUpdate),
      actions = List(Action(uuid, SubmissionRequest, dateTime, None))
    )
  }
}
