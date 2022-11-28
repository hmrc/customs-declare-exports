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

package uk.gov.hmrc.exports.models

import play.api.libs.json.Json
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup.StatusGroup

import java.time.ZonedDateTime

case class FetchSubmissionPageData(
  // Specify what group of Submissions to fetch.
  // The Submissions are grouped by categories of EnhancedStatus(es) (the 'latestEnhancedStatus' field).
  statusGroups: Seq[StatusGroup],

  // 'enhancedStatusLastUpdated' of the first Submission in the last page/batch of Submissions returned.
  // Used to fetch the previous page.
  datetimeForPreviousPage: Option[ZonedDateTime] = None,

  // 'enhancedStatusLastUpdated' of the last Submission in the last page/batch of Submissions returned.
  // Used to fetch the next page.
  datetimeForNextPage: Option[ZonedDateTime] = None,

  // Specify what page of Submissions to fetch according to the 'dashboardIdx' index.
  page: Option[Int] = None,

  // Specify the max number of Submissions to include in the page/batch.
  limit: Int
)

object FetchSubmissionPageData {
  implicit val format = Json.format[FetchSubmissionPageData]

  val DEFAULT_LIMIT = 25
}
