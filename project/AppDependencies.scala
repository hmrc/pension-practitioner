import play.sbt.PlayImport.ehcache
import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"      % "0.64.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.14.0",
    "com.networknt"           %  "json-schema-validator"      % "1.0.3",
    "uk.gov.hmrc"             %% "domain"                     % "6.2.0-play-28",
    ehcache
  )

  val test = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.64.0"            % Test,
    "com.github.simplyscala"  %% "scalatest-embedmongo"       % "0.2.4"             % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "5.14.0"   % Test,
    "com.typesafe.play"       %% "play-test"                % current   % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % "test, it",
    "org.pegdown"             %  "pegdown"                    % "1.6.0"       % "test, it",
    "org.mockito"             %% "mockito-scala"              % "1.16.42"     % "test",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.16.42"     % "test",
    "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.31.0"      % "test",
    "org.scalatestplus"       %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2" % "test",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.5"
  )
}
