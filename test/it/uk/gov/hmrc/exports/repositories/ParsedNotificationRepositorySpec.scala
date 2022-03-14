/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.bson.BSONObjectID
import stubs.TestMongoDB.mongoConfiguration
import testdata.ExportsTestData._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec

import scala.concurrent.ExecutionContext.Implicits.global

class ParsedNotificationRepositorySpec extends IntegrationTestBaseSpec {

  private val repo = GuiceApplicationBuilder().configure(mongoConfiguration).injector.instanceOf[ParsedNotificationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.removeAll().futureValue
  }

  "Notification Repository on insert" when {

    "the operation was successful" should {
      "result in a success" in {
        repo.insert(notification).futureValue.ok must be(true)

        val notificationInDB = repo.findNotificationsByActionId(actionId).futureValue
        notificationInDB.length must equal(1)
        notificationInDB.head must equal(notification)
      }
    }

    "trying to save the same Notification twice" should {
      "be allowed" in {
        repo.insert(notification).futureValue.ok must be(true)
        repo.insert(notification.copy(_id = BSONObjectID.generate())).futureValue.ok must be(true)
      }

      "result in having 2 notifications persisted" in {
        repo.insert(notification).futureValue.ok must be(true)
        repo.insert(notification.copy(_id = BSONObjectID.generate())).futureValue.ok must be(true)

        val notificationsInDB = repo.findNotificationsByActionId(notification.actionId).futureValue
        notificationsInDB.length must equal(2)
        notificationsInDB.head must equal(notification)
      }
    }
  }

  "Notification Repository on findLatestNotification" when {

    "no actionsIds are given" should {
      "return None" in {
        repo.findLatestNotification(Seq.empty).futureValue must equal(None)
      }
    }

    "there is no Notification with the given actionId" should {
      "return None" in {
        repo.findLatestNotification(Seq(actionId)).futureValue must equal(None)
      }
    }

    "there is a Notification with the given actionId" should {
      "return it" in {
        repo.insert(notification).futureValue
        repo.findLatestNotification(Seq(actionId)).futureValue.head must equal(notification)
      }
    }

    "there are multiple Notifications with given actionId" should {
      "return the last received Notification" in {
        repo.insert(notification).futureValue
        repo.insert(notification_2).futureValue
        repo.insert(notification_3).futureValue

        repo.findLatestNotification(Seq(actionId, actionId_2, actionId_3)).futureValue.head must equal(notification_3)
      }
    }
  }

  "Notification Repository on findNotificationsByActionId" when {

    "there is no Notification with given actionId" should {
      "return empty list" in {
        repo.findNotificationsByActionId(actionId).futureValue must equal(Seq.empty)
      }
    }

    "there is single Notification with given actionId" should {
      "return this Notification only" in {
        repo.insert(notification).futureValue

        val foundNotifications = repo.findNotificationsByActionId(actionId).futureValue

        foundNotifications.length must equal(1)
        foundNotifications.head must equal(notification)
      }
    }

    "there are multiple Notifications with given actionId" should {
      "return all the Notifications" in {
        repo.insert(notification).futureValue
        repo.insert(notification_2).futureValue

        val foundNotifications = repo.findNotificationsByActionId(actionId).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }
  }

  "Notification Repository on findNotificationsByActionIds" when {

    "there is no Notification for any given actionId" should {
      "return empty list" in {
        repo.findNotificationsByActionIds(Seq(actionId, actionId_2)).futureValue must equal(Seq.empty)
      }
    }

    "there are Notifications for one of provided actionIds" should {
      "return those Notifications" in {
        repo.insert(notification).futureValue
        repo.insert(notification_2).futureValue

        val foundNotifications =
          repo.findNotificationsByActionIds(Seq(actionId, actionId_2)).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }

    "there are Notifications with different actionIds" should {
      "return only Notifications with conversationId same as provided one" in {
        repo.insert(notification).futureValue
        repo.insert(notification_2).futureValue
        repo.insert(notification_3).futureValue

        val foundNotifications = repo.findNotificationsByActionIds(Seq(actionId_2)).futureValue

        foundNotifications.length must equal(1)
        foundNotifications must contain(notification_3)
      }
    }

    "there are Notifications for all of provided actionIds" should {
      "return all the Notifications" in {
        repo.insert(notification).futureValue
        repo.insert(notification_2).futureValue
        repo.insert(notification_3).futureValue

        val foundNotifications =
          repo.findNotificationsByActionIds(Seq(actionId, actionId_2)).futureValue

        foundNotifications.length must equal(3)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
        foundNotifications must contain(notification_3)
      }
    }

    "the input is an empty list" should {
      "return empty list" in {
        repo.insert(notification).futureValue
        repo.insert(notification_2).futureValue
        repo.insert(notification_3).futureValue

        repo.findNotificationsByActionIds(Seq.empty).futureValue must equal(Seq.empty)
      }
    }
  }
}
