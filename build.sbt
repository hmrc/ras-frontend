import play.core.PlayVersion
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "ras-frontend"
val bootstrapVersion = "7.19.0"

lazy val plugins: Seq[Plugins] =
  Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)

val compile: Seq[ModuleID] = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapVersion,
  "uk.gov.hmrc" %% "http-caching-client"        % "10.0.0-play-28",
  "uk.gov.hmrc" %% "play-frontend-hmrc"         % "7.13.0-play-28"
)

val test: Seq[ModuleID] = Seq(
  "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapVersion,
  "org.scalatest"          %% "scalatest"               % "3.2.16",
  "org.pegdown"            %  "pegdown"                 % "1.6.0",
  "org.jsoup"              %  "jsoup"                   % "1.16.1",
  "com.typesafe.play"      %% "play-test"               % PlayVersion.current,
  "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
  "org.mockito"            %% "mockito-scala-scalatest" % "1.17.14",
  "org.scalacheck"         %% "scalacheck"              % "1.17.0",
  "uk.gov.hmrc"            %% "domain"                  % "8.3.0-play-28",
  "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.8"
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

scalacOptions ++= Seq(
  "-Wconf:cat=unused-imports&src=html/.*:s",
  "-Wconf:src=routes/.*:s",
  "-feature"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    majorVersion := 0,
    scoverageSettings,
    defaultSettings(),
    scalaSettings,
    scalaVersion := "2.13.10",
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
