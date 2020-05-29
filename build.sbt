addCommandAlias(
  "fmt",
  "scalafix RemoveUnused; test:scalafix RemoveUnused; all scalafix test:scalafix; all scalafmtSbt scalafmt test:scalafmt"
)
addCommandAlias(
  "checkfmt",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck; all compile:scalafix --check; all test:scalafix --check;"
)

lazy val root = (project in file("."))
  .settings(
    organization := "com.snowplowanalytics",
    name := "data-structures-ci",
    description := "A CLI helper tool for common CI/CD scenarios when developing Snowplow Schemas",
    version := "0.3.0",
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
