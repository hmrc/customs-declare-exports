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
import play.api.Configuration
import uk.gov.hmrc.exports.base.UnitSpec

class ExportsMigrationConfigSpec extends UnitSpec {

  private val configWithExportsMigrationEnabled: Config = ConfigFactory.parseString("microservice.features.exportsMigration=enabled")
  private val configWithExportsMigrationDisabled: Config = ConfigFactory.parseString("microservice.features.exportsMigration=disabled")
  private val emptyConfig: Config = ConfigFactory.parseString("microservice.features.default=disabled")

  private def buildExportsMigrationConfig(config: Config): ExportsMigrationConfig =
    new ExportsMigrationConfig(new FeatureSwitchConfig(Configuration(config)))

  "ExportsMigrationConfig on isExportsMigrationEnabled" should {

    "return true" when {

      "exports migrations feature is enabled" in {

        val exportsMigrationConfig: ExportsMigrationConfig = buildExportsMigrationConfig(configWithExportsMigrationEnabled)

        exportsMigrationConfig.isExportsMigrationEnabled mustBe true
      }
    }

    "return false" when {

      "exports migrations feature is disabled" in {

        val exportsMigrationConfig: ExportsMigrationConfig = buildExportsMigrationConfig(configWithExportsMigrationDisabled)

        exportsMigrationConfig.isExportsMigrationEnabled mustBe false
      }

      "exports migrations feature is not defined" in {

        val exportsMigrationConfig: ExportsMigrationConfig = buildExportsMigrationConfig(emptyConfig)

        exportsMigrationConfig.isExportsMigrationEnabled mustBe false
      }
    }
  }

}
