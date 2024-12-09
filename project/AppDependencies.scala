import play.sbt.PlayImport.caffeine
import sbt._

object AppDependencies {
  private val bootstrapVersion = "9.5.0"
  private val mongoVersion = "2.3.0"

  val compile = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.networknt"           %  "json-schema-validator"      % "1.5.1",
    "uk.gov.hmrc"             %% "domain-play-30"             % "10.0.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.17.2",
    caffeine
  )

  val test = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVersion        % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion    % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.19"            % Test,
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.18.0"          % Test,
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.15.0"          % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "7.0.1"             % Test,
  )
}
