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
  Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)

val silencerVersion = "1.7.1"

val compile: Seq[ModuleID] = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.20.0",
  "uk.gov.hmrc" %% "http-caching-client"        % "9.5.0-play-28",
  "uk.gov.hmrc" %% "time"                       % "3.25.0",
  "uk.gov.hmrc" %% "play-frontend-hmrc"         % "1.31.0-play-28",
  compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
)

val test: Seq[ModuleID] = Seq(
  "org.scalatest"          %% "scalatest"          % "3.0.9",
  "org.pegdown"            %  "pegdown"            % "1.6.0",
  "org.jsoup"              %  "jsoup"              % "1.13.1",
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current,
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3",
  "org.mockito"            %  "mockito-core"       % "3.7.7",
  "org.scalacheck"         %% "scalacheck"         % "1.15.4",
  "uk.gov.hmrc"            %% "domain"             % "7.0.0-play-28"
).map(_ % "test")

val all: Seq[ModuleID] = compile ++ test

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;testOnlyDoNotUseInAppConf.*;conf.*;models.*;views.*;app.*;uk.gov.hmrc.*;prod.*",
    ScoverageKeys.coverageMinimum := 86,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    majorVersion := 0,
    scoverageSettings,
    publishingSettings,
    defaultSettings(),
    scalaSettings,
    scalaVersion := "2.12.12",
    PlayKeys.playDefaultPort := 9673,
    libraryDependencies ++= all,
    retrieveManaged := true
  )

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

scalacOptions ++= Seq(
  "-P:silencer:pathFilters=views;routes"
)