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

  "Submission Repository on saveSubmission" when {

    "the operation was successful" should {
      "return true" in {
        repo.save(submission).futureValue must be(true)

        val submissionFromDB = repo.findSubmissionByMrn(eori, mrn).futureValue

        submissionFromDB must be(defined)
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

  "Submission Repository on updateStatus" should {

    "return empty Option" when {
      "there is no Submission with given ConversationId" in {
        val newStatus = "02"
        repo.updateStatus(eori, conversationId)(newStatus).futureValue mustNot be(defined)
      }
    }

    "return Submission updated" when {
      "there is a Submission containing Action with given ConversationId" in {
        repo.save(submission).futureValue
        val newStatus = "02"
        val expectedUpdatedSubmission = submission.copy(status = newStatus)

        val updatedSubmission = repo.updateStatus(eori, conversationId)(newStatus).futureValue

        updatedSubmission.value must equal(expectedUpdatedSubmission)
      }

      "new Status is the same as the previous one" in {
        repo.save(submission).futureValue

        val updatedSubmission = repo.updateStatus(eori, conversationId)(submissionStatus).futureValue

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
        repo.findAllSubmissionsByEori(eori).futureValue must equal(Seq.empty)
      }
    }

    "there is single Submission associated with this EORI" should {
      "return this Submission only" in {
        repo.save(submission).futureValue

        val retrievedSubmissions = repo.findAllSubmissionsByEori(eori).futureValue

        retrievedSubmissions.size must equal(1)
        retrievedSubmissions.headOption.value must equal(submission)
      }
    }

    "there are multiple Submissions associated with this EORI" should {
      "return all the Submissions" in {
        repo.save(submission).futureValue
        repo.save(submission_2).futureValue

        val retrievedSubmissions = repo.findAllSubmissionsByEori(eori).futureValue

        retrievedSubmissions.size must equal(2)
        retrievedSubmissions must contain(submission)
        retrievedSubmissions must contain(submission_2)
      }
    }
  }

  "Submission Repository on findSubmissionByMrn" when {

    "there is no Submission with given MRN" should {
      "return empty Option" in {
        repo.findSubmissionByMrn(eori, mrn).futureValue mustNot be(defined)
      }
    }

    "there is a Submission with given MRN" should {
      "return this Submission" in {
        repo.save(submission).futureValue

        val retrievedSubmission = repo.findSubmissionByMrn(eori, mrn).futureValue

        retrievedSubmission.value must equal(submission)
      }
    }
  }

  "Submission Repository on findSubmissionByConversationId" when {

    "there is no Submission containing Action with given ConversationId" should {
      "return empty Option" in {
        repo.findSubmissionByConversationId(eori, conversationId).futureValue mustNot be(defined)
      }
    }

    "there is a Submission containing Action with given ConversationId" should {
      "return this Submission" in {
        repo.save(submission).futureValue

        val retrievedSubmission = repo.findSubmissionByConversationId(eori, conversationId).futureValue

        retrievedSubmission.value must equal(submission)
      }
    }
  }

}

object SubmissionRepositorySpec {
  import util.TestDataHelper._

  val uuid: String = UUID.randomUUID().toString
  val uuid_2: String = UUID.randomUUID().toString
  val eori: String = "GB167676"
  val ducr: String = randomAlphanumericString(16)
  val lrn: String = randomAlphanumericString(22)
  val mrn: String = "MRN87878797"
  val mrn_2: String = "MRN12341234"
  val submissionStatus: String = "Pending"
  val conversationId: String = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"
  val conversationId_2: String = "b1c09f1b-7c94-4e90-b754-7c5c71c55e22"

  lazy val action = Action(requestType = "Submission", conversationId = conversationId)
  lazy val action_2 = Action(requestType = "Submission", conversationId = conversationId_2)

  lazy val submission: Submission = Submission(
    uuid = uuid,
    eori = eori,
    lrn = lrn,
    status = submissionStatus,
    mrn = Some(mrn),
    ducr = Some(ducr),
    actions = Seq(action)
  )
  lazy val submission_2: Submission = Submission(
    uuid = uuid_2,
    eori = eori,
    lrn = lrn,
    status = submissionStatus,
    mrn = Some(mrn_2),
    ducr = Some(ducr),
    actions = Seq(action_2)
  )

}


//"Submission repository" should {
//
//  "retrieve all submissions for EORI" in {
//  repo.save(submission).futureValue
//  repo.save(submission.copy(uuid = UUID.randomUUID().toString, ducr = None, mrn = None)).futureValue
//
//  val foundSubmissions = repo.findByEori(eori).futureValue
//
//  foundSubmissions.length must be(2)
//
//  foundSubmissions.head.eori must be(eori)
//  foundSubmissions.head.ducr must be(Some(ducr))
//  foundSubmissions.head.lrn must be(lrn)
//  foundSubmissions.head.mrn must be(Some(mrn))
//  foundSubmissions.head.status must be(submissionStatus)
//
//  //      foundSubmissions.head.conversationId must be(conversationId)
//}
//
//  "retrieve submission by conversationId" in {
//  repo.save(submission).futureValue
//  val found = repo.getByConversationId(conversationId).futureValue.get
//
//  found.eori must be(eori)
//  //      found.conversationId must be(conversationId)
//  found.mrn must be(Some(mrn))
//  found.lrn must be(lrn)
//  found.ducr must be(Some(ducr))
//}
//
//  "retrieve submission by EORI and MRN" in {
//  repo.save(submission).futureValue
//  val found = repo.getByEoriAndMrn(eori, mrn).futureValue.get
//
//  found.eori must be(eori)
//  //      found.conversationId must be(conversationId)
//  found.mrn must be(Some(mrn))
//  found.lrn must be(lrn)
//  found.ducr must be(Some(ducr))
//}
//
//  "save submission" in {
//  repo.save(submission).futureValue must be(true)
//
//  val returnedSubmission = repo.getByConversationId(conversationId).futureValue.get
//
//  returnedSubmission must equal(submission)
//}
//
//  "update submission" in {
//  repo.save(submission).futureValue must be(true)
//  val returnedSubmission = repo.getByConversationId(conversationId).futureValue.get
//
//  returnedSubmission must equal(submission)
//
//  val updatedSubmission = returnedSubmission.copy(mrn = Some("newMrn"), status = "02")
//  repo.updateSubmission(updatedSubmission).futureValue must be(true)
//  val returnedUpdatedSubmission = repo.getByConversationId(conversationId).futureValue.get
//
//  returnedUpdatedSubmission must equal(updatedSubmission)
//}
//
//  "update MRN and status" in {
//  repo.save(submission).futureValue must be(true)
//
//  repo.updateMrnAndStatus(eori, conversationId, "newMRN", Some("02")).futureValue must be(true)
//
//  val returnedUpdatedSubmission = repo.getByConversationId(conversationId).futureValue.get
//  returnedUpdatedSubmission.mrn must be(defined)
//  returnedUpdatedSubmission.mrn.get must equal("newMRN")
//  returnedUpdatedSubmission.status must equal("02")
//}
//
//  "not update when old submission does not exist" in {
//  val submissionToUpdate = submission.copy(mrn = Some("newMRN"), status = "02")
//
//  repo.updateSubmission(submissionToUpdate).futureValue must be(false)
//}
//
//  "not update MRN and status when new status is None" in {
//  repo.save(submission).futureValue
//  repo.updateMrnAndStatus(eori, conversationId, mrn, None).futureValue must be(false)
//
//  val found = repo.findByEori(eori).futureValue
//  found.head.status must be(submissionStatus)
//}
//
//  "be able to cancel declaration" in {
//  repo.save(submission).futureValue
//  repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequested)
//}
//
//  "return Cancellation Request Exists status if the declaration has already been cancelled" in {
//  repo.save(submission).futureValue
//  repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequested)
//  repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequestExists)
//}
//
//  "return Missing Declaration status when trying to cancel non existing declaration" in {
//  repo.cancelDeclaration("incorrect", "incorrect").futureValue must be(MissingDeclaration)
//}
//
//  //TODO: add return status when declaration is actually cancelled
//}