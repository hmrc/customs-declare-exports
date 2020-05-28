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

package uk.gov.hmrc.exports.mongock.changesets

import com.github.cloudyrock.mongock.{ChangeLog, ChangeSet}
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoDatabase
import play.api.Logger

import scala.io.Source

@ChangeLog
class TestChangeLog {

  private val logger = Logger(this.getClass)

  @ChangeSet(order = "001", id = "CEDS-2473 test migration", author = "Maciej Rewera")
  def javascriptMigrationTest(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2473 test migration' db migration...")

    val fileName = "migrationscripts/TestMigration_CEDS-2473.js"
    migrateWithScript(fileName, db)

    logger.info("Applying 'CEDS-2473 test migration' db migration... Done.")
  }

  private def migrateWithScript(fileName: String, db: MongoDatabase): Unit = {
    val scriptFile = Source.fromURL(getClass.getClassLoader.getResource(fileName), "UTF-8")
    val migrationScript = scriptFile.mkString
    scriptFile.close()

    logger.info(s"Executing data migration according to script ${fileName}:\n${migrationScript}")

    db.runCommand(new BasicDBObject("eval", migrationScript))
  }
}
