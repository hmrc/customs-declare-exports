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

package uk.gov.hmrc.exports.services.notifications

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import testdata.ExportsTestData.mrn
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.NotificationDetails
import uk.gov.hmrc.exports.models.{Pointer, PointerSection, PointerSectionType}
import uk.gov.hmrc.exports.services.mapping.WcoToExportsMappingHelper

import java.time.ZonedDateTime

class NotificationParserSpec extends UnitSpec with BeforeAndAfterEach with MockitoSugar {

  private val wcoToExportsPointerMappings = mock[WcoToExportsMappingHelper]
  private val notificationParser = new NotificationParser(wcoToExportsPointerMappings)

  private def compareNotificationSequences(actual: Seq[NotificationDetails], expected: Seq[NotificationDetails]) = {
    actual.size mustBe expected.size

    val actualVsExpected = actual.zip(expected)
    actualVsExpected.foreach { case (actual, expected) =>
      actual.mrn mustBe expected.mrn
      actual.status mustBe expected.status
      withClue(s"Check if actual '${actual.dateTimeIssued.toInstant()}' is equal to expected '${expected.dateTimeIssued.toInstant()}'?") {
        actual.dateTimeIssued.toInstant().equals(expected.dateTimeIssued.toInstant()) mustBe true
      }
      actual.errors mustBe expected.errors
    }
  }

  override def afterEach(): Unit = {
    reset(wcoToExportsPointerMappings)
    super.afterEach()
  }

  val defaultReturn = Pointer(
    List(
      PointerSection("declaration", PointerSectionType.FIELD),
      PointerSection("items", PointerSectionType.FIELD),
      PointerSection("1", PointerSectionType.SEQUENCE),
      PointerSection("additionalDocument", PointerSectionType.FIELD),
      PointerSection("2", PointerSectionType.SEQUENCE),
      PointerSection("documentStatus", PointerSectionType.FIELD)
    )
  )

  "NotificationParser on parse" when {

    "provided with notification containing no Response element" should {

      "return empty sequence" in {
        val xml = exampleEmptyNotification(mrn).asXml

        val result = notificationParser.parse(xml)

        result mustBe Seq.empty[NotificationDetails]
      }
    }

    "provided with notification containing single Response element" which {

      "contains no error elements" should {

        "return single NotificationDetails with empty errors" in {
          val testNotification = exampleReceivedNotification(mrn)

          val result = notificationParser.parse(testNotification.asXml)

          compareNotificationSequences(result, testNotification.asDomainModel)
        }
      }

      "contains error elements" should {

        "return single NotificationDetails with errors" in {
          when(wcoToExportsPointerMappings.mapWcoPointerToExportsPointer(any())).thenReturn(Some(defaultReturn))

          val testNotification = exampleRejectNotification(mrn)

          val result = notificationParser.parse(testNotification.asXml)

          compareNotificationSequences(result, testNotification.asDomainModel)
        }
      }

      "contains error element 67A with a sequence number" should {
        "ignore the sequence number for that section" in {
          when(wcoToExportsPointerMappings.mapWcoPointerToExportsPointer(any())).thenReturn(Some(defaultReturn))

          val testNotification = exampleRejectNotification(mrn, with67ASequenceNo = true)

          val result = notificationParser.parse(testNotification.asXml)

          compareNotificationSequences(result, testNotification.asDomainModel)
        }
      }

      "contains child description element in parent error element" should {
        "return single NotificationDetails with errors populated with a smartErrorDescription" in {
          when(wcoToExportsPointerMappings.mapWcoPointerToExportsPointer(any())).thenReturn(Some(defaultReturn))
          val testNotification = exampleRejectNotification(mrn, withErrorDescription = true)

          val result = notificationParser.parse(testNotification.asXml)

          compareNotificationSequences(result, testNotification.asDomainModel)
        }
      }

      "contains error element with a suppressed pointer" should {
        "ignore the pointer" in {
          val pointerReturn = Pointer(List(PointerSection("suppressed", PointerSectionType.FIELD)))
          when(wcoToExportsPointerMappings.mapWcoPointerToExportsPointer(any())).thenReturn(Some(pointerReturn))
          val testNotification = exampleRejectNotificationWithSuppressedPointers(mrn)

          val result = notificationParser.parse(testNotification.asXml)

          compareNotificationSequences(result, testNotification.asDomainModel)
        }
      }
    }

    "provided with notification containing multiple Response elements" should {

      "return multiple NotificationDetails" in {
        val testNotification = exampleNotificationWithMultipleResponses(mrn)

        val result = notificationParser.parse(testNotification.asXml)

        compareNotificationSequences(result, testNotification.asDomainModel)
      }
    }

    "provided with notification containing invalid element" should {

      "throw an Exception" in {
        val testNotification = exampleUnparsableNotification(mrn)

        an[Exception] mustBe thrownBy(notificationParser.parse(testNotification.asXml))
      }
    }

    "provided with notification with a IssueDateTime value " should {
      "correctly parse the dateTime to a UTC value" when {
        "it contains an offset value of +00 (UTC)" in {
          val testNotification = exampleReceivedNotification(mrn, ZonedDateTime.from(formatter304.parse("20221231235959+00")))

          val result = notificationParser.parse(testNotification.asXml)

          compareNotificationSequences(result, testNotification.asDomainModel)
        }

        "it contains an offset value of +01 (BST)" in {
          val testNotification = exampleReceivedNotification(mrn, ZonedDateTime.from(formatter304.parse("20221231235959+01")))

          val result = notificationParser.parse(testNotification.asXml)

          compareNotificationSequences(result, testNotification.asDomainModel)
        }

        "it contains an offset value of -01 " in {
          val testNotification = exampleReceivedNotification(mrn, ZonedDateTime.from(formatter304.parse("20221231235959-01")))

          val result = notificationParser.parse(testNotification.asXml)

          compareNotificationSequences(result, testNotification.asDomainModel)
        }
      }
    }

    "provided with notification without a VersionId value " should {
      "correctly parse the version as None" in {
        val testNotification = exampleReceivedNotificationNoVersionId(mrn)

        val result = notificationParser.parse(testNotification.asXml)

        compareNotificationSequences(result, testNotification.asDomainModel)
      }
    }
  }
}
