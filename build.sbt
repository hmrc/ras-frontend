import sbt.Keys.*
import sbt.*
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

val appName = "ras-frontend"

ThisBuild / scalaVersion := "2.13.13"
ThisBuild / majorVersion := 0

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;testOnlyDoNotUseInAppConf.*;conf.*;models.*;views.*;app.*;uk.gov.hmrc.*;prod.*;connectors.*",
    ScoverageKeys.coverageMinimumStmtTotal := 86,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:cat=unused-imports&src=html/.*:s",
      "-Wconf:src=routes/.*:s",
      "-feature"
    ),
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    ),
    PlayKeys.playDefaultPort := 9673,
    libraryDependencies ++= AppDependencies(),
  )

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
