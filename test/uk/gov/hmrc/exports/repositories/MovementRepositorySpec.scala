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

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.base.{CustomsExportsBaseSpec, ExportsTestData}

import scala.concurrent.ExecutionContext.Implicits.global

class MovementRepositorySpec extends CustomsExportsBaseSpec with BeforeAndAfterEach with ExportsTestData {

  override protected def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll()
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()
  val repo = component[MovementsRepository]

  "MovementsRepository" should {
    "save movement with EORI, DUCR and timestamp" in {
      repo.save(movement).futureValue must be(true)

      // we can now display a list of all the movements belonging to the current user, searching by EORI
      val found = repo.findByEori(eori).futureValue
      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)
      found.head.ducr must be(ducr)

      // a timestamp has been generated representing "creation time" of case class instance
      found.head.submittedTimestamp must (be >= before).and(be <= System.currentTimeMillis())

      // we can also retrieve the movement individually by conversation ID
      val got = repo.getByConversationId(conversationId).futureValue
      got.get.eori must be(eori)
      got.get.conversationId must be(conversationId)
      got.get.ducr must be(ducr)

      // or we can retrieve it by eori and MRN
      val gotAgain = repo.getByEoriAndDucr(eori, ducr).futureValue
      gotAgain.get.eori must be(eori)
      gotAgain.get.conversationId must be(conversationId)
      gotAgain.get.ducr must be (ducr)

      // update status test
      val movement1 = repo.getByConversationId(conversationId).futureValue

      val updatedMovement = movement1.get.copy(status = Some("Accepted"))
      val updateStatusResult = repo.updateMovementStatus(updatedMovement).futureValue
      updateStatusResult must be(true)
      val newMovement = repo.getByConversationId(conversationId).futureValue

      newMovement.get must be(updatedMovement)
    }
  }
}
