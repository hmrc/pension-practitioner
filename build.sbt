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
  .settings(scalaVersion := "3.4.1")
  .settings(
    RoutesKeys.routesImport ++= Seq("models.enumeration.JourneyType",
      "models.SchemeReferenceNumber"),
    PlayKeys.devSettings += "play.server.http.port" -> "8209",
    scalacOptions ++= Seq(
      "-feature",
      "-Xfatal-warnings",                        // Treat all warnings as errors
      "-Wconf:src=target/.*:s",                  // silence warnings from compiled files
      "-Wconf:msg=Flag.*repeatedly:silent",      // Suppress warnings for repeated flags
      "-Wconf:src=routes/.*:s"
    )
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
