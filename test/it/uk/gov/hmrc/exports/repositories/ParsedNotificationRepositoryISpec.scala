/*
 * Copyright 2023 HM Revenue & Customs
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

import org.bson.types.ObjectId
import testdata.ExportsTestData._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.IntegrationTestSpec

class ParsedNotificationRepositoryISpec extends IntegrationTestSpec {

  private val repository = instanceOf[ParsedNotificationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
  }

  "Notification Repository on insert" when {

    "the operation was successful" should {
      "result in a success" in {
        repository.insertOne(notification).futureValue.isRight mustBe true

        val notificationInDB = repository.findAll("actionId", actionId).futureValue
        notificationInDB.length must equal(1)
        notificationInDB.head must equal(notification)
      }
    }

    "trying to save the same Notification twice" should {
      "be allowed" in {
        repository.insertOne(notification).futureValue.isRight mustBe true
        repository.insertOne(notification.copy(_id = ObjectId.get)).futureValue.isRight mustBe true
      }

      "result in having 2 notifications persisted" in {
        repository.insertOne(notification).futureValue.isRight mustBe true
        repository.insertOne(notification.copy(_id = ObjectId.get)).futureValue.isRight mustBe true

        val notificationsInDB = repository.findAll("actionId", notification.actionId).futureValue
        notificationsInDB.length must equal(2)
        notificationsInDB.head must equal(notification)
      }
    }
  }

  "Notification Repository on findAll" when {

    "there is no Notification with given actionId" should {
      "return empty list" in {
        repository.findAll("actionId", actionId).futureValue must equal(Seq.empty)
      }
    }

    "there is single Notification with given actionId" should {
      "return this Notification only" in {
        repository.insertOne(notification).futureValue

        val foundNotifications = repository.findAll("actionId", actionId).futureValue

        foundNotifications.length must equal(1)
        foundNotifications.head must equal(notification)
      }
    }

    "there are multiple Notifications with given actionId" should {
      "return all the Notifications" in {
        repository.insertOne(notification).futureValue
        repository.insertOne(notification_2).futureValue

        val foundNotifications = repository.findAll("actionId", actionId).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }
  }

  "Notification Repository on findNotifications" when {

    "there is no Notification for any given actionId" should {
      "return empty list" in {
        repository.findNotifications(Seq(actionId, actionId_2)).futureValue must equal(Seq.empty)
      }
    }

    "there are Notifications for one of provided actionIds" should {
      "return those Notifications" in {
        repository.insertOne(notification).futureValue
        repository.insertOne(notification_2).futureValue

        val foundNotifications =
          repository.findNotifications(Seq(actionId, actionId_2)).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }

    "there are Notifications with different actionIds" should {
      "return only Notifications with conversationId same as provided one" in {
        repository.insertOne(notification).futureValue
        repository.insertOne(notification_2).futureValue
        repository.insertOne(notification_3).futureValue

        val foundNotifications = repository.findNotifications(Seq(actionId_2)).futureValue

        foundNotifications.length must equal(1)
        foundNotifications must contain(notification_3)
      }
    }

    "there are Notifications for all of provided actionIds" should {
      "return all the Notifications" in {
        repository.insertOne(notification).futureValue
        repository.insertOne(notification_2).futureValue
        repository.insertOne(notification_3).futureValue

        val foundNotifications =
          repository.findNotifications(Seq(actionId, actionId_2)).futureValue

        foundNotifications.length must equal(3)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
        foundNotifications must contain(notification_3)
      }
    }

    "the input is an empty list" should {
      "return empty list" in {
        repository.insertOne(notification).futureValue
        repository.insertOne(notification_2).futureValue
        repository.insertOne(notification_3).futureValue

        repository.findNotifications(Seq.empty).futureValue must equal(Seq.empty)
      }
    }
  }

  "Notification Repository on findLatestNotification" should {
    "return the latest Notification" in {
      val actionId = notification.actionId
      repository.insertOne(notification).futureValue
      repository.insertOne(notification_2.copy(actionId = actionId)).futureValue
      repository.insertOne(notification_3.copy(actionId = actionId)).futureValue

      val latestNotification = repository.findLatestNotification(actionId).futureValue.value

      latestNotification.details.dateTimeIssued mustBe notification_3.details.dateTimeIssued
      latestNotification.details.errors mustBe Seq.empty
    }
  }
}
