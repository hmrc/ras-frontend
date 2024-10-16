import sbt.*

object AppDependencies {
  val bootstrapVersion = "9.5.0"
  val hmrcMongoVersion = "2.0.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "9.11.0",
    "uk.gov.hmrc"       %% "domain-play-30"             % "9.0.0",
    "uk.gov.hmrc"       %% "tax-year"                   % "4.0.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % hmrcMongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-30"  % hmrcMongoVersion,
    "org.jsoup"             % "jsoup"                     % "1.18.1",
    "org.scalatestplus"     %% "scalacheck-1-17"          % "3.2.18.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}