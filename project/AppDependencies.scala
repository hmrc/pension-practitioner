import play.sbt.PlayImport.caffeine
import sbt.*

object AppDependencies {
  private val bootstrapVersion = "10.4.0"
  private val mongoVersion = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.networknt"                 % "json-schema-validator"      % "1.5.7",
    "uk.gov.hmrc"                  %% "domain-play-30"             % "11.0.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.19.2",
    "org.typelevel"                %% "cats-effect"                % "3.6.3",
    caffeine
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVersion        % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion    % Test
  )
}
