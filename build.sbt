import sbt.Tests.{Group, SubProcess}
import sbt.{IntegrationTest, _}
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.gitstamp.GitStampPlugin._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}
import uk.gov.hmrc.versioning.SbtGitVersioning

val appName = "customs-declare-exports"

PlayKeys.devSettings := Seq("play.server.http.port" -> "6792")

lazy val allResolvers = resolvers ++= Seq(
  Resolver.bintrayRepo("hmrc", "releases"),
  Resolver.jcenterRepo
)

lazy val IntegrationTest = config("it") extend Test
lazy val ComponentTest = config("component") extend Test

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] = {
  tests map {
    //test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    test => Group(test.name, Seq(test), SubProcess(ForkOptions()))
  }
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    majorVersion := 0,
    scalaVersion := "2.12.11"
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .configs(ComponentTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(inConfig(ComponentTest)(Defaults.itSettings): _*)
  .settings(
    commonSettings,
    allResolvers,
    scoverageSettings
  )
  .settings(
    Test / unmanagedSourceDirectories := Seq(
      (Test / baseDirectory).value / "test/unit",
      (Test / baseDirectory).value / "test/util"
    ),
    addTestReportOption(Test, "test-reports")
  )
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := Seq(
      (IntegrationTest / baseDirectory).value / "test/it",
      (Test / baseDirectory).value / "test/util"
    ),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    IntegrationTest / parallelExecution := false
  )
  .settings(
    Keys.fork in ComponentTest := false,
    ComponentTest / unmanagedSourceDirectories := Seq(
      (ComponentTest / baseDirectory).value / "test/component",
      (Test / baseDirectory).value / "test/util"
    ),
    addTestReportOption(ComponentTest, "int-test-reports"),
    ComponentTest / testGrouping := oneForkedJvmPerTest((definedTests in ComponentTest).value),
    ComponentTest / parallelExecution := false
  )

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
    "<empty>"
    ,"Reverse.*"
    ,"domain\\..*"
    ,"models\\..*"
    ,"metrics\\..*"
    ,".*(BuildInfo|Routes|Options).*"
  ).mkString(";"),
  coverageMinimum := 85,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

lazy val commonSettings: Seq[Setting[_]] = scalaSettings ++ publishingSettings ++ defaultSettings() ++ gitStampSettings
