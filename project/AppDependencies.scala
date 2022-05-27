import sbt._

object AppDependencies {

  val bootstrapPlayVersion = "5.24.0"
  val hmrcMongoVersion = "0.64.0"
  val testScope = "test,it"

  val compile = Seq(
    "uk.gov.hmrc"                    %% "bootstrap-backend-play-28"         % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"              %% "hmrc-mongo-play-28"                % hmrcMongoVersion,
    "uk.gov.hmrc.mongo"              %% "hmrc-mongo-work-item-repo-play-28" % hmrcMongoVersion,
    "uk.gov.hmrc"                    %% "wco-dec"                           % "0.36.0",
    "uk.gov.hmrc"                    %% "logback-json-logger"               % "5.2.0",
    "com.github.tototoshi"           %% "scala-csv"                         % "1.3.9",
    "com.fasterxml.jackson.module"   %% "jackson-module-scala"              % "2.13.3",
    // Used by the Migration tool. Try to keep it to the same version of mongo-scala-driver.
    "org.mongodb"                    %  "mongodb-driver-sync"               % "4.6.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapPlayVersion % testScope,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion     % testScope,
    //"org.scalatest"          %% "scalatest-featurespec"    % "3.2.9"             % testScope,
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.36.8"             % testScope,
    "com.github.tomakehurst" %  "wiremock-jre8"           % "2.33.2"             % testScope,
    "org.mockito"            %% "mockito-scala"           % "1.17.5"             % "test"
  )
}
