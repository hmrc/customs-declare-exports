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

import org.scalatest.BeforeAndAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.base.{CustomsExportsBaseSpec, ExportsTestData}
import uk.gov.hmrc.exports.models.{CancellationRequestExists, CancellationRequested, MissingDeclaration, Submission}

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionRepositorySpec extends CustomsExportsBaseSpec with BeforeAndAfterAll with ExportsTestData {

  override lazy val app: Application = GuiceApplicationBuilder().build()
  val repo = component[SubmissionRepository]

  override def beforeAll(): Unit = {
    super.beforeAll()
    repo.removeAll()

    repo.save(submission).futureValue
  }

  override def afterAll(): Unit = {
    super.afterAll()
    repo.removeAll()
  }

  "SubmissionRepository" should {

    "find submission by eori" in {
      val found = repo.findByEori(eori).futureValue

      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)
      found.head.mrn must be(Some(mrn))
      found.head.lrn must be(lrn)
      found.head.ducr must be(ducr)
    }

    "get by conversationId" in {
      val found = repo.getByConversationId(conversationId).futureValue.get

      found.eori must be(eori)
      found.conversationId must be(conversationId)
      found.mrn must be(Some(mrn))
      found.lrn must be(lrn)
      found.ducr must be(ducr)
    }

    "get by eori and mrn" in {
      val found = repo.getByEoriAndMrn(eori, mrn).futureValue.get

      found.eori must be(eori)
      found.conversationId must be(conversationId)
      found.mrn must be(Some(mrn))
      found.lrn must be(lrn)
      found.ducr must be(ducr)
    }

    "update submission" in {
      val submissionToUpdate = Submission("eori", "conversationId", "ducr", Some("lrn"), Some("mrn"), status = "01")

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

    "cancel declaration" in {
      repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequested)

      repo.cancelDeclaration(eori, mrn).futureValue must be(CancellationRequestExists)

      repo.cancelDeclaration("incorrect", "incorrect").futureValue must be(MissingDeclaration)
    }
  }
}
