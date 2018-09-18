/*
 * Copyright 2018 HM Revenue & Customs
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

import uk.gov.hmrc.exports.base.CustomsExportsBaseSpec
import uk.gov.hmrc.mongo.ReactiveRepository
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionRepositorySpec extends CustomsExportsBaseSpec with BeforeAndAfterEach {
  
  override protected def afterEach(): Unit = {
    super.afterEach()
    repositories.foreach { repo =>
      repo.removeAll()
    }
  }

  val repo = component[SubmissionRepository]

   val repositories: Seq[ReactiveRepository[_, _]] = Seq(repo)

  "repo" should {
    "save declaration with EORI and timestamp" in {
      /*
      The first time an declaration is submitted, we save it with the user's EORI, their LRN (if provided)
      and the conversation ID we received from the customs-declarations API response, generating a timestamp to record
      when this occurred.
       */
      val eori = randomString(8)
      val lrn = Some(randomString(70))
      val mrn = randomString(16)
      val conversationId = randomString(80)

      val before = System.currentTimeMillis()
      val submission = Submission(eori, conversationId, lrn, Some(mrn))
      repo.insert(submission).futureValue.ok must be(true)

      // we can now display a list of all the declarations belonging to the current user, searching by EORI
      val found = repo.findByEori(eori).futureValue
      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)
      found.head.lrn must be(lrn)
      found.head.mrn must be(Some(mrn))

      // a timestamp has been generated representing "creation time" of case class instance
      found.head.submittedTimestamp must (be >= before).and(be <= System.currentTimeMillis())

      // we can also retrieve the submission individually by conversation ID
      val got = repo.getByConversationId(conversationId).futureValue
      got.get.eori must be(eori)
      got.get.conversationId must be(conversationId)
      got.get.lrn must be(lrn)
      got.get.mrn must be(Some(mrn))

      // or we can retrieve it by eori and MRN
      val gotAgain = repo.getByEoriAndMrn(eori, mrn).futureValue
      gotAgain.get.eori must be(eori)
      gotAgain.get.conversationId must be(conversationId)
      gotAgain.get.lrn must be(lrn)
      gotAgain.get.mrn must be(Some(mrn))
    }
  }
}
