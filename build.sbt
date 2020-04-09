addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("checkfmt", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = (project in file("."))
  .settings(
    organization := "com.snowplowanalytics",
    name := "schema-ci",
    version := "0.1.0",
    scalaVersion := "2.12.11",
    libraryDependencies ++= Build.dependencies,
    onLoadMessage := Build.welcomeMessage,
    onChangedBuildSource := ReloadOnSourceChanges
  )
  .enablePlugins(ScalafmtPlugin)
  .settings(Build.sbtAssemblySettings)