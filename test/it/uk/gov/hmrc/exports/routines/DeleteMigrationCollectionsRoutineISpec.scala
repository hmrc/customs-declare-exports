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
package uk.gov.hmrc.exports.routines

import com.codahale.metrics.MetricRegistry
import com.mongodb.client.MongoClients
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.jdk.CollectionConverters._

class DeleteMigrationCollectionsRoutineISpec extends AnyWordSpec with BeforeAndAfterEach with GuiceOneAppPerSuite {

  private val databaseName = "test-customs-declare-exports"

  private val mongoConfiguration = Configuration.from(Map("mongodb.uri" -> s"mongodb://localhost:27017/$databaseName"))

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .disable[MetricRegistry]
      .configure(mongoConfiguration)
      .build()

  private val mongoClient = MongoClients.create()
  private val mongoDatabase = mongoClient.getDatabase(databaseName)

  private val collections = List("exportsMigrationChangeLog", "exportsMigrationLock")

  override def afterEach(): Unit = {
    mongoClient.close()
    super.afterEach()
  }

  "DeleteMigrationCollectionsRoutine" should {
    "drop all migration-related collections" in {
      val existingCollections = mongoDatabase.listCollectionNames().asScala.toList
      collections.foreach { collection =>
        if (!existingCollections.contains(collection)) mongoDatabase.createCollection(collection)
      }

      expectedNumberOfCollections(2)

      val routine = app.injector.instanceOf[DeleteMigrationCollectionsRoutine]
      routine.execute().futureValue

      expectedNumberOfCollections(0)
    }
  }

  private def expectedNumberOfCollections(expectedSize: Int): Assertion =
    mongoDatabase.listCollectionNames().asScala.filter(collections.contains).toList.size mustBe expectedSize
}
