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

package uk.gov.hmrc.exports.controllers

import play.api.http.Status.{OK, SERVICE_UNAVAILABLE}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status, stubControllerComponents, GET}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.migrations.MigrationStatus

class CustomHealthControllerSpec extends UnitSpec {

  private val cc = stubControllerComponents()
  private val config = mock[Configuration]
  private val env = mock[Environment]
  private val migrationStatus = mock[MigrationStatus]

  private val controller = new CustomHealthController(migrationStatus, config, env, cc)

  "GET /ping/ping/ endpoint" should {
    "return OK" when {
      "Migration status is success" in {
        when(migrationStatus.isSuccess).thenReturn(true)

        val result = controller.ping(FakeRequest(GET, "/ping"))

        status(result) mustBe OK
      }
    }

    "return 503 - Service Unavailable" when {
      "Migration status is failure" in {
        when(migrationStatus.isSuccess).thenReturn(false)

        val result = controller.ping(FakeRequest(GET, "/ping"))

        status(result) mustBe SERVICE_UNAVAILABLE
      }
    }
  }
}
