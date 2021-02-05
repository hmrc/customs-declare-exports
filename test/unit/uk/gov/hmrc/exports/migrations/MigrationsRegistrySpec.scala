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

package uk.gov.hmrc.exports.migrations

import com.mongodb.client.MongoDatabase
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

class MigrationsRegistrySpec extends UnitSpec {

  private def buildMigrationDefinition(migrationInfo: MigrationInformation): MigrationDefinition = new MigrationDefinition {
    override val migrationInformation: MigrationInformation = migrationInfo
    override def migrationFunction(db: MongoDatabase): Unit = {}
  }

  "MigrationsRegistry on register" should {

    "return new MigrationsRegistry object with MigrationDefinition added" in {

      val migrationDefinition = buildMigrationDefinition(MigrationInformation("id", 0, "author"))
      val registry = MigrationsRegistry()

      val newRegistry = registry.register(migrationDefinition)

      newRegistry mustNot be(registry)
      newRegistry.migrations mustNot be('empty)
      newRegistry.migrations must contain(migrationDefinition)
    }

    "allow to chain method calls" in {

      val migrationDefinition_1 = buildMigrationDefinition(MigrationInformation("id_1", 1, "author_1"))
      val migrationDefinition_2 = buildMigrationDefinition(MigrationInformation("id_2", 2, "author_2"))
      val migrationDefinition_3 = buildMigrationDefinition(MigrationInformation("id_3", 3, "author_3"))

      val registry = MigrationsRegistry().register(migrationDefinition_1).register(migrationDefinition_2).register(migrationDefinition_3)

      registry.migrations.size mustBe 3
      registry.migrations must contain(migrationDefinition_1)
      registry.migrations must contain(migrationDefinition_2)
      registry.migrations must contain(migrationDefinition_3)
    }

    "allow adding the same MigrationDefinition twice" in {

      val migrationDefinition = buildMigrationDefinition(MigrationInformation("id", 0, "author"))

      val registry = MigrationsRegistry().register(migrationDefinition).register(migrationDefinition)

      registry.migrations.size mustBe 2
      registry.migrations mustBe Seq(migrationDefinition, migrationDefinition)
    }
  }

}
