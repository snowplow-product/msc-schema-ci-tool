addCommandAlias("fmt", "all scalafmtSbt scalafmtAll; all scalafixAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll; all scalafixAll --check")

lazy val root = (project in file("."))
  .settings(
    organization := "com.snowplowanalytics",
    name := "data-structures-ci",
    description := "A CLI helper tool for common CI/CD scenarios when developing Snowplow Schemas",
    version := "0.3.3",
    scalaVersion := "2.13.3",
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies ++= Build.dependencies,
    semanticdbEnabled in ThisBuild := true,
    semanticdbVersion in ThisBuild := scalafixSemanticdb.revision,
    scalafixDependencies in ThisBuild := Build.scalafixDependencies,
    scalafixScalaBinaryVersion in ThisBuild := CrossVersion.binaryScalaVersion(scalaVersion.value),
    onLoadMessage := Build.welcomeMessage,
    resolvers += Resolver.bintrayRepo("snowplow", "snowplow-maven"),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .enablePlugins(ScalafmtPlugin, BuildInfoPlugin)
  .settings(Build.sbtAssemblySettings)
  .settings(Build.sbtBuildInfoSettings)
