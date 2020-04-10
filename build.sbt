addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("checkfmt", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = (project in file("."))
  .settings(
    organization := "com.snowplowanalytics",
    name := "schema-ci",
    description := "A CLI helper tool for common CI/CD scenarios when developing Snowplow Schemas",
    version := "0.1.0",
    scalaVersion := "2.12.11",
    libraryDependencies ++= Build.dependencies,
    onLoadMessage := Build.welcomeMessage,
    onChangedBuildSource := ReloadOnSourceChanges,
    resolvers += Resolver.bintrayRepo("snowplow", "snowplow-maven")
  )
  .enablePlugins(ScalafmtPlugin, BuildInfoPlugin)
  .settings(Build.sbtAssemblySettings)
  .settings(Build.sbtBuildInfoSettings)
