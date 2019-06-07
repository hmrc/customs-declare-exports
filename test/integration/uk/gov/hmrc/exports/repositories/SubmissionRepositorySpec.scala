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

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.models.{CancellationRequestExists, CancellationRequested, MissingDeclaration, Submission}
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import unit.uk.gov.hmrc.exports.base.CustomsExportsBaseSpec

class SubmissionRepositorySpec extends CustomsExportsBaseSpec with BeforeAndAfterEach {
  import SubmissionRepositorySpec._

  override lazy val app: Application = GuiceApplicationBuilder().build()
  private val repo: SubmissionRepository = app.injector.instanceOf[SubmissionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.removeAll().futureValue
  }

  override def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll().futureValue
  }

  "Submission repository" should {

    "be able to get submission by EORI" in {
      repo.save(submission).futureValue
      val found = repo.findByEori(eori).futureValue

      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)
      found.head.mrn must be(Some(mrn))
      found.head.lrn must be(lrn)
      found.head.ducr must be(Some(ducr))
    }

    "be able to get submission by conversationId" in {
      repo.save(submission).futureValue
      val found = repo.getByConversationId(conversationId).futureValue.get

      found.eori must be(eori)
      found.conversationId must be(conversationId)
      found.mrn must be(Some(mrn))
      found.lrn must be(lrn)
      found.ducr must be(Some(ducr))
    }

    "be able to get submission by EORI and MRN" in {
      repo.save(submission).futureValue
      val found = repo.getByEoriAndMrn(eori, mrn).futureValue.get

      found.eori must be(eori)
      found.conversationId must be(conversationId)
      found.mrn must be(Some(mrn))
      found.lrn must be(lrn)
      found.ducr must be(Some(ducr))
    }

    "be able to save submission" in {
      repo.save(submission).futureValue must be(true)

      val returnedSubmission = repo.getByConversationId(conversationId).futureValue.get

      returnedSubmission must equal(submission)
    }

    "be able to update submission" in {
      repo.save(submission).futureValue must be(true)
      val returnedSubmission = repo.getByConversationId(conversationId).futureValue.get

      returnedSubmission must equal(submission)

      val updatedSubmission = returnedSubmission.copy(mrn = Some("newMrn"), status = "02")
      repo.updateSubmission(updatedSubmission).futureValue must be(true)
      val returnedUpdatedSubmission = repo.getByConversationId(conversationId).futureValue.get

      returnedUpdatedSubmission must equal(updatedSubmission)
    }

    "be able to update MRN and status" in {
      repo.save(submission).futureValue must be(true)

      repo.updateMrnAndStatus(eori, conversationId, "newMRN", Some("02")).futureValue must be(true)

      val returnedUpdatedSubmission = repo.getByConversationId(conversationId).futureValue.get
      returnedUpdatedSubmission.mrn.value must equal("newMRN")
      returnedUpdatedSubmission.status must equal("02")
    }

    "not update when old submission does not exist" in {
      val submissionToUpdate = Submission("654321", "654321", Some("ducr"), Some("lrn"), Some("mrn"), status = "01")

      repo.updateSubmission(submissionToUpdate).futureValue must be(false)
    }

    "not update MRN and status when new status is None" in {
      repo.save(submission).futureValue
      repo.updateMrnAndStatus(eori, conversationId, mrn, None).futureValue must be(false)

      val found = repo.findByEori(eori).futureValue
      found.head.status must be("01")
    }

    "be able to cancel declaration" in {
      repo.save(submission).futureValue
      repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequested)
    }

    "return Cancellation Request Exists status if the declaration has already been cancelled" in {
      repo.save(submission).futureValue
      repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequested)
      repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequestExists)
    }

    "return Missing Declaration status when trying to cancel non existing declaration" in {
      repo.cancelDeclaration("incorrect", "incorrect").futureValue must be(MissingDeclaration)
    }

    //TODO: add return status when declaration is actually cancelled
  }
}

object SubmissionRepositorySpec {
  import util.TestDataHelper._

  val eori: String = "GB167676"
  val lrn: Option[String] = Some(randomAlphanumericString(22))
  val mrn: String = "MRN87878797"
  val mucr: String = randomAlphanumericString(16)
  val conversationId: String = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"
  val ducr: String = randomAlphanumericString(16)

  val submission: Submission = Submission(eori, conversationId, Some(ducr), lrn, Some(mrn), status = "01")

}
