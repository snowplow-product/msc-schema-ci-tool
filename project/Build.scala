import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.AssemblyPlugin.defaultShellScript
import sbt._
import sbt.librarymanagement.ModuleID
import sbt.Keys._
import sbtbuildinfo.BuildInfoPlugin.autoImport._

object Build {

  object Versions {
    val zio        = "1.0.0-RC18-2"
    val cats       = "2.0.0"
    val catsEffect = "2.0.0"
    val sttp       = "2.0.7"
    val circe      = "0.13.0"
    val decline    = "1.2.0"
    val igluClient = "1.0.0-rc1"
    val jwt        = "4.3.0"
    val jwk        = "1.0.5"
  }

  val dependencies: Seq[ModuleID] = Seq(
    "dev.zio"                      %% "zio"                           % Versions.zio,
    "dev.zio"                      %% "zio-interop-cats"              % (Versions.catsEffect + ".0-RC12"),
    "io.circe"                     %% "circe-core"                    % Versions.circe,
    "io.circe"                     %% "circe-generic"                 % Versions.circe,
    "io.circe"                     %% "circe-generic-extras"          % Versions.circe,
    "io.circe"                     %% "circe-parser"                  % Versions.circe,
    "io.circe"                     %% "circe-literal"                 % Versions.circe,
    "org.typelevel"                %% "cats-core"                     % Versions.cats,
    "org.typelevel"                %% "cats-effect"                   % Versions.catsEffect,
    "com.softwaremill.sttp.client" %% "core"                          % Versions.sttp,
    "com.softwaremill.sttp.client" %% "circe"                         % Versions.sttp,
    "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % Versions.sttp,
    "com.monovore"                 %% "decline"                       % Versions.decline,
    "com.monovore"                 %% "decline-effect"                % Versions.decline,
    "com.monovore"                 %% "decline-refined"               % Versions.decline,
    "com.snowplowanalytics"        %% "iglu-scala-client"             % Versions.igluClient,
    "com.pauldijou"                %% "jwt-circe"                     % Versions.jwt,
    "com.chatwork"                 %% "scala-jwk"                     % Versions.jwk,
    "dev.zio"                      %% "zio-test"                      % Versions.zio % "test",
    "dev.zio"                      %% "zio-test-sbt"                  % Versions.zio % "test",
    "org.slf4j"                     % "slf4j-nop"                     % "1.7.30",
    compilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
    compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

  val welcomeMessage: String = {
    import scala.Console._

    def item(text: String): String = s"${GREEN}▶ ${CYAN}$text${RESET}"

    s"""Useful sbt tasks:
       |${item("checkfmt")}      - Check source files formatting using scalafmt
       |${item("fmt")}           - Formats source files using scalafmt
       |${item("clean")}         - Clean target directory
       |${item("test")}          - Run tests
       |${item("assembly")}      - Package the app as a fat JAR
      """.stripMargin
  }

  lazy val sbtAssemblySettings: Seq[Setting[_]] = Seq(
    assemblyJarName in assembly := { name.value },
    mainClass in assembly := Some("com.snowplowanalytics.schemaci.Main"),
    assemblyOption in assembly ~= { _.copy(prependShellScript = Some(defaultShellScript)) },
    crossPaths := false,
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _                        => MergeStrategy.first
    }
  )

  lazy val sbtBuildInfoSettings: Seq[Setting[_]] = Seq(
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      description,
      "cid" -> sys.env.get("SNOWPLOW_API_CLIENT_ID"),
      "cs"  -> sys.env.get("SNOWPLOW_API_CLIENT_SECRET")
    ),
    buildInfoPackage := "com.snowplowanalytics.schemaci"
  )

}
