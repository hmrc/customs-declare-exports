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

package uk.gov.hmrc.exports.config

import uk.gov.hmrc.exports.base.CustomsExportsBaseSpec

class AppConfigSpec extends CustomsExportsBaseSpec {
  val config = app.injector.instanceOf[AppConfig]

  "The config" should {
    "have auth url" in {
      config.authUrl must be("http://localhost:8500")
    }

    "have login url" in {
      config.loginUrl must be("http://localhost:9949/auth-login-stub/gg-sign-in")
    }
  }
}
