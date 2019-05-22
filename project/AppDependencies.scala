import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val wireMockVersion = "2.23.2"
  private val testScope = "test,it"

  val compile = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.19.0-play-26",
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.39.0",
    "uk.gov.hmrc" %% "wco-dec" % "0.30.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "4.6.0",
    "com.typesafe.play" %% "play-json-joda" % "2.6.10"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.8.0-play-26" % testScope,
    "org.scalatest" %% "scalatest" % "3.0.5" % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test",
    "com.github.tomakehurst" % "wiremock" % wireMockVersion % testScope exclude("org.apache.httpcomponents","httpclient") exclude("org.apache.httpcomponents","httpcore"),
   "org.pegdown" % "pegdown" % "1.6.0" % testScope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope,
    "org.mockito" % "mockito-core" % "2.27.0" % "test"
  )

  val jettyVersion = "9.2.26.v20180806"

  val jettyOverrides = Set(
    "org.eclipse.jetty" % "jetty-server" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-security" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-servlets" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-continuation" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-xml" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-client" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-http" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-io" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty" % "jetty-util" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty.websocket" % "websocket-api" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty.websocket" % "websocket-common" % jettyVersion % IntegrationTest,
    "org.eclipse.jetty.websocket" % "websocket-client" % jettyVersion % IntegrationTest
  )
}
