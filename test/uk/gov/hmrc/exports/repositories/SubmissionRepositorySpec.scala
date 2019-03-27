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

package uk.gov.hmrc.exports.repositories

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.base.{CustomsExportsBaseSpec, ExportsTestData}
import uk.gov.hmrc.exports.models.{CancellationRequestExists, CancellationRequested, MissingDeclaration, Submission}

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionRepositorySpec extends CustomsExportsBaseSpec with ExportsTestData with BeforeAndAfterEach {

  override lazy val app: Application = GuiceApplicationBuilder().build()
  val repo: SubmissionRepository = component[SubmissionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.save(submission).futureValue
  }

  override def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll().futureValue
  }

  "Submission repository" should {

    "be able to get submission by eori" in {
      val found = repo.findByEori(eori).futureValue

      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)
      found.head.mrn must be(Some(mrn))
      found.head.lrn must be(lrn)
      found.head.ducr must be(Some(ducr))
    }

    "be able to get submission by conversationId" in {
      val found = repo.getByConversationId(conversationId).futureValue.get

      found.eori must be(eori)
      found.conversationId must be(conversationId)
      found.mrn must be(Some(mrn))
      found.lrn must be(lrn)
      found.ducr must be(Some(ducr))
    }

    "be able to get submission by eori and mrn" in {
      val found = repo.getByEoriAndMrn(eori, mrn).futureValue.get

      found.eori must be(eori)
      found.conversationId must be(conversationId)
      found.mrn must be(Some(mrn))
      found.lrn must be(lrn)
      found.ducr must be(Some(ducr))
    }

    "be able to update submission" in {
      val submissionToUpdate = Submission("eori", "conversationId", Some("ducr"), Some("lrn"), Some("mrn"), status = "01")

      repo.save(submissionToUpdate).futureValue must be(true)

      val oldFound = repo.getByConversationId("conversationId").futureValue.get

      oldFound.mrn must be(Some("mrn"))
      oldFound.status must be("01")

      val updatedSubmission = oldFound.copy(mrn = Some("newMrn"), status = "02")

      repo.updateSubmission(updatedSubmission).futureValue must be(true)

      val newFound = repo.getByConversationId("conversationId").futureValue.get

      newFound.mrn must be(Some("newMrn"))
      newFound.status must be("02")
    }

    "not update when old submission not exist" in {
      val submissionToUpdate = Submission("654321", "654321", Some("ducr"), Some("lrn"), Some("mrn"), status = "01")

      repo.updateSubmission(submissionToUpdate).futureValue must be(false)
    }

    //TODO: add return status when declaration is actually cancelled
    "be able to cancel declaration" in {
      repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequested)

      repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequestExists)

      repo.cancelDeclaration("incorrect", "incorrect").futureValue must be(MissingDeclaration)
    }

    "update mrn and status for existing submission" in {
      repo.updateMrnAndStatus(eori, conversationId, mrn, Some("NewStatus")).futureValue must be(true)
    }

    "update mrn and status without new status for existing submission" in {
      repo.updateMrnAndStatus(eori, conversationId, mrn,  None).futureValue must be(false)
    }
  }
}
