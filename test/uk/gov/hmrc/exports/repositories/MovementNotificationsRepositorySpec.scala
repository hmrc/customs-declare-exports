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
import uk.gov.hmrc.mongo.ReactiveRepository

class MovementNotificationsRepositorySpec extends CustomsExportsBaseSpec with BeforeAndAfterEach with ExportsTestData {

  override protected def afterEach(): Unit = {
    super.afterEach()
    repositories.foreach { repo =>
      repo.removeAll()
    }
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()

  val repo = component[MovementNotificationsRepository]

  val repositories: Seq[ReactiveRepository[_, _]] = Seq(repo)

  "Movement notifications repository" should {
    "save notification with EORI, conversationID and timestamp" in {
      repo.save(movementNotification).futureValue must be (true)

      // we can now display a list of all the declarations belonging to the current user, searching by EORI
      val found = repo.findByEori(eori).futureValue
      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)

      found.head.dateTimeReceived must be (now)

      // we can also retrieve the submission individually by conversation Id
      val got = repo.getByConversationId(conversationId).futureValue
      got.get.eori must be(eori)
      got.get.conversationId must be(conversationId)

      // or we can retrieve it by eori and conversationId
      val gotAgain = repo.getByEoriAndConversationId(eori, conversationId).futureValue
      gotAgain.get.eori must be(eori)
      gotAgain.get.conversationId must be(conversationId)
    }
  }
}
