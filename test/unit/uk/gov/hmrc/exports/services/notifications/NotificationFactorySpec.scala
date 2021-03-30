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

package uk.gov.hmrc.exports.services.notifications

import org.mockito.ArgumentMatchers.{any, eq => meq}
import testdata.ExportsTestData.{actionId, mrn}
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.UnitSpec

import scala.xml.NodeSeq

class NotificationFactorySpec extends UnitSpec {

  private val notificationParser = mock[NotificationParser]

  private val notificationFactory = new NotificationFactory(notificationParser)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(notificationParser)
    when(notificationParser.parse(any[NodeSeq])).thenReturn(Seq.empty)
  }

  override def afterEach(): Unit = {
    reset(notificationParser)
    super.afterEach()
  }

  "NotificationFactory on buildNotifications" should {

    "call NotificationParser passing xml payload" in {
      val xml = exampleReceivedNotification(mrn).asXml
      val xmlInput = xml.toString

      notificationFactory.buildNotifications(actionId, xmlInput)

      verify(notificationParser).parse(meq(xml))
    }
  }

  "NotificationFactory on buildNotifications" when {

    "NotificationParser returns single NotificationDetails" should {

      val xmlInput = exampleReceivedNotification(mrn).asXml.toString

      "return single Notification with actionId" in {
        val details = notification.details
        when(notificationParser.parse(any[NodeSeq])).thenReturn(Seq(details))

        val result = notificationFactory.buildNotifications(actionId, xmlInput)

        result.size mustBe 1
        result.head.actionId mustBe actionId
      }

      "return single Notification with payload" in {
        val details = notification.details
        when(notificationParser.parse(any[NodeSeq])).thenReturn(Seq(details))

        val result = notificationFactory.buildNotifications(actionId, xmlInput)

        result.size mustBe 1
        result.head.payload mustBe xmlInput
      }

      "return single Notification with given details" in {
        val details = notification.details
        when(notificationParser.parse(any[NodeSeq])).thenReturn(Seq(details))

        val result = notificationFactory.buildNotifications(actionId, xmlInput)

        result.size mustBe 1
        result.head.details mustBe details
      }
    }

    "NotificationParser returns multiple NotificationDetails" should {

      val xmlInput = exampleNotificationWithMultipleResponses(mrn).asXml.toString
      val details = notification.details
      val details_2 = notification_2.details
      val details_3 = notification_3.details

      "return the same amount of Notifications with the same actionId" in {
        when(notificationParser.parse(any[NodeSeq])).thenReturn(Seq(details, details_2, details_3))

        val result = notificationFactory.buildNotifications(actionId, xmlInput)

        result.size mustBe 3
        result.head.actionId mustBe actionId
        result(1).actionId mustBe actionId
        result(2).actionId mustBe actionId
      }

      "return the same amount of Notifications with the same payload" in {
        when(notificationParser.parse(any[NodeSeq])).thenReturn(Seq(details, details_2, details_3))

        val result = notificationFactory.buildNotifications(actionId, xmlInput)

        val expectedPayload = xmlInput
        result.size mustBe 3
        result.head.payload mustBe expectedPayload
        result(1).payload mustBe expectedPayload
        result(2).payload mustBe expectedPayload
      }

      "return the same amount of Notifications with given details" in {
        when(notificationParser.parse(any[NodeSeq])).thenReturn(Seq(details, details_2, details_3))

        val result = notificationFactory.buildNotifications(actionId, xmlInput)

        result.size mustBe 3
        val allDetails = result.map(_.details)
        Seq(details, details_2, details_3).foreach { det =>
          allDetails must contain(det)
        }
      }
    }

    "NotificationParser returns no NotificationDetails" should {

      val xmlInput = exampleUnparsableNotification(mrn).asXml.toString

      "return empty sequence" in {

        val result = notificationFactory.buildNotifications(actionId, xmlInput)

        result mustBe empty
      }
    }

    "NotificationParser throws an Exception" should {

      val xmlInput = exampleUnparsableNotification(mrn).asXml.toString
      val exception = new RuntimeException("Test Exception")

      "not throw an exception" in {
        when(notificationParser.parse(any[NodeSeq])).thenThrow(exception)

        noException should be thrownBy notificationFactory.buildNotifications(actionId, xmlInput)
      }

      "return empty sequence" in {
        when(notificationParser.parse(any[NodeSeq])).thenThrow(exception)

        val result = notificationFactory.buildNotifications(actionId, xmlInput)

        result mustBe empty
      }
    }
  }

}
