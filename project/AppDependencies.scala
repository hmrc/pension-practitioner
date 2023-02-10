import play.sbt.PlayImport.ehcache
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.74.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "7.13.0",
    "com.networknt"           %  "json-schema-validator"      % "1.0.76",
    "uk.gov.hmrc"             %% "domain"                     % "8.1.0-play-28",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.14.2",
    ehcache
  )

  val test = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.74.0"            % Test,
    "de.flapdoodle.embed"     %  "de.flapdoodle.embed.mongo"  % "3.5.1"             % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "7.13.0"            % Test,
    "com.vladsch.flexmark"    % "flexmark-all"                % "0.64.0"            % "test, it",
    "org.pegdown"             %  "pegdown"                    % "1.6.0"             % "test, it",
    "org.scalatest"           %% "scalatest"                  % "3.2.15"            % Test,
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.15.0"          % Test,
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.15.0"          % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"             % Test,
    "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.35.0"            % Test
  )
}
