addCommandAlias("fmt", "all scalafmtSbt scalafmtAll; all scalafixAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll; all scalafixAll --check")

lazy val root = (project in file("."))
  .settings(
    organization := "com.snowplowanalytics",
    name := "data-structures-ci",
    description := "A CLI helper tool for common CI/CD scenarios when developing Snowplow Schemas",
    version := "1.0.0",
    scalaVersion := "2.13.7",
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies ++= Build.dependencies,
    ThisBuild / semanticdbEnabled := true,
    ThisBuild / semanticdbVersion := scalafixSemanticdb.revision,
    ThisBuild / scalafixDependencies := Build.scalafixDependencies,
    ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
    onLoadMessage := Build.welcomeMessage,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .enablePlugins(ScalafmtPlugin, BuildInfoPlugin)
  .settings(Build.sbtAssemblySettings)
  .settings(Build.sbtBuildInfoSettings)
