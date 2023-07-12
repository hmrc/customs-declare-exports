import sbt._

object AppDependencies {

  val bootstrapPlayVersion = "7.13.0"
  val hmrcMongoVersion = "0.74.0"
  val testScope = "test,it"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-28" % hmrcMongoVersion,
    "uk.gov.hmrc" %% "wco-dec" % "0.39.0",
    "com.github.tototoshi" %% "scala-csv" % "1.3.10",
    // Used by the Migration tool. Keep this library's version to the same major.minor version as the mongo-scala-driver.
    "org.mongodb" % "mongodb-driver-sync" % "4.6.1",
    // Added to replace javax.xml.bind (removed in Java 11)
    "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.8"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapPlayVersion % testScope,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % testScope,
    "com.vladsch.flexmark" % "flexmark-all" % "0.64.0" % testScope,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.12" % "test",
    "org.scalatest" %% "scalatest" % "3.2.15" % testScope,
    "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0" % testScope,
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % testScope
  )
}
