package uk.gov.hmrc.exports.base

import com.mongodb.client.{MongoClient, MongoClients}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.mongo.ExportsClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

object TestExportsClient extends ExportsClient {

  val mongoClient = MongoClients.create()

  private val config = Configuration()

  override val appConfig: AppConfig = new AppConfig(config, Environment.simple(), new ServicesConfig(config))

  override protected def createMongoClient: (MongoClient, String) = (mongoClient, "test-customs-declare-exports")

}
