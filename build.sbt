import play.sbt.routes.RoutesKeys
import sbt.Keys.scalacOptions

val appName = "pension-practitioner"

lazy val microservice = Project(appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(scalaVersion := "2.13.12")
  .settings(
    RoutesKeys.routesImport ++= Seq("models.enumeration.JourneyType",
      "models.SchemeReferenceNumber"),
    PlayKeys.devSettings += "play.server.http.port" -> "8209",
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(
    Test / parallelExecution := true
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
  )
