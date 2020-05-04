addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt; compile:scalafix; test:scalafix")
addCommandAlias(
  "checkfmt",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck; compile:scalafix --check; test:scalafix --check"
)

lazy val root = (project in file("."))
  .settings(
    organization := "com.snowplowanalytics",
    name := "schema-ci",
    description := "A CLI helper tool for common CI/CD scenarios when developing Snowplow Schemas",
    version := "0.1.0",
    scalaVersion := "2.12.11",
    scalacOptions += "-Yrangepos",
    libraryDependencies ++= Build.dependencies,
    scalafixDependencies in ThisBuild ++= Build.scalafixDependencies,
    onLoadMessage := Build.welcomeMessage,
    onChangedBuildSource := ReloadOnSourceChanges,
    resolvers += Resolver.bintrayRepo("snowplow", "snowplow-maven"),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .enablePlugins(ScalafmtPlugin, BuildInfoPlugin)
  .settings(Build.sbtAssemblySettings)
  .settings(Build.sbtBuildInfoSettings)
