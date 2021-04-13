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

class NotificationRepositorySpec extends IntegrationTestBaseSpec {

  private val repo = GuiceApplicationBuilder().configure(mongoConfiguration).injector.instanceOf[NotificationRepository]

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

    "trying to save an unparsable notification" should {
      "be allowed" in {
        repo.insert(notificationUnparsed).futureValue.ok must be(true)

        val notificationInDB = repo.findUnparsedNotifications().futureValue
        notificationInDB.length must equal(1)
        notificationInDB.head must equal(notificationUnparsed)
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
        repo.insert(notificationUnparsed).futureValue

        repo.findNotificationsByActionIds(Seq.empty).futureValue must equal(Seq.empty)
      }
    }
  }

  "Notification Repository on findNotificationsByMrn" when {

    "there is no Notification with given mrn" should {
      "return empty list" in {
        repo.findNotificationsByMrn(mrn).futureValue must equal(Seq.empty)
      }
    }

    "there is single Notification with given mrn" should {
      "return this Notification only" in {
        repo.insert(notification).futureValue

        val foundNotifications = repo.findNotificationsByMrn(mrn).futureValue

        foundNotifications.length must equal(1)
        foundNotifications.head must equal(notification)
      }
    }

    "there are multiple Notifications with given mrn" should {
      "return all the Notifications" in {
        repo.insert(notification).futureValue
        repo.insert(notification_2).futureValue

        val foundNotifications = repo.findNotificationsByMrn(mrn).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }
  }

  "Notification Repository on findUnparsedNotifications" when {

    "there are no unparsed Notification" should {
      "return empty list" in {
        repo.insert(notification).futureValue

        repo.findUnparsedNotifications().futureValue must equal(Seq.empty)
      }
    }

    "there is one unparsed Notification" should {
      "return those Notifications" in {
        repo.insert(notification).futureValue
        repo.insert(notificationUnparsed).futureValue

        val foundNotifications = repo.findUnparsedNotifications().futureValue

        foundNotifications.length must equal(1)
        foundNotifications must contain(notificationUnparsed)
      }
    }

    "there are many unparsed Notification" should {
      "return those Notifications" in {
        val anotherUnparsableNotification = notificationUnparsed.copy(_id = BSONObjectID.generate(), actionId = actionId_3)

        repo.insert(notification).futureValue
        repo.insert(notificationUnparsed).futureValue
        repo.insert(anotherUnparsableNotification).futureValue

        val foundNotifications = repo.findUnparsedNotifications().futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notificationUnparsed)
        foundNotifications must contain(anotherUnparsableNotification)
      }
    }
  }

  "Notification Repository on removeNotificationsForActionId" when {

    "there are no Notifications for the given actionId" should {
      "not delete any notifications" in {
        repo.insert(notification_3).futureValue

        repo.removeUnparsedNotificationsForActionId(notification.actionId).futureValue

        repo.findNotificationsByActionId(notification_3.actionId).futureValue mustBe Seq(notification_3)
      }
    }

    "there is one Notification for the given actionId" should {
      "delete just that matching notification" in {
        repo.insert(notificationUnparsed).futureValue
        repo.insert(notification_3).futureValue

        repo.removeUnparsedNotificationsForActionId(notificationUnparsed.actionId).futureValue

        repo.findNotificationsByActionId(notificationUnparsed.actionId).futureValue mustBe Seq.empty
        repo.findNotificationsByActionId(notification_3.actionId).futureValue mustBe Seq(notification_3)
      }
    }

    "there are multiple Notifications for the given actionId" should {
      "delete all matching notifications" in {
        repo.insert(notificationUnparsed).futureValue
        repo.insert(notificationUnparsed.copy(_id = BSONObjectID.generate(), payload = "<something></else>")).futureValue
        repo.insert(notification_3).futureValue

        repo.removeUnparsedNotificationsForActionId(notificationUnparsed.actionId).futureValue

        repo.findNotificationsByActionId(notificationUnparsed.actionId).futureValue mustBe Seq.empty
        repo.findNotificationsByActionId(notification_3.actionId).futureValue mustBe Seq(notification_3)
      }
    }

    "there are multiple Notifications for the given actionId, some of them have been parsed" should {
      "delete only matching actionId notifications that are unparsed" in {
        val parsedNotificationWithSameActionId = notification_3.copy(_id = BSONObjectID.generate(), actionId = notificationUnparsed.actionId)

        repo.insert(notificationUnparsed).futureValue
        repo.insert(notificationUnparsed.copy(_id = BSONObjectID.generate(), payload = "<something></else>")).futureValue
        repo.insert(parsedNotificationWithSameActionId).futureValue
        repo.insert(notification_3).futureValue

        repo.removeUnparsedNotificationsForActionId(notificationUnparsed.actionId).futureValue

        repo.findNotificationsByActionId(notificationUnparsed.actionId).futureValue mustBe Seq(parsedNotificationWithSameActionId)
        repo.findNotificationsByActionId(notification_3.actionId).futureValue mustBe Seq(notification_3)
      }
    }
  }
}
