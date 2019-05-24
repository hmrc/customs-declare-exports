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
import uk.gov.hmrc.exports.repositories.NotificationsRepository
import unit.uk.gov.hmrc.exports.base.CustomsExportsBaseSpec
import util.ExportsTestData

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationsRepositorySpec extends CustomsExportsBaseSpec with ExportsTestData with BeforeAndAfterEach {

  override protected def afterEach(): Unit = {

    super.afterEach()
    repo.removeAll().futureValue
  }

  override lazy val app: Application = GuiceApplicationBuilder().build()
  private val repo = component[NotificationsRepository]

  "Notifications repository" should {

    "save notification and retrieve it by EORI" in {

      repo.save(notification).futureValue must be(true)
      val found = repo.findByEori(eori).futureValue

      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)
      found.head.dateTimeReceived.compareTo(now) must be(0)
    }

    "save notification and retrieve it by conversationId" in {

      repo.save(notification).futureValue must be(true)
      val found = repo.getByConversationId(conversationId).futureValue

      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)
      found.head.dateTimeReceived.compareTo(now) must be(0)
    }

    "save notification and retrieve it by both EORI and conversationId" in {

      repo.save(notification).futureValue must be(true)
      val found = repo.getByEoriAndConversationId(eori, conversationId).futureValue

      found.length must be(1)
      found.head.eori must be(eori)
      found.head.conversationId must be(conversationId)
      found.head.dateTimeReceived.compareTo(now) must be(0)
    }

    "save two notifications and retrive both" in {

      repo.save(notification).futureValue must be(true)
      val first = repo.getByEoriAndConversationId(eori, conversationId).futureValue

      first.length must be(1)
      first.head.eori must be(eori)
      first.head.conversationId must be(conversationId)
      first.head.dateTimeReceived.compareTo(now) must be(0)
      first.head.response.head.functionalReferenceId must be(Some("123"))

      repo.save(notification2).futureValue must be(true)
      val second = repo.getByEoriAndConversationId(eori, conversationId).futureValue

      second.length must be(2)
      second(1).eori must be(eori)
      second(1).conversationId must be(conversationId)
      second(1).dateTimeReceived.compareTo(now) must be(0)
      second(1).response.head.functionalReferenceId must be(Some("456"))
    }
  }
}
