import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / majorVersion := 0

lazy val microservice = Project("ras-frontend", file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
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
  .settings(CodeCoverageSettings())

