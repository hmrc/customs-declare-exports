import sbt.Tests.{Group, SubProcess}
import sbt.{IntegrationTest, _}
import uk.gov.hmrc.DefaultBuildSettings._
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
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test(),
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    majorVersion := 0,
    scalaVersion := "2.12.8"
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
    unmanagedSourceDirectories in Test := Seq(
      (baseDirectory in Test).value / "test/unit",
      (baseDirectory in Test).value / "test/util"
    ),
    addTestReportOption(Test, "test-reports")
  )
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := Seq(
      (baseDirectory in IntegrationTest).value / "test/it",
      (baseDirectory in Test).value / "test/util"
    ),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false
  )
  .settings(
    Keys.fork in ComponentTest := false,
    unmanagedSourceDirectories in ComponentTest := Seq(
      (baseDirectory in ComponentTest).value / "test/component",
      (baseDirectory in Test).value / "test/util"
    ),
    addTestReportOption(ComponentTest, "comp-test-reports"),
    testGrouping in ComponentTest := oneForkedJvmPerTest((definedTests in ComponentTest).value),
    parallelExecution in ComponentTest := false
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
