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

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.exports.migrations.MigrationStatus
import uk.gov.hmrc.play.health.HealthController

import javax.inject.Inject

class CustomHealthController @Inject() (
  migrationStatus: MigrationStatus,
  configuration: Configuration,
  environment: Environment,
  controllerComponents: ControllerComponents
) extends HealthController(configuration, environment, controllerComponents) {

  override def ping: Action[AnyContent] =
    if (migrationStatus.isSuccess) {
      super.ping
    } else {
      Action(ServiceUnavailable("Migrations failed."))
    }
}
