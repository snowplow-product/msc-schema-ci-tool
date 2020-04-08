addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("packageJar", "assembly")

lazy val root = (project in file("."))
  .settings(
    organization := "com.snowplowanalytics",
    name := "msc-ci-tool",
    version := "0.1.0",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Build.dependencies,
    onLoadMessage := Build.welcomeMessage,
    resolvers += "jitpack".at("https://jitpack.io"),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    onChangedBuildSource := ReloadOnSourceChanges
  )
  .enablePlugins(
    ScalafmtPlugin
  )
  .settings(
    assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
    crossPaths := false,
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _                        => MergeStrategy.first
    },
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    artifact in (Compile, assembly) := {
      val previous: Artifact = (artifact in (Compile, assembly)).value
      previous.withClassifier(Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly)
  )
  .settings(Build.publishSettings)
