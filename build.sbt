import play.core.PlayVersion
import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "ras-frontend"

lazy val plugins: Seq[Plugins] =
  Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)

lazy val playSettings: Seq[Setting[_]] = Seq.empty

val compile: Seq[ModuleID] = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-play-26"   % "2.3.0",
  "uk.gov.hmrc" %% "govuk-template"      % "5.63.0-play-26",
  "uk.gov.hmrc" %% "auth-client"         % "3.3.0-play-26",
  "uk.gov.hmrc" %% "http-caching-client" % "9.2.0-play-26",
  "uk.gov.hmrc" %% "time"                % "3.19.0",
  "uk.gov.hmrc" %% "play-frontend-hmrc" % "0.54.0-play-26"
)

val test: Seq[ModuleID] = Seq(
  "uk.gov.hmrc"            %% "hmrctest"           % "3.10.0-play-26",
  "org.scalatest"          %% "scalatest"          % "3.0.9",
  "org.pegdown"            % "pegdown"             % "1.6.0",
  "org.jsoup"              % "jsoup"               % "1.13.1",
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3",
  "org.mockito"            % "mockito-core"        % "3.7.7",
  "org.scalacheck"         %% "scalacheck"         % "1.15.3",
  "uk.gov.hmrc"            %% "domain"             % "5.10.0-play-26"
).map(_ % "test")

val all: Seq[ModuleID] = compile ++ test

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;testOnlyDoNotUseInAppConf.*;conf.*;models.*;views.*;app.*;uk.gov.hmrc.*;prod.*",
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    majorVersion := 0,
    playSettings,
    scoverageSettings,
    publishingSettings,
    defaultSettings(),
    scalaSettings,
    scalaVersion := "2.12.12",
    PlayKeys.playDefaultPort := 9673,
    libraryDependencies ++= all,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := InjectedRoutesGenerator,
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    )
  )

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.govukfrontend.views.html.helpers._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)
