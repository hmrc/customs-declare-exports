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

package uk.gov.hmrc.exports.migrations.changelogs.emaildetails

import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.emaildetails.RenameSendEmailDetailsToItemISpec._

class RenameSendEmailDetailsToItemISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "sendEmailWorkItems"
  override val changeLog = new RenameSendEmailDetailsToItem()

  "RenameSendEmailDetailsToItem" should {

    "correctly migrate documents renaming 'sendEmailDetails' to 'item'" in {
      runTest(testDataBeforeRenaming, testDataAfterRenaming)
    }

    "not change the documents already migrated" in {
      runTest(testDataAfterRenaming, testDataAfterRenaming)
    }

    "not change the documents that do not include the 'sendEmailDetails' object" in {
      runTest(testDataOutOfScope, testDataOutOfScope)
    }
  }
}

object RenameSendEmailDetailsToItemISpec {

  val testDataBeforeRenaming: String =
    """{
      |    "_id" : "62963187b7000076f955c101",
      |    "availableAt" : "2022-05-31T15:17:27.708Z",
      |    "receivedAt" : "2022-05-31T15:17:27.708Z",
      |    "failureCount" : 0,
      |    "updatedAt" : "2022-05-31T15:20:02.465Z",
      |    "status" : "succeeded",
      |    "sendEmailDetails" : {
      |        "notificationId" : "62963187b7000076f955c100",
      |        "mrn" : "22GB8817MV57110081",
      |        "actionId" : "587d980c-91e0-4ef2-8590-1fa67d0c9631",
      |        "alertTriggered" : false
      |    }
      |}""".stripMargin

  val testDataAfterRenaming: String =
    """{
      |    "_id" : "62963187b7000076f955c101",
      |    "availableAt" : "2022-05-31T15:17:27.708Z",
      |    "receivedAt" : "2022-05-31T15:17:27.708Z",
      |    "failureCount" : 0,
      |    "updatedAt" : "2022-05-31T15:20:02.465Z",
      |    "status" : "succeeded",
      |    "item" : {
      |        "notificationId" : "62963187b7000076f955c100",
      |        "mrn" : "22GB8817MV57110081",
      |        "actionId" : "587d980c-91e0-4ef2-8590-1fa67d0c9631",
      |        "alertTriggered" : false
      |    }
      |}""".stripMargin

  val testDataOutOfScope: String =
    """{
      |    "_id" : "62963187b7000076f955c101",
      |    "availableAt" : "2022-05-31T15:17:27.708Z",
      |    "receivedAt" : "2022-05-31T15:17:27.708Z",
      |    "failureCount" : 0,
      |    "updatedAt" : "2022-05-31T15:20:02.465Z",
      |    "status" : "succeeded",
      |    "outOfScope" : {
      |        "notificationId" : "62963187b7000076f955c100",
      |        "mrn" : "22GB8817MV57110081",
      |        "actionId" : "587d980c-91e0-4ef2-8590-1fa67d0c9631",
      |        "alertTriggered" : false
      |    }
      |}""".stripMargin
}
