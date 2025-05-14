/*
 * Copyright 2024 HM Revenue & Customs
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

import testdata.ExportsTestData._
import testdata.SubmissionTestData._
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models.FetchSubmissionPageData
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup._
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.SubmissionRepositoryISpecHelper.itemsPerPage
import uk.gov.hmrc.exports.util.TimeUtils

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZonedDateTime}
import java.util.UUID
import scala.util.Random

class SubmissionRepositoryISpec extends IntegrationTestSpec {

  private val repository = instanceOf[SubmissionRepository]

  private def fetchData(reverse: Boolean = false): FetchSubmissionPageData =
    FetchSubmissionPageData(List.empty, reverse = reverse, limit = itemsPerPage, uuid = uuid)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
  }

  "SubmissionRepository.addAction" when {

    "there is no Submission with the given UUID" should {
      "return an empty Option" in {
        val newAction = Action(actionId_2, CancellationRequest, decId = Some(""), versionNo = 0)
        repository.addAction(uuid, newAction).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with the given UUID" should {
      "return the Submission updated" in {
        repository.insertOne(submission).futureValue.isRight mustBe true
        val newAction = Action(actionId_2, CancellationRequest, submission.latestDecId, submission.latestVersionNo + 1)

        val updatedSubmission = repository.addAction(submission.uuid, newAction).futureValue

        val expectedUpdatedSubmission = submission.copy(actions = submission.actions :+ newAction, lastUpdated = updatedSubmission.value.lastUpdated)
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

        val submissionInDB = repository.findAll("eori", eori).futureValue
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
      repository.findAll("eori", eori).futureValue must have length 2
    }
  }

  import uk.gov.hmrc.exports.repositories.SubmissionRepositoryISpecHelper._

  private def genSubmissions(statusGroup: StatusGroup, size: Int = itemsPerPage * 2, descending: Boolean = true): Seq[Submission] = {
    val lastStatusUpdate = TimeUtils.now()
    val statuses = toEnhancedStatus(statusGroup).toList

    val submissions = (0 until size).map { seconds =>
      genSubmission(lastStatusUpdate.minusSeconds(seconds.toLong), statuses(Random.nextInt(statuses.length)))
    }.toList

    repository.bulkInsert(submissions).futureValue mustBe size

    submissions.sortWith { case (s1, s2) =>
      val date1 = s1.enhancedStatusLastUpdated
      val date2 = s2.enhancedStatusLastUpdated
      if (date1 != date2) {
        if (descending) date1.isAfter(date2) else date1.isBefore(date2)
      } else {
        if (descending) s1.uuid > s2.uuid else s1.uuid < s2.uuid
      }
    }

  }

  "SubmissionRepository.fetchFirstPage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchFirstPage("eori", SubmittedStatuses, fetchData()).futureValue mustBe Seq.empty
    }

    List(false, true).foreach { descending =>
      val order = if (descending) "descending" else "ascending"

      s"return the first page of Submissions for the given EORI, statusGroup and statusLastUpdated in $order order" in {
        genSubmissions(CancelledStatuses) // don't fetch from these
        val allSubmissions = genSubmissions(SubmittedStatuses, descending = descending) // fetch the 1st page from these instead
        val submissions = repository.fetchFirstPage("eori", SubmittedStatuses, fetchData(!descending)).futureValue
        (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(ix))
      }
    }
  }

  "SubmissionRepository.fetchLastPage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchLastPage("eori", SubmittedStatuses, fetchData()).futureValue mustBe Seq.empty
    }

    List(false, true).foreach { descending =>
      val order = if (descending) "descending" else "ascending"

      s"return the last page of Submissions for the given EORI, statusGroup and statusLastUpdated in $order order" in {
        genSubmissions(CancelledStatuses) // don't fetch from these
        val allSubmissions = genSubmissions(SubmittedStatuses, itemsPerPage * 3, descending = descending) // fetch the last page from these instead
        val submissions = repository.fetchLastPage("eori", SubmittedStatuses, fetchData(!descending)).futureValue
        (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(itemsPerPage * 2 + ix))
      }
    }
  }

  "SubmissionRepository.fetchLoosePage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchLoosePage("eori", SubmittedStatuses, 2, fetchData()).futureValue mustBe Seq.empty
    }

    List(false, true).foreach { descending =>
      val order = if (descending) "descending" else "ascending"

      s"return a loose page of Submissions for the given EORI, statusGroup and statusLastUpdated in $order order" in {
        genSubmissions(CancelledStatuses) // don't fetch from these
        val allSubmissions = genSubmissions(SubmittedStatuses, itemsPerPage * 4, descending = descending) // fetch a loose page from these instead
        val submissions = repository.fetchLoosePage("eori", SubmittedStatuses, 3, fetchData(!descending)).futureValue
        (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(itemsPerPage * 2 + ix))
      }
    }
  }

  "SubmissionRepository.fetchNextPage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchNextPage("eori", SubmittedStatuses, TimeUtils.now(), fetchData()).futureValue mustBe Seq.empty
    }

    List(false, true).foreach { descending =>
      val order = if (descending) "descending" else "ascending"

      s"return the next page of Submissions for the given EORI, statusGroup and statusLastUpdated in $order order" in {
        genSubmissions(CancelledStatuses) // don't fetch from these
        val allSubmissions = genSubmissions(SubmittedStatuses, itemsPerPage * 4, descending = descending) // fetch the next page from these instead

        // Assume current page is 2
        val lastSubmissionOfPage2 = allSubmissions(itemsPerPage * 2 - 1)
        val dateTimeOfLastSubmissionOfPage2 = lastSubmissionOfPage2.enhancedStatusLastUpdated
        val uuidOfLastSubmissionOfPage2 = lastSubmissionOfPage2.uuid

        val fetchDataWithUuid = FetchSubmissionPageData(
          statusGroups = fetchData(!descending).statusGroups,
          reverse = fetchData(!descending).reverse,
          limit = fetchData(!descending).limit,
          uuid = uuidOfLastSubmissionOfPage2
        )
        val submissions = repository
          .fetchNextPage("eori", SubmittedStatuses, dateTimeOfLastSubmissionOfPage2, fetchDataWithUuid)
          .futureValue
        (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(itemsPerPage * 2 + ix))
      }
    }
  }

  "SubmissionRepository.fetchPreviousPage" should {

    "return an empty Sequence when there no Submissions for the given EORI and statusGroup" in {
      repository.fetchPreviousPage("eori", SubmittedStatuses, TimeUtils.now(), fetchData()).futureValue mustBe Seq.empty
    }

    List(false, true).foreach { descending =>
      val order = if (descending) "descending" else "ascending"

      s"return the previous page of Submissions for the given EORI, statusGroup and statusLastUpdated in $order order" in {
        genSubmissions(CancelledStatuses) // don't fetch from these
        val allSubmissions =
          genSubmissions(SubmittedStatuses, itemsPerPage * 4, descending = descending) // fetch the previous page from these instead

        // Assume current page is 3
        val firstSubmissionOfPage3 = allSubmissions(itemsPerPage * 2)
        val dateTimeOfFirstSubmissionOfPage3 = firstSubmissionOfPage3.enhancedStatusLastUpdated
        val uuidOfFirstSubmissionOfPage3 = firstSubmissionOfPage3.uuid

        val fetchDataWithUuid = FetchSubmissionPageData(
          statusGroups = fetchData(!descending).statusGroups,
          reverse = fetchData(!descending).reverse,
          limit = fetchData(!descending).limit,
          uuid = uuidOfFirstSubmissionOfPage3
        )
        val submissions = repository
          .fetchPreviousPage("eori", SubmittedStatuses, dateTimeOfFirstSubmissionOfPage3, fetchDataWithUuid)
          .futureValue
          // For the 'fetchPreviousPage' test only, it's required to reverse the resulting submissions.
          // The service does this in SubmissionService.
          .reverse

        (0 until itemsPerPage).foreach(ix => submissions(ix) mustBe allSubmissions(itemsPerPage + ix))
      }
    }
  }

  "SubmissionRepository.createdAt" should {
    "provide the insertion time of a Document" in {
      repository.insertOne(submission).futureValue.isRight mustBe true

      val createdAt = repository.createdAt("uuid", submission.uuid).futureValue
      createdAt.value.truncatedTo(ChronoUnit.DAYS) mustBe Instant.now.truncatedTo(ChronoUnit.DAYS)
    }
  }
}

object SubmissionRepositoryISpecHelper {

  val itemsPerPage = 4
  val lrnLength = 22

  val mrn = "mrn"
  val ducr = "ducr"

  val dateTime = TimeUtils.now().withNano(111000000)

  def genSubmission(lastStatusUpdate: ZonedDateTime, status: EnhancedStatus = RECEIVED): Submission = {
    val uuid = UUID.randomUUID.toString
    Submission(
      uuid = uuid,
      eori = "eori",
      lrn = Random.alphanumeric.take(lrnLength).mkString.toUpperCase,
      mrn = Some(mrn),
      ducr = ducr,
      latestEnhancedStatus = status,
      enhancedStatusLastUpdated = lastStatusUpdate,
      actions = List(Action(uuid, SubmissionRequest, decId = Some(""), versionNo = 1, None, dateTime)),
      latestDecId = Some(uuid)
    )
  }
}
