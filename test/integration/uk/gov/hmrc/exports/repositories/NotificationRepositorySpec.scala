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

import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.exports.repositories.NotificationRepository
import util.testdata.ExportsTestData._
import util.testdata.NotificationTestData._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class NotificationRepositorySpec
    extends WordSpec with BeforeAndAfterEach with ScalaFutures with MustMatchers with OptionValues
    with IntegrationPatience {

  private val injector: Injector = {
    SharedMetricRegistries.clear()
    GuiceApplicationBuilder().injector()
  }

  private val repo = injector.instanceOf[NotificationRepository]

  implicit val ec: ExecutionContext = global

  override def afterEach(): Unit = {
    super.afterEach()
    repo.removeAll().futureValue
  }

  "Notification Repository on save" when {

    "the operation was successful" should {
      "return true" in {
        repo.save(notification).futureValue must be(true)

        val notificationInDB = repo.findNotificationsByActionId(actionId).futureValue
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

      val notificationsInDB = repo.findNotificationsByActionId(notification.actionId).futureValue
      notificationsInDB.length must equal(2)
      notificationsInDB.head must equal(notification)
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
        repo.save(notification).futureValue

        val foundNotifications = repo.findNotificationsByActionId(actionId).futureValue

        foundNotifications.length must equal(1)
        foundNotifications.head must equal(notification)
      }
    }

    "there are multiple Notifications with given actionId" should {
      "return all the Notifications" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue

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
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue

        val foundNotifications =
          repo.findNotificationsByActionIds(Seq(actionId, actionId_2)).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }

    "there are Notifications with different actionIds" should {
      "return only Notifications with conversationId same as provided one" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue
        repo.save(notification_3).futureValue

        val foundNotifications = repo.findNotificationsByActionIds(Seq(actionId_2)).futureValue

        foundNotifications.length must equal(1)
        foundNotifications must contain(notification_3)
      }
    }

    "there are Notifications for all of provided actionIds" should {
      "return all the Notifications" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue
        repo.save(notification_3).futureValue

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
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue
        repo.save(notification_3).futureValue

        repo.findNotificationsByActionIds(Seq.empty).futureValue must equal(Seq.empty)
      }
    }
  }

}
