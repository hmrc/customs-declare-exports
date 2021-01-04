/*
 * Copyright 2021 HM Revenue & Customs
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

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{MustMatchers, WordSpec}
import play.api.Configuration
import uk.gov.hmrc.exports.features.{Feature, FeatureStatus}

class FeatureSwitchConfigSpec extends WordSpec with MustMatchers {

  private val validConfig: Config =
    ConfigFactory.parseString("""
        |microservice.features.default=disabled
      """.stripMargin)
  private val emptyConfig: Config = ConfigFactory.empty()

  val validFeatureSwitchConfig = new FeatureSwitchConfig(Configuration(validConfig))
  val emptyFeatureSwitchConfig = new FeatureSwitchConfig(Configuration(emptyConfig))

  "Feature Switch config" should {

    "have default feature status" in {
      validFeatureSwitchConfig.defaultFeatureStatus mustBe FeatureStatus.disabled
    }

    "return correct value for feature" in {
      validFeatureSwitchConfig.featureStatus(Feature.default) mustBe FeatureStatus.disabled
    }

    "return correct value for isFeatureOn method" in {
      validFeatureSwitchConfig.isFeatureOn(Feature.default) mustBe false
    }

    "throw an exception when microservice.services.features.default is missing" in {
      intercept[Exception](emptyFeatureSwitchConfig.defaultFeatureStatus).getMessage mustBe
        "Missing configuration key: microservice.features.default"
    }
  }
}
