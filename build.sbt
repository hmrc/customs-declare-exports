import play.sbt.routes.RoutesKeys
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.gitstamp.GitStampPlugin._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "customs-declare-exports"

PlayKeys.devSettings := Seq("play.server.http.port" -> "6792")

RoutesKeys.routesImport ++= Seq(
  "uk.gov.hmrc.exports.models.DeclarationSort",
  "uk.gov.hmrc.exports.models.Page"
)

lazy val IntegrationTest = config("it") extend Test

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(gitStampSettings: _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(scoverageSettings)
  .settings(
    Test / unmanagedSourceDirectories := Seq((Test / baseDirectory).value / "test/unit", (Test / baseDirectory).value / "test/util"),
    Test / unmanagedResourceDirectories := Seq(baseDirectory.value / "test" / "resources"),
    Test / javaOptions ++= Seq("-Dconfig.resource=test.application.conf"),
    addTestReportOption(Test, "test-reports"),
    Test / Keys.fork := true
  )
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := Seq(
      (IntegrationTest / baseDirectory).value / "test/it",
      (Test / baseDirectory).value / "test/util"
    ),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / parallelExecution := false
  )
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

lazy val commonSettings = List(
  majorVersion := 0,
  scalaVersion := "2.13.8",
  scalacOptions ++= scalacFlags,
  retrieveManaged := true,
  dependencyOverrides += "commons-codec" % "commons-codec" % "1.15",
  libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
)

lazy val scalacFlags = List(
  "-deprecation",                                // warn about use of deprecated APIs
  "-encoding", "UTF-8",                          // source files are in UTF-8
  "-feature",                                    // warn about misused language features
  "-language:implicitConversions",
  "-unchecked",                                  // warn about unchecked type parameters
  //"-Wconf:any:warning-verbose",
  "-Wconf:cat=unused-imports&src=routes/.*:s",   // silent "unused import" warnings from Play routes
  "-Wextra-implicit",
  "-Xcheckinit",
  "-Xfatal-warnings",                            // warnings are fatal!!
  "-Ywarn-numeric-widen",
)

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
    "<empty>",
    "Reverse.*",
    "domain\\..*",
    "models\\..*",
    "metrics\\..*",
    ".*(BuildInfo|Routes|Options).*"
  ).mkString(";"),
  coverageMinimumStmtTotal := 85,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  Test / parallelExecution := false
)
