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
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.25.0",
  "uk.gov.hmrc" %% "http-caching-client"        % "9.5.0-play-28",
  "uk.gov.hmrc" %% "time"                       % "3.25.0",
  "uk.gov.hmrc" %% "play-frontend-hmrc"         % "6.2.0-play-28",
  compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
)

val test: Seq[ModuleID] = Seq(
  "org.scalatest"          %% "scalatest"          % "3.2.15",
  "org.pegdown"            %  "pegdown"            % "1.6.0",
  "org.jsoup"              %  "jsoup"              % "1.15.4",
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
  "org.mockito"            %  "mockito-core"       % "5.2.0",
  "org.scalacheck"         %% "scalacheck"         % "1.17.0",
  "uk.gov.hmrc"            %% "domain"             % "8.1.0-play-28"
).map(_ % "test")

val all: Seq[ModuleID] = compile ++ test

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;testOnlyDoNotUseInAppConf.*;conf.*;models.*;views.*;app.*;uk.gov.hmrc.*;prod.*;connectors.*",
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
    defaultSettings(),
    scalaSettings,
    scalaVersion := "2.12.12",
    PlayKeys.playDefaultPort := 9673,
    libraryDependencies ++= all,
    retrieveManaged := true
  )

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

scalacOptions ++= Seq(
  "-P:silencer:pathFilters=views;routes"
)

addCommandAlias("scalastyleAll", "all scalastyle test:scalastyle")
