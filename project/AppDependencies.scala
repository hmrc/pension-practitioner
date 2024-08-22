import play.sbt.PlayImport.ehcache
import sbt._

object AppDependencies {
  private val bootstrapVersion = "8.4.0"
  private val mongoVersion = "1.7.0"

  val compile = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.networknt"           %  "json-schema-validator"      % "1.0.76",
    "uk.gov.hmrc"             %% "domain-play-30"             % "9.0.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.17.0",
    ehcache
  )

  val test = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVersion        % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion    % Test,
    "com.vladsch.flexmark"    % "flexmark-all"                % "0.64.8"            % "test, it",
    "org.pegdown"             %  "pegdown"                    % "1.6.0"             % "test, it",
    "org.scalatest"           %% "scalatest"                  % "3.2.17"            % Test,
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.17.0"          % Test,
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.15.0"          % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "7.0.1"             % Test,
  )
}
