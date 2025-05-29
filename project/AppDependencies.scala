import play.sbt.PlayImport.caffeine
import sbt._

object AppDependencies {
  private val bootstrapVersion = "9.12.0"
  private val mongoVersion = "2.6.0"

  val compile = Seq(
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.networknt"                 % "json-schema-validator"      % "1.5.7",
    "uk.gov.hmrc"                  %% "domain-play-30"             % "11.0.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.19.0",
    caffeine
  )

  val test = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVersion        % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion    % Test
  )
}
