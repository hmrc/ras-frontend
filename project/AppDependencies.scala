import sbt.*

object AppDependencies {
  val bootstrapVersion = "7.23.0"
  val hmrcMongoVersion = "1.3.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-28" % "8.5.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"         % hmrcMongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-28"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-28"  % hmrcMongoVersion,
    "org.scalatest"         %% "scalatest"                % "3.2.18",
    "org.jsoup"             % "jsoup"                     % "1.17.2",
    "org.scalatestplus"     %% "mockito-5-10"             % "3.2.18.0",
    "org.scalatestplus"     %% "scalacheck-1-17"          % "3.2.18.0",
    "uk.gov.hmrc"           %% "domain"                   % "8.3.0-play-28",
    "com.vladsch.flexmark"  % "flexmark-all"              % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}