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

import java.time.LocalDateTime

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.models.declaration.notifications.{ErrorPointer, Notification, NotificationError}
import uk.gov.hmrc.exports.repositories.NotificationsRepository
import util.testdata.TestDataHelper

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class NotificationsRepositorySpec
    extends WordSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with ScalaFutures with MustMatchers
    with OptionValues {

  import NotificationsRepositorySpec._

  override lazy val app: Application = GuiceApplicationBuilder().build()
  private val repo = app.injector.instanceOf[NotificationsRepository]

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

        val notificationInDB = repo.findNotificationByConversationId(conversationId).futureValue
        notificationInDB.length must equal(1)
        notificationInDB.head must equal(notification)
      }
    }
  }

  "Notification Repository on findNotificationByConversationId" when {

    "there is no Notification with given conversationId" should {
      "return empty list" in {
        repo.findNotificationByConversationId(conversationId).futureValue must equal(Seq.empty)
      }
    }

    "there is single Notification with given conversationId" should {
      "return this Notification only" in {
        repo.save(notification).futureValue

        val foundNotifications = repo.findNotificationByConversationId(conversationId).futureValue

        foundNotifications.length must equal(1)
        foundNotifications.head must equal(notification)
      }
    }

    "there are multiple Notifications with given conversationId" should {
      "return all the Notifications" in {
        repo.save(notification).futureValue
        repo.save(notification_2).futureValue

        val foundNotifications = repo.findNotificationByConversationId(conversationId).futureValue

        foundNotifications.length must equal(2)
        foundNotifications must contain(notification)
        foundNotifications must contain(notification_2)
      }
    }
  }

}

object NotificationsRepositorySpec {

  private lazy val functionCodes: Seq[String] =
    Seq("01", "02", "03", "05", "06", "07", "08", "09", "10", "11", "16", "17", "18")
  private lazy val functionCodesRandomised: Iterator[String] = Random.shuffle(functionCodes).toIterator
  private def randomResponseFunctionCode: String = functionCodesRandomised.next()

  val conversationId: String = "b1c09f1b-7c94-4e90-b754-7c5c71c44e11"
  val mrn: String = "MRN87878797"
  lazy val dateTimeIssued: LocalDateTime = LocalDateTime.now()
  lazy val dateTimeIssued_2: LocalDateTime = LocalDateTime.now()
  val functionCode: String = randomResponseFunctionCode
  val functionCode_2: String = randomResponseFunctionCode
  val nameCode: Option[String] = None
  val errors = Seq(
    NotificationError(
      validationCode = "CDS12056",
      pointers = Seq(ErrorPointer(documentSectionCode = "42A", tagId = None))
    )
  )

  private val payloadExemplaryLength = 300
  val payload = TestDataHelper.randomAlphanumericString(payloadExemplaryLength)
  val payload_2 = TestDataHelper.randomAlphanumericString(payloadExemplaryLength)

  val notification = Notification(
    conversationId = conversationId,
    mrn = mrn,
    dateTimeIssued = dateTimeIssued,
    functionCode = functionCode,
    nameCode = nameCode,
    errors = errors,
    payload = payload
  )
  val notification_2 = Notification(
    conversationId = conversationId,
    mrn = mrn,
    dateTimeIssued = dateTimeIssued_2,
    functionCode = functionCode_2,
    nameCode = nameCode,
    errors = errors,
    payload = payload_2
  )

}
