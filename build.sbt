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

val compile: Seq[ModuleID] = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "7.15.0",
  "uk.gov.hmrc" %% "http-caching-client"        % "10.0.0-play-28",
  "uk.gov.hmrc" %% "time"                       % "3.25.0",
  "uk.gov.hmrc" %% "play-frontend-hmrc"         % "7.3.0-play-28",
)

val test: Seq[ModuleID] = Seq(
  "org.scalatest"          %% "scalatest"               % "3.2.15",
  "org.pegdown"            %  "pegdown"                 % "1.6.0",
  "org.jsoup"              %  "jsoup"                   % "1.15.4",
  "com.typesafe.play"      %% "play-test"               % PlayVersion.current,
  "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
  "org.mockito"            %% "mockito-scala-scalatest" % "1.17.14",
  "org.scalacheck"         %% "scalacheck"              % "1.17.0",
  "uk.gov.hmrc"            %% "domain"                  % "8.2.0-play-28",
  "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.0"
).map(_ % "test")

val all: Seq[ModuleID] = compile ++ test

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;testOnlyDoNotUseInAppConf.*;conf.*;models.*;views.*;app.*;uk.gov.hmrc.*;prod.*;connectors.*",
    ScoverageKeys.coverageMinimumStmtTotal := 86,
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
    scalaVersion := "2.12.15",
    // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
    // Try to remove when sbt 1.8.0+ and scoverage is 2.0.7+
    ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    PlayKeys.playDefaultPort := 9673,
    libraryDependencies ++= all,
    retrieveManaged := true
  )

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

addCommandAlias("scalastyleAll", "all scalastyle test:scalastyle")
