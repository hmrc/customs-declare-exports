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
import com.google.common.collect.ImmutableMap
import com.mongodb.BasicDBObject
import com.mongodb.client.model.Filters._
import com.mongodb.client.model.Updates.rename
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.model.Filters
import play.api.Logger
import uk.gov.hmrc.exports.services.CountriesService

import scala.collection.JavaConversions._

@ChangeLog(order = "001")
class CacheChangeLog {

  private val logger = Logger(this.getClass)

  @ChangeSet(order = "001", id = "Exports DB Baseline", author = "Paulo Monteiro")
  def dbBaseline(db: MongoDatabase): Unit = {}

  @ChangeSet(order = "002", id = "CEDS-2231 Change country name to country code for location page", author = "Patryk Rudnicki")
  def updateAllCountriesNameToCodesForLocationPage(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2231 Change country name to country code for location page'... ")
    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.goodsLocation.country", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    val documents = getDeclarationsCollection(db).find(new BasicDBObject(query))

    documents.foreach { document =>
      val goodsLocation = document
        .get("locations", classOf[util.Map[String, util.Map[String, String]]])
        .get("goodsLocation")
        .asInstanceOf[util.Map[String, String]]

      val countryName: String = goodsLocation.get("country")

      goodsLocation.put("country", findCountryCode(countryName))

      // update document on MongoDB
      val queryIndexes: util.Map[String, String] =
        ImmutableMap.of("id", document.get("id").asInstanceOf[String], "eori", document.get("eori").asInstanceOf[String])

      val objectToBeUpdated: BasicDBObject = new BasicDBObject(queryIndexes)

      logger.info(s"Applying [2] changes to ${document.get("id")}")
      getDeclarationsCollection(db).replaceOne(objectToBeUpdated, document)
    }

    logger.info("Applying 'CEDS-2231 Change country name to country code for location page'... Done.")
  }

  private def findCountryCode(countryName: String): String = {
    val service: CountriesService = new CountriesService

    service.allCountries.find(_.countryName == countryName).map(_.countryCode).getOrElse(countryName)
  }

  @ChangeSet(order = "003", id = "CEDS-2247 Change origination country structure", author = "Patryk Rudnicki")
  def changeOriginationCountryStructure(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2247 Change origination country structure'... ")
    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.originationCountry", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    val documents = getDeclarationsCollection(db).find(new BasicDBObject(query))

    documents.foreach { document =>
      val locations = document.get("locations", classOf[util.Map[String, String]])
      val originationCountry: String = locations.get("originationCountry")

      val queryIndexes: util.Map[String, String] =
        ImmutableMap.of("id", document.get("id").asInstanceOf[String], "eori", document.get("eori").asInstanceOf[String])

      val objectToBeUpdated: BasicDBObject = new BasicDBObject(queryIndexes)

      val codeElement = ImmutableMap.of("code", originationCountry)
      val setUpdate = ImmutableMap.of("$set", ImmutableMap.of("locations.originationCountry", codeElement))
      val setUpdateObj = new BasicDBObject(setUpdate)

      logger.info(s"Applying [3] changes to ${document.get("id")}")
      getDeclarationsCollection(db).findOneAndUpdate(objectToBeUpdated, setUpdateObj)
    }
    logger.info("Applying 'CEDS-2247 Change origination country structure'... Done.")
  }

  @ChangeSet(order = "004", id = "CEDS-2247 Change destination country structure", author = "Patryk Rudnicki")
  def changeDestinationCountryStructure(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2247 Change destination country structure' db migrations... ")
    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.destinationCountry", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    val documents = getDeclarationsCollection(db).find(new BasicDBObject(query))
    logger.info(s"[${documents.size}] documents found")

    documents.foreach { document =>
      val locations = document.get("locations", classOf[util.Map[String, String]])
      val destinationCountry: String = locations.get("destinationCountry")

      val queryIndexes: util.Map[String, String] =
        ImmutableMap.of("id", document.get("id").asInstanceOf[String], "eori", document.get("eori").asInstanceOf[String])

      val objectToBeUpdated: BasicDBObject = new BasicDBObject(queryIndexes)

      val codeElement = ImmutableMap.of("code", destinationCountry)
      val setUpdate = ImmutableMap.of("$set", ImmutableMap.of("locations.destinationCountry", codeElement))
      val setUpdateObj = new BasicDBObject(setUpdate)
      logger.info(s"Applying [4] changes to ${document.get("id")}")
      getDeclarationsCollection(db).findOneAndUpdate(objectToBeUpdated, setUpdateObj)
    }
    logger.info("Applying 'CEDS-2247 Change destination country structure' db migrations... Done.")
  }

  @ChangeSet(order = "005", id = "CEDS-2247 Change routing countries structure", author = "Patryk Rudnicki")
  def changeRoutingCountriesStructure(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2247 Change routing countries structure... ")
    val queryParams: ImmutableMap[String, AnyRef] =
      ImmutableMap.of("locations.routingCountries", ImmutableMap.of("$exists", true, "$ne", ""))
    val query: Document = new Document(queryParams)

    val documents = getDeclarationsCollection(db).find(new BasicDBObject(query))
    logger.info(s"[${documents.size}] documents found")

    documents.foreach { document =>
      val locations = document.get("locations", classOf[util.Map[String, java.util.List[String]]])
      val routingCountries: Seq[String] = locations.get("routingCountries")

      val queryIndexes: util.Map[String, String] =
        ImmutableMap.of("id", document.get("id").asInstanceOf[String], "eori", document.get("eori").asInstanceOf[String])

      val objectToBeUpdated: BasicDBObject = new BasicDBObject(queryIndexes)

      val updatedRoutingCountries = routingCountries.map(ImmutableMap.of("code", _)).toArray

      val setUpdate = ImmutableMap.of("$set", ImmutableMap.of("locations.routingCountries", updatedRoutingCountries))
      val setUpdateObj = new BasicDBObject(setUpdate)

      logger.info(s"Applying [5] changes to ${document.get("id")}")
      getDeclarationsCollection(db).findOneAndUpdate(objectToBeUpdated, setUpdateObj)
    }
    logger.info("Applying 'CEDS-2247 Change routing countries structure... Done.")
  }

  @ChangeSet(order = "006", id = "CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode", author = "Maciej Rewera")
  def updateTransportBorderModeOfTransportCode(db: MongoDatabase): Unit = {

    logger.info("Applying 'CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode'...")

    getDeclarationsCollection(db).updateMany(
      and(exists("transport.borderModeOfTransportCode"), not(Filters.eq("transport.borderModeOfTransportCode", ""))),
      rename("transport.borderModeOfTransportCode", "temp")
    )
    getDeclarationsCollection(db).updateMany(and(exists("temp")), rename("temp", "transport.borderModeOfTransportCode.code"))

    logger.info("Applying 'CEDS-2250 Add one structure level to /transport/borderModeOfTransportCode'... Done.")
  }

  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] = {
    val collectionName = "declarations"
    db.getCollection(collectionName)
  }
}
