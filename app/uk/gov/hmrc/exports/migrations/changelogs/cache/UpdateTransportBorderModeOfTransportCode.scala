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

package uk.gov.hmrc.exports.migrations.changelogs.cache
import com.mongodb.client.MongoDatabase
import org.mongodb.scala.model.Filters.{and, exists, not}
import org.mongodb.scala.model.Updates.rename
import play.api.Logger
import uk.gov.hmrc.exports.migrations.changelogs.MigrationInformation

class UpdateTransportBorderModeOfTransportCode extends CacheMigrationDefinition {

  private val logger = Logger(this.getClass)

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = "CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode", order = 6, author = "Maciej Rewera")

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info("Applying 'CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode' db migration...")

    getDeclarationsCollection(db).updateMany(
      and(exists("transport.borderModeOfTransportCode"), not(exists("transport.borderModeOfTransportCode.code"))),
      rename("transport.borderModeOfTransportCode", "temp")
    )
    getDeclarationsCollection(db).updateMany(exists("temp"), rename("temp", "transport.borderModeOfTransportCode.code"))

    logger.info("Applying 'CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode' db migration... Done.")
  }
}
