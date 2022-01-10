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

package uk.gov.hmrc.exports.models.declaration.submissions

import org.scalatest.EitherValues
import uk.gov.hmrc.exports.base.UnitSpec

class SubmissionQueryParametersSpec extends UnitSpec with EitherValues {

  private val queryStringBindable = SubmissionQueryParameters.queryStringBindable

  "SubmissionQueryParameters.QueryStringBindable" when {

    "bind method is called" should {

      "return empty SubmissionQueryParameters" when {

        "provided with empty params" in {

          val params = Map.empty[String, Seq[String]]
          val expectedSubmissionQueryParameters = SubmissionQueryParameters()

          val result = queryStringBindable.bind("", params)

          result mustBe defined
          result.get.isRight mustBe true
          result.get.value mustBe expectedSubmissionQueryParameters
        }

        "provided with params containing NO known keys" in {

          val params = Map("param1" -> Seq("value1", "value2"), "param2" -> Seq("value3", "value4"))
          val expectedSubmissionQueryParameters = SubmissionQueryParameters()

          val result = queryStringBindable.bind("", params)

          result mustBe defined
          result.get.isRight mustBe true
          result.get.value mustBe expectedSubmissionQueryParameters
        }
      }

      "return non-empty SubmissionQueryParameters" when {

        "provided with params containing known keys" in {

          val params = Map("id" -> Seq("testUuid"), "ducr" -> Seq("testDucr"), "lrn" -> Seq("testLrn"))
          val expectedSubmissionQueryParameters = SubmissionQueryParameters(uuid = Some("testUuid"), ducr = Some("testDucr"), lrn = Some("testLrn"))

          val result = queryStringBindable.bind("", params)

          result mustBe defined
          result.get.isRight mustBe true
          result.get.value mustBe expectedSubmissionQueryParameters
        }
      }
    }

    "unbind method is called" should {

      "return empty String" when {
        "SubmissionQueryParameters is empty" in {

          val submissionQueryParameters = SubmissionQueryParameters()
          val expectedResult = ""

          val result = queryStringBindable.unbind("", submissionQueryParameters)

          result mustBe expectedResult
        }
      }

      "return single query parameter" when {

        "SubmissionQueryParameters has uuid field only" in {

          val submissionQueryParameters = SubmissionQueryParameters(uuid = Some("testUuid"))
          val expectedResult = "id=testUuid"

          val result = queryStringBindable.unbind("", submissionQueryParameters)

          result mustBe expectedResult
        }

        "SubmissionQueryParameters has ducr field only" in {

          val submissionQueryParameters = SubmissionQueryParameters(ducr = Some("testDucr"))
          val expectedResult = "ducr=testDucr"

          val result = queryStringBindable.unbind("", submissionQueryParameters)

          result mustBe expectedResult
        }

        "SubmissionQueryParameters has lrn field only" in {

          val submissionQueryParameters = SubmissionQueryParameters(lrn = Some("testLrn"))
          val expectedResult = "lrn=testLrn"

          val result = queryStringBindable.unbind("", submissionQueryParameters)

          result mustBe expectedResult
        }
      }

      "return all defined parameters separated by '&'" in {

        val submissionQueryParameters = SubmissionQueryParameters(uuid = Some("testUuid"), ducr = Some("testDucr"), lrn = Some("testLrn"))
        val expectedResult = "id=testUuid&ducr=testDucr&lrn=testLrn"

        val result = queryStringBindable.unbind("", submissionQueryParameters)

        result mustBe expectedResult
      }
    }
  }

}
