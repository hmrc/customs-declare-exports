import play.core.PlayVersion
import sbt._

object AppDependencies {

  private val testScope = "test, it"

  val compile = Seq(
    "uk.gov.hmrc"                    %%  "simple-reactivemongo"         % "7.31.0-play-27",
    "uk.gov.hmrc"                    %%  "bootstrap-backend-play-27"    % "3.3.0",
    "uk.gov.hmrc"                    %%  "wco-dec"                      % "0.35.0",
    "uk.gov.hmrc"                    %%  "logback-json-logger"          % "4.8.0",
    "com.github.cloudyrock.mongock"  %   "mongock-core"                 % "2.0.2",
    "org.mongodb.scala"              %%  "mongo-scala-driver"           % "2.9.0",
    "com.github.tototoshi"           %%  "scala-csv"                    % "1.3.6",
    "uk.gov.hmrc"                    %%  "work-item-repo"               % "7.11.0-play-27"
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"                % "3.2.3"             % testScope,
    "org.scalatest"          %% "scalatest-featurespec"    % "3.2.3"             % testScope,
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "4.0.3"             % testScope,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.36.8"            % testScope,
    "com.github.tomakehurst" %  "wiremock-jre8"            % "2.27.2"            % testScope,
    "org.mockito"            %% "mockito-scala"            % "1.16.32"           % "test"
  )
}
