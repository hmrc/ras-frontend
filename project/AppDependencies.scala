import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
		"uk.gov.hmrc" %% "bootstrap-play-25" % "5.3.0",
		"uk.gov.hmrc" %% "govuk-template" % "5.55.0-play-25",
	  "uk.gov.hmrc" %% "play-ui" % "8.10.0-play-25",
    "uk.gov.hmrc" %% "play-partials" % "6.11.0-play-25",
    "uk.gov.hmrc" %% "auth-client" % "2.35.0-play-25",
    "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-25"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25",
    "org.scalatest" %% "scalatest" % "3.0.8",
    "org.pegdown" % "pegdown" % "1.6.0",
    "org.jsoup" % "jsoup" % "1.12.2",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
    "org.mockito" % "mockito-core" % "3.3.3",
    "org.scalacheck" %% "scalacheck" % "1.14.3",
		"uk.gov.hmrc" %% "domain" % "5.9.0-play-25"
	).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
