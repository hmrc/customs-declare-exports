import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % "6.7.0",
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.11.0",
    "uk.gov.hmrc" %% "wco-dec" % "0.30.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "4.6.0"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.8.0-play-25" % scope,
    "org.scalatest" %% "scalatest" % "3.0.5" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % "test",
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" % "mockito-core" % "2.27.0" % "test"
  )
}
