import sbt.*

object AppDependencies {
  val bootstrapVersion = "7.21.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc" %% "http-caching-client"        % "10.0.0-play-28",
    "uk.gov.hmrc" %% "play-frontend-hmrc"         % "7.17.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-28" % bootstrapVersion,
    "org.scalatest"         %% "scalatest"              % "3.2.16",
    "org.jsoup"             % "jsoup"                   % "1.16.1",
    "org.scalatestplus"     %% "mockito-4-11"           % "3.2.16.0",
    "org.scalatestplus"     %% "scalacheck-1-17"        % "3.2.16.0",
    "uk.gov.hmrc"           %% "domain"                 % "8.3.0-play-28",
    "com.vladsch.flexmark"  % "flexmark-all"            % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}