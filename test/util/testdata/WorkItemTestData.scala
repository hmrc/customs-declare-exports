/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.DateTime
import org.joda.time.DateTime.now
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.workitem.{ProcessingStatus, WorkItem}

object WorkItemTestData {

  def buildTestSendEmailDetails: SendEmailDetails = SendEmailDetails(notificationId = BSONObjectID.generate, mrn = ExportsTestData.mrn)

  def buildTestSendEmailWorkItem(status: ProcessingStatus, updatedAt: DateTime = now, availableAt: DateTime = now): WorkItem[SendEmailDetails] =
    buildTestWorkItem(status, updatedAt, availableAt, item = buildTestSendEmailDetails)

  def buildTestWorkItem[T](status: ProcessingStatus, updatedAt: DateTime = now, availableAt: DateTime = now, item: T): WorkItem[T] =
    WorkItem[T](
      id = BSONObjectID.generate,
      receivedAt = now,
      updatedAt = updatedAt,
      availableAt = availableAt,
      status = status,
      failureCount = 0,
      item = item
    )

}
