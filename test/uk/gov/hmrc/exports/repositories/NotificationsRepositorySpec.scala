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

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationsRepositorySpec extends CustomsExportsBaseSpec with ExportsTestData with BeforeAndAfterEach {

  override protected def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll()
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()

  val repo = component[NotificationsRepository]

  // TODO: possibly split the tests, as it is too high level
  "Notifications repository" should {
    "save notification with eori, conversationId and timestamp" in {
      repo.save(notification).futureValue must be(true)

      // we can now display a list of all the declarations belonging to the current user, searching by EORI
      val found = repo.findByEori(eori).futureValue
      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)

      found.head.dateTimeReceived.compareTo(now) must be(0)

      // we can also retrieve the submission individually by conversation Id
      val got = repo.getByConversationId(conversationId).futureValue.head
      got.eori must be(eori)
      got.conversationId must be(conversationId)

      // or we can retrieve it by eori and conversationId
      val gotAgain = repo.getByEoriAndConversationId(eori, conversationId).futureValue.head
      gotAgain.eori must be(eori)
      gotAgain.conversationId must be(conversationId)
    }
  }
}
