import sbt._

object Dependencies {

  private val bootstrapPlayVersion = "9.19.0"
  private val hmrcMongoVersion = "2.11.0"
  private val flexmark = "flexmark-all"

  private val compile = List(
    "uk.gov.hmrc"            %% "bootstrap-backend-play-30"          % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-work-item-repo-play-30"  % hmrcMongoVersion,
    "uk.gov.hmrc"            %% "wco-dec"                            % "0.43.0",
    "commons-codec"          %  "commons-codec"                      % "1.20.0",
    "org.glassfish.jaxb"     %  "jaxb-runtime"                       % "4.0.6",
  )

  private val test = List(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"             % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"            % hmrcMongoVersion,
    "com.vladsch.flexmark"   %  flexmark                             % "0.64.8",
    "org.scalatestplus"      %% "mockito-4-11"                       % "3.2.18.0",
    "org.scalatest"          %% "scalatest"                          % "3.2.19",
    "org.scalatestplus"      %% "scalacheck-1-15"                    % "3.2.11.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] =
    (compile ++ test).map(moduleId => if (moduleId.name == flexmark) moduleId else moduleId.withSources)
}
