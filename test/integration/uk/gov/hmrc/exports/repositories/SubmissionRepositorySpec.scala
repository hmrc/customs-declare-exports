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

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.models.declaration.{Action, Submission}
import uk.gov.hmrc.exports.repositories.SubmissionRepository

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionRepositorySpec
    extends WordSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with ScalaFutures with MustMatchers
    with OptionValues {

  import SubmissionRepositorySpec._

  override lazy val app: Application = GuiceApplicationBuilder().build()
  private val repo: SubmissionRepository = app.injector.instanceOf[SubmissionRepository]

  implicit val ec: ExecutionContext = global

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.removeAll().futureValue
  }

  override def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll().futureValue
  }

  "Submission Repository on save" when {

    "the operation was successful" should {
      "return true" in {
        repo.save(submission).futureValue must be(true)

        val submissionInDB = repo.findSubmissionByMrn(mrn).futureValue
        submissionInDB must be(defined)
      }
    }
  }

  "Submission Repository on updateMrn" should {

    "return empty Option" when {
      "there is no Submission with given ConversationId" in {
        val newMrn = mrn_2
        repo.updateMrn(eori, conversationId)(newMrn).futureValue mustNot be(defined)
      }
    }

    "return Submission updated" when {
      "there is a Submission containing Action with given ConversationId" in {
        repo.save(submission).futureValue
        val newMrn = mrn_2
        val expectedUpdatedSubmission = submission.copy(mrn = Some(newMrn))

        val updatedSubmission = repo.updateMrn(eori, conversationId)(newMrn).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }

      "new MRN is the same as the old one" in {
        repo.save(submission).futureValue

        val updatedSubmission = repo.updateMrn(eori, conversationId)(mrn).futureValue

        updatedSubmission.value must equal(submission)
      }
    }
  }

  "Submission Repository on addAction" when {

    "there is no Submission with given MRN" should {
      "return empty Option" in {
        val newAction = Action("Cancellation", conversationId_2)
        repo.addAction(eori, mrn)(newAction).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given MRN" should {
      "return Submission updated" in {
        repo.save(submission).futureValue
        val newAction = Action("Cancellation", conversationId_2)
        val expectedUpdatedSubmission = submission.copy(actions = submission.actions :+ newAction)

        val updatedSubmission = repo.addAction(eori, mrn)(newAction).futureValue

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

        val retrievedSubmissions = repo.findAllSubmissionsForEori(eori).futureValue

        retrievedSubmissions.size must equal(2)
        retrievedSubmissions must contain(submission)
        retrievedSubmissions must contain(submission_2)
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

  "Submission Repository on findSubmissionByUuid" when {

    "there is no Submission containing Action with given ConversationId" should {
      "return empty Option" in {
        repo.findSubmissionByUuid(uuid).futureValue mustNot be(defined)
      }
    }

    "there is a Submission containing Action with given ConversationId" should {
      "return this Submission" in {
        repo.save(submission).futureValue

        val retrievedSubmission = repo.findSubmissionByUuid(uuid).futureValue

        retrievedSubmission.value must equal(submission)
      }
    }
  }

}

object SubmissionRepositorySpec {
  import util.testdata.TestDataHelper._

  val uuid: String = UUID.randomUUID().toString
  val uuid_2: String = UUID.randomUUID().toString
  val eori: String = "GB167676"
  val ducr: String = randomAlphanumericString(16)
  val lrn: String = randomAlphanumericString(22)
  val mrn: String = "MRN87878797"
  val mrn_2: String = "MRN12341234"
  val conversationId: String = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"
  val conversationId_2: String = "b1c09f1b-7c94-4e90-b754-7c5c71c55e22"

  lazy val action = Action(requestType = "Submission", conversationId = conversationId)
  lazy val action_2 = Action(requestType = "Submission", conversationId = conversationId_2)

  lazy val submission: Submission = Submission(
    uuid = uuid,
    eori = eori,
    lrn = lrn,
    mrn = Some(mrn),
    ducr = Some(ducr),
    actions = Seq(action)
  )
  lazy val submission_2: Submission = Submission(
    uuid = uuid_2,
    eori = eori,
    lrn = lrn,
    mrn = Some(mrn_2),
    ducr = Some(ducr),
    actions = Seq(action_2)
  )

}
