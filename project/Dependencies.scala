import sbt._

object Dependencies {

  val bootstrapPlayVersion = "9.4.0"
  val hmrcMongoVersion = "2.2.0"

  val compile: Seq[ModuleID] = List(
    "uk.gov.hmrc"            %% "bootstrap-backend-play-30"          % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-work-item-repo-play-30"  % hmrcMongoVersion,
    "uk.gov.hmrc"            %% "wco-dec"                            % "0.39.0",
    "com.github.tototoshi"   %% "scala-csv"                          % "1.4.1",
    "commons-codec"          %  "commons-codec"                      % "1.17.1",
//    "org.glassfish.jaxb"     %  "jaxb-runtime"                       % "4.0.5",
//    "jakarta.xml.bind"       %  "jakarta.xml.bind-api"               % "4.0.2",
    "org.glassfish.jaxb"     %  "jaxb-runtime"                       % "2.3.8",
    "javax.xml.bind"         %  "jaxb-api"                           % "2.3.1"
  )

  val test: Seq[ModuleID] = List(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"             % bootstrapPlayVersion % "test",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"            % hmrcMongoVersion     % "test",
    "com.vladsch.flexmark"   %  "flexmark-all"                       % "0.64.8"             % "test",
    "org.mockito"            %% "mockito-scala-scalatest"            % "1.17.37"            % "test",
    "org.scalatest"          %% "scalatest"                          % "3.2.19"             % "test",
    "org.scalatestplus"      %% "scalacheck-1-15"                    % "3.2.11.0"           % "test"
  )

  def apply(): Seq[ModuleID] =
    (compile ++ test).map(moduleId => if (moduleId.name == "flexmark-all") moduleId else moduleId.withSources)
}
