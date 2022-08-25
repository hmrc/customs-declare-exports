import sbt._

object AppDependencies {

  val bootstrapPlayVersion = "5.24.0"
  val hmrcMongoVersion = "0.68.0"
  val testScope = "test,it"

  val compile = Seq(
    "uk.gov.hmrc"                    %% "bootstrap-backend-play-28"         % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"              %% "hmrc-mongo-play-28"                % hmrcMongoVersion,
    "uk.gov.hmrc.mongo"              %% "hmrc-mongo-work-item-repo-play-28" % hmrcMongoVersion,
    "uk.gov.hmrc"                    %% "wco-dec"                           % "0.37.0",
    "uk.gov.hmrc"                    %% "logback-json-logger"               % "5.2.0",
    "com.github.tototoshi"           %% "scala-csv"                         % "1.3.10",
    "com.fasterxml.jackson.module"   %% "jackson-module-scala"              % "2.13.3",
    // Used by the Migration tool. Keep this library's version to the same major.minor version as the mongo-scala-driver.
    "org.mongodb"                    %  "mongodb-driver-sync"               % "4.6.0",
    // Added to replace javax.xml.bind (removed in Java 11)
    "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.6"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapPlayVersion % testScope,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion     % testScope,
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.36.8"             % testScope,
    "com.github.tomakehurst" %  "wiremock-jre8"           % "2.33.2"             % testScope,
    "org.mockito"            %% "mockito-scala"           % "1.17.7"             % testScope
  )
}
