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
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatusGroup.EnhancedStatusGroup
import uk.gov.hmrc.exports.models.declaration.submissions.Submission

import java.time.ZonedDateTime

case class SubmissionPageData(
  limit: Int,

  /* When NOT None, select what EnhancedStatusGroup documents to fetch according to
     'datetimeForPreviousPage' or 'datetimeForNextPage' or 'page'.
     When None, fetch the first page of the first EnhancedStatusGroup which has documents.
     When None, 'datetimeForPreviousPage' and 'datetimeForNextPage' and 'page' are ignored. */
  statusGroup: Option[EnhancedStatusGroup] = None,

  // Only used when 'statusGroup' is NOT None and 'page' is NOT '0 or 1' (None or other values).
  // When NOT None, fetch the previous page. Alternative to 'datetimeForNextPage'.
  datetimeForPreviousPage: Option[ZonedDateTime] = None,

  // Only used when 'statusGroup' is NOT None and 'page' is NOT '0 or 1' (None or other values)
  // and 'datetimeForPreviousPage' is None. When NOT None, fetch the next page.
  datetimeForNextPage: Option[ZonedDateTime] = None,

  /* Only used when 'statusGroup' is NOT None.
     When '0 or 1', fetch the first page of EnhancedStatusGroup documents.
     When None and 'datetimeForPreviousPage' and 'datetimeForNextPage' are NONE, fetch the last page.
     When NOT '0 and 1' and 'datetimeForPreviousPage' and 'datetimeForNextPage' are NONE, fetch a specific page. */
  page: Option[Int] = None
)

object SubmissionPageData {
  implicit val format = Json.format[SubmissionPageData]

  val DEFAULT_LIMIT = 25
}

case class PageOfSubmissions(statusGroup: EnhancedStatusGroup, totalSubmissionsInGroup: Int, submissions: Seq[Submission])

object PageOfSubmissions {
  implicit val format = Json.format[PageOfSubmissions]
}
