/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.uk.gov.hmrc.exports.models.declaration.submissions

import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.retrieve

class SubmissionStatusSpec extends WordSpec with MustMatchers {

  "Reads for status" should {
    "correctly retrieve a value for every scenario" in {

      retrieve("Pending") must be(SubmissionStatus.PENDING)
      retrieve("Cancellation Requested") must be(SubmissionStatus.REQUESTED_CANCELLATION)
      retrieve("01") must be(SubmissionStatus.ACCEPTED)
      retrieve("02") must be(SubmissionStatus.RECEIVED)
      retrieve("03") must be(SubmissionStatus.REJECTED)
      retrieve("05") must be(SubmissionStatus.UNDERGOING_PHYSICAL_CHECK)
      retrieve("06") must be(SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED)
      retrieve("07") must be(SubmissionStatus.AMENDED)
      retrieve("08") must be(SubmissionStatus.RELEASED)
      retrieve("09") must be(SubmissionStatus.CLEARED)
      retrieve("10") must be(SubmissionStatus.CANCELLED)
      retrieve("1139") must be(SubmissionStatus.CUSTOMS_POSITION_GRANTED)
      retrieve("1141") must be(SubmissionStatus.CUSTOMS_POSITION_DENIED)
      retrieve("16") must be(SubmissionStatus.GOODS_HAVE_EXITED_THE_COMMUNITY)
      retrieve("17") must be(SubmissionStatus.DECLARATION_HANDLED_EXTERNALLY)
      retrieve("18") must be(SubmissionStatus.AWAITING_EXIT_RESULTS)
      retrieve("UnknownStatus") must be(SubmissionStatus.UNKNOWN)
      retrieve("WrongStatus") must be(SubmissionStatus.UNKNOWN)
      retrieve("11", Some("39")) must be(SubmissionStatus.CUSTOMS_POSITION_GRANTED)
      retrieve("11", Some("41")) must be(SubmissionStatus.CUSTOMS_POSITION_DENIED)

    }

    "ignore name codes when function code not 11" in {

      retrieve("06", Some("xx")) must be(SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED)
      retrieve("16", Some("yy")) must be(SubmissionStatus.GOODS_HAVE_EXITED_THE_COMMUNITY)

    }
  }
}
