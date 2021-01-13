import sbt._

object AppDependencies {

  val compile = Seq(

    "uk.gov.hmrc"             %% "simple-reactivemongo"     % "7.31.0-play-26",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-26"        % "3.2.0",
    "com.networknt"           %  "json-schema-validator"    % "1.0.3",
    "uk.gov.hmrc"             %% "domain"                   % "5.10.0-play-26"
  )

  val test = Seq(
//    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.16.0"  % Test classifier "tests",
    "org.scalatest"           %% "scalatest"                % "3.0.8"   % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"   % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.2"   % "test, it",
    "org.mockito"             %  "mockito-all"              % "1.10.19" % "test",
    "org.scalacheck"          %% "scalacheck"               % "1.14.0"  % "test",
    "com.github.tomakehurst"  %  "wiremock-jre8"            % "2.26.0"  % "test",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.2"   % "test, it"
  )

}
