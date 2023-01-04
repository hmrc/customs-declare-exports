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

package testdata

import org.bson.types.ObjectId
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant

object WorkItemTestData {

  def buildTestSendEmailDetails: SendEmailDetails =
    SendEmailDetails(notificationId = ObjectId.get, mrn = ExportsTestData.mrn, actionId = "actionId")

  def buildTestSendEmailWorkItem(
    status: ProcessingStatus,
    updatedAt: Instant = Instant.now,
    availableAt: Instant = Instant.now
  ): WorkItem[SendEmailDetails] =
    buildTestWorkItem(status, updatedAt, availableAt, item = buildTestSendEmailDetails)

  def buildTestWorkItem[T](status: ProcessingStatus, updatedAt: Instant = Instant.now, availableAt: Instant = Instant.now, item: T): WorkItem[T] =
    WorkItem[T](
      id = ObjectId.get,
      receivedAt = Instant.now,
      updatedAt = updatedAt,
      availableAt = availableAt,
      status = status,
      failureCount = 0,
      item = item
    )
}
