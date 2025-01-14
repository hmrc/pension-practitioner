import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val coverageExcludedFiles = "<empty>;Reverse.*;.*repository.*;" +
    ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;.*MigrationService"

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedFiles := coverageExcludedFiles,
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
  )
}
