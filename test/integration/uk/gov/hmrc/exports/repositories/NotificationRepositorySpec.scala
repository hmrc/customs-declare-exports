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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.exports.repositories.NotificationRepository
import util.testdata.ExportsTestData._
import util.testdata.NotificationTestData._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class NotificationRepositorySpec
    extends WordSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with ScalaFutures with MustMatchers
    with OptionValues {

  override lazy val app: Application = GuiceApplicationBuilder().build()
  private val repo = app.injector.instanceOf[NotificationRepository]

  implicit val ec: ExecutionContext = global

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.removeAll().futureValue
  }

  override def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll().futureValue
  }

  "Notification Repository on save" when {

    "the operation was successful" should {
      "return true" in {
        repo.save(notification).futureValue must be(true)

        val notificationInDB = repo.findNotificationsByConversationId(conversationId).futureValue
        notificationInDB.length must equal(1)
        notificationInDB.head must equal(notification)
      }
    }

    //TODO if there is a requirement to reduce / elimate duplicate notifications these tests and the
    // indexes will need changing. for now these tests have been changed to represent the current /
    // expected behavior. Which is allow duplicates.
    "trying to save the same Notification twice" should {
      "be allowed" in {
        repo.save(notification).futureValue must be(true)

        repo.save(notification).futureValue must be(true)
      }
    }

    "result in having 2 notifications persisted" in {
      repo.save(notification).futureValue must be(true)

      repo.save(notification).futureValue

      val notificationsInDB = repo.findNotificationsByConversationId(notification.conversationId).futureValue
      notificationsInDB.length must equal(2)
      notificationsInDB.head must equal(notification)
    }
  }

  "Notification Repository on findNotificationsByConversationId" when {

    "there is no Notification with given conversationId" should {
      "return empty list" in {
        repo.findNotificationsByConversationId(conversationId).futureValue must equal(Seq.empty)
      }
    }

    "there is single Notification with given conversationId" should {
      "return this Notification only" in {
        repo.save(notification).futureValue

        val foundNotifications = repo.findNotificationsByConversationId(conversationId).futureValue

        foundNotifications.length must equal(1)
        foundNotifications.head must equal(notification)
      }
    }

    "there are multiple Notifications with given conversationId" should {
      "return all the Notifications" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue

        val foundNotifications = repo.findNotificationsByConversationId(conversationId).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }
  }

  "Notification Repository on findNotificationsByConversationIds" when {

    "there is no Notification for any given conversationId" should {
      "return empty list" in {
        repo.findNotificationsByConversationIds(Seq(conversationId, conversationId_2)).futureValue must equal(Seq.empty)
      }
    }

    "there are Notifications for one of provided conversationIds" should {
      "return those Notifications" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue

        val foundNotifications =
          repo.findNotificationsByConversationIds(Seq(conversationId, conversationId_2)).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }

    "there are Notifications with different conversationIds" should {
      "return only Notifications with conversationId same as provided one" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue
        repo.save(notification_3).futureValue

        val foundNotifications = repo.findNotificationsByConversationIds(Seq(conversationId_2)).futureValue

        foundNotifications.length must equal(1)
        foundNotifications must contain(notification_3)
      }
    }

    "there are Notifications for all of provided conversationIds" should {
      "return all the Notifications" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue
        repo.save(notification_3).futureValue

        val foundNotifications =
          repo.findNotificationsByConversationIds(Seq(conversationId, conversationId_2)).futureValue

        foundNotifications.length must equal(3)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
        foundNotifications must contain(notification_3)
      }
    }

    "the input is an empty list" should {
      "return empty list" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue
        repo.save(notification_3).futureValue

        repo.findNotificationsByConversationIds(Seq.empty).futureValue must equal(Seq.empty)
      }
    }
  }

}
