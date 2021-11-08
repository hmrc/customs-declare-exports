import play.core.PlayVersion
import sbt._

object AppDependencies {

  private val testScope = "test, it"

  val compile = Seq(
    "uk.gov.hmrc"                    %%  "simple-reactivemongo"         % "8.0.0-play-28",
    "uk.gov.hmrc"                    %%  "bootstrap-backend-play-28"    % "5.16.0",
    "uk.gov.hmrc"                    %%  "wco-dec"                      % "0.36.0",
    "uk.gov.hmrc"                    %%  "logback-json-logger"          % "5.1.0",
    "com.github.cloudyrock.mongock"  %   "mongock-core"                 % "2.0.2",
    "org.mongodb.scala"              %%  "mongo-scala-driver"           % "2.9.0",
    "com.github.tototoshi"           %%  "scala-csv"                    % "1.3.8",
    "uk.gov.hmrc"                    %%  "work-item-repo"               % "8.1.0-play-28",
    "com.fasterxml.jackson.module"   %%  "jackson-module-scala"         % "2.12.3"
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"                % "3.2.9"             % testScope,
    "org.scalatest"          %% "scalatest-featurespec"    % "3.2.9"             % testScope,
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"             % testScope,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.36.8"            % testScope,
    "com.github.tomakehurst" %  "wiremock-jre8"            % "2.28.1"            % testScope,
    "org.mockito"            %% "mockito-scala"            % "1.16.37"           % "test"
  )
}
