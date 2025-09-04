import sbt.Setting
import scoverage.ScoverageKeys.*

object CodeCoverageSettings {

  private val settings: Seq[Setting[?]] = Seq(
    coverageExcludedPackages := "<empty>;Reverse.*;.*connectors.*;..*Routes.*",
    coverageMinimumStmtTotal := 86,
    coverageFailOnMinimum := true,
    coverageHighlighting := true
  )

  def apply(): Seq[Setting[?]] = settings

}