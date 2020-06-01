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

import java.util

import com.github.cloudyrock.mongock.{ChangeLog, ChangeSet}
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.{BsonType, Document}
import org.mongodb.scala.model.Filters.{eq => feq, ne => fne, _}
import org.mongodb.scala.model.UpdateOneModel
import org.mongodb.scala.model.Updates.{rename, set}
import play.api.Logger
import uk.gov.hmrc.exports.models.generators.{IdGenerator, StringIdGenerator}
import uk.gov.hmrc.exports.services.CountriesService

import scala.collection.JavaConversions._

@ChangeLog
class CacheChangeLog {

  private val logger = Logger(this.getClass)

  private val INDEX_ID = "id"
  private val INDEX_EORI = "eori"

  private var idGenerator: IdGenerator[String] = new StringIdGenerator
  private[changesets] def setIdGenerator(newIdGenerator: IdGenerator[String]): Unit =
    this.idGenerator = newIdGenerator

  @ChangeSet(order = "001", id = "Exports DB Baseline", author = "Paulo Monteiro")
  def dbBaseline(db: MongoDatabase): Unit = {}

  @ChangeSet(order = "002", id = "CEDS-2231 Change country name to country code for location page", author = "Patryk Rudnicki")
  def updateAllCountriesNameToCodesForLocationPage(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2231 Change country name to country code for location page' db migration... ")

    val documents: Iterable[Document] = getDeclarationsCollection(db).find(
      and(
        exists("locations.goodsLocation.country"),
        `type`("locations.goodsLocation.country", BsonType.STRING),
        fne("locations.goodsLocation.country", ""),
        regex("locations.goodsLocation.country", "^.{3,}$")
      )
    )

    documents.foreach { document =>
      val documentId = document.get(INDEX_ID).asInstanceOf[String]
      val eori = document.get(INDEX_EORI).asInstanceOf[String]

      val countryName = document.get("locations", classOf[Document]).get("goodsLocation", classOf[Document]).get("country").asInstanceOf[String]

      logger.info("Updating [" + countryName + "] for document Id [" + documentId + "]")
      getDeclarationsCollection(db)
        .updateOne(and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori)), set("locations.goodsLocation.country", getCountryCode(countryName)))
    }

    logger.info("Applying 'CEDS-2231 Change country name to country code for location page' db migration... Done.")
  }

  @ChangeSet(order = "003", id = "CEDS-2247 Change origination country structure", author = "Patryk Rudnicki")
  def changeOriginationCountryStructure(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2247 Change origination country structure' db migration... ")

    getDeclarationsCollection(db).updateMany(
      and(exists("locations.originationCountry"), not(exists("locations.originationCountry.code"))),
      rename("locations.originationCountry", "temp")
    )
    getDeclarationsCollection(db).updateMany(exists("temp"), rename("temp", "locations.originationCountry.code"))

    logger.info("Applying 'CEDS-2247 Change origination country structure' db migration... Done.")
  }

  @ChangeSet(order = "004", id = "CEDS-2247 Change destination country structure", author = "Patryk Rudnicki")
  def changeDestinationCountryStructure(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2247 Change destination country structure' db migration... ")

    getDeclarationsCollection(db).updateMany(
      and(exists("locations.destinationCountry"), not(exists("locations.destinationCountry.code"))),
      rename("locations.destinationCountry", "temp")
    )
    getDeclarationsCollection(db).updateMany(exists("temp"), rename("temp", "locations.destinationCountry.code"))

    logger.info("Applying 'CEDS-2247 Change destination country structure' db migration... Done.")
  }

  @ChangeSet(order = "005", id = "CEDS-2247 Change routing countries structure", author = "Patryk Rudnicki")
  def changeRoutingCountriesStructure(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2247 Change routing countries structure' db migration... ")

    val documents: Iterable[Document] = getDeclarationsCollection(db).find(
      and(
        not(elemMatch("locations.routingCountries", exists("code", true))),
        exists("locations.routingCountries"),
        not(size("locations.routingCountries", 0))
      )
    )

    documents.foreach { document =>
      val documentId = document.get(INDEX_ID).asInstanceOf[String]
      val eori = document.get(INDEX_EORI).asInstanceOf[String]

      val routingCountries = document.get("locations", classOf[Document]).get("routingCountries", classOf[util.List[String]]).toSeq

      val codesList = seqAsJavaList(routingCountries.map(code => mapAsJavaMap(Map("code" -> code))))

      logger.info("Updating document Id [" + documentId + "]")
      getDeclarationsCollection(db).updateOne(and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori)), set("locations.routingCountries", codesList))
    }

    logger.info("Applying 'CEDS-2247 Change destination country structure' db migration... Done.")
  }

  @ChangeSet(order = "006", id = "CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode", author = "Maciej Rewera")
  def updateTransportBorderModeOfTransportCode(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode' db migration...")

    getDeclarationsCollection(db).updateMany(
      and(exists("transport.borderModeOfTransportCode"), not(exists("transport.borderModeOfTransportCode.code"))),
      rename("transport.borderModeOfTransportCode", "temp")
    )
    getDeclarationsCollection(db).updateMany(exists("temp"), rename("temp", "transport.borderModeOfTransportCode.code"))

    logger.info("Applying 'CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode' db migration... Done.")
  }

  @ChangeSet(order = "007", id = "CEDS-2387 Add ID field to /items/packageInformation", author = "Maciej Rewera", runAlways = true)
  def addIdFieldToPackageInformation(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2387 Add ID field to /items/packageInformation' db migration...")

    val queryBatchSize = 2
    val updateBatchSize = 10

    getDeclarationsCollection(db)
      .find(
        and(
          exists("items"),
          not(size("items", 0)),
          exists("items.packageInformation"),
          not(size("items.packageInformation", 0)),
          not(exists("items.packageInformation.id"))
        )
      )
      .batchSize(queryBatchSize)
      .toIterator
      .map { document =>
          val items = document.get("items", classOf[util.List[Document]])
          val itemsUpdated: util.List[Document] = items.map(updateItem)
          logger.debug(s"Items updated: $itemsUpdated")

          val documentId = document.get(INDEX_ID).asInstanceOf[String]
          val eori = document.get(INDEX_EORI).asInstanceOf[String]
          val filter = and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori))
          val update = set[util.List[Document]]("items", itemsUpdated)
          logger.debug(s"[filter: $filter] [update: $update]")

          new UpdateOneModel[Document](filter, update)
      }
      .grouped(updateBatchSize)
      .zipWithIndex
      .foreach {
        case (requests, idx) =>
          logger.info(s"ChangeSet 007. Updating batch no. $idx...")

          getDeclarationsCollection(db).bulkWrite(requests)
          logger.info(s"ChangeSet 007. Updated batch no. $idx")
      }

    logger.info("Applying 'CEDS-2387 Add ID field to /items/packageInformation' db migration... Done.")
  }

  private def updateItem(itemDocument: Document): Document = {
    val packageInformationElems = itemDocument.get("packageInformation", classOf[util.List[Document]])

    val packageInformationElemsUpdated = packageInformationElems.map(_.append("id", idGenerator.generateId()))

    itemDocument.updated("packageInformation", packageInformationElemsUpdated)
    new Document(itemDocument)
  }

  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] = {
    val collectionName = "declarations"
    db.getCollection(collectionName)
  }

  private def getCountryCode(countryName: String): String = {
    val service = new CountriesService
    service.allCountriesAsJava.toStream
      .find(country => country.countryName == countryName)
      .map(_.countryCode)
      .getOrElse(countryName)
  }
}
