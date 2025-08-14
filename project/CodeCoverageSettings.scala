import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val coverageExcludedFiles: Seq[String] = Seq(
      "<empty>",
      "Reverse.*",
      ".*repository.*",
      ".*BuildInfo.*",
      ".*javascript.*",
      ".*Routes.*",
      ".*GuiceInjector",
      ".*AuditService.*",
      ".*\\$anon.*",
      ".*EnumPathBinder.*"
    )

  private val implicitOFormatObjects: Seq[String] = Seq(
      ".*MinimalDetailsEvent.*",
      ".*PSPDetails.*",
      ".*SchemeDetails.*",
      ".*ListOfSchemes.*",
      ".*PSPRegistration.*"
    )

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedFiles := (coverageExcludedFiles ++ implicitOFormatObjects).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
  )
}
