import play.sbt.routes.RoutesKeys
import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "pension-practitioner"

lazy val microservice = Project(appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(scalaVersion := "2.13.8")
  .settings(publishingSettings: _*)
  .settings(
    RoutesKeys.routesImport ++= Seq("models.enumeration.JourneyType",
      "models.FeatureToggleName"),
    PlayKeys.devSettings += "play.server.http.port" -> "8209",
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(
    Test / parallelExecution := true
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
  )
  .settings(resolvers += Resolver.jcenterRepo)
