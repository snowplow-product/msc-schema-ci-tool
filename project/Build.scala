import sbt._
import sbt.librarymanagement.ModuleID
import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.AssemblyPlugin.defaultShellScript
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import scalafix.sbt.ScalafixPlugin.autoImport.scalafixSemanticdb

object Build {

  object Versions {
    val zio        = "1.0.13"
    val cats       = "2.7.0"
    val catsEffect = "2.5.1"
    val sttp       = "3.3.18"
    val netty      = "4.1.72.Final"
    val circe      = "0.14.1"
    val decline    = "1.4.0"
    val igluClient = "1.1.1"
  }

  val dependencies: Seq[ModuleID] = Seq(
    "dev.zio"                       %% "zio"                           % Versions.zio,
    "dev.zio"                       %% "zio-interop-cats"              % (Versions.catsEffect + ".0"),
    "io.circe"                      %% "circe-core"                    % Versions.circe,
    "io.circe"                      %% "circe-generic"                 % Versions.circe,
    "io.circe"                      %% "circe-generic-extras"          % Versions.circe,
    "io.circe"                      %% "circe-parser"                  % Versions.circe,
    "org.typelevel"                 %% "cats-core"                     % Versions.cats,
    "org.typelevel"                 %% "cats-effect"                   % Versions.catsEffect,
    "com.softwaremill.sttp.client3" %% "core"                          % Versions.sttp,
    "com.softwaremill.sttp.client3" %% "circe"                         % Versions.sttp,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % Versions.sttp,
    // Override netty with latest available to get https://github.com/netty/netty/pull/11805 fix
    "io.netty"                       % "netty-codec-http"              % Versions.netty,
    "io.netty"                       % "netty-codec-socks"             % Versions.netty,
    "io.netty"                       % "netty-handler"                 % Versions.netty,
    "io.netty"                       % "netty-handler-proxy"           % Versions.netty,
    "com.monovore"                  %% "decline"                       % Versions.decline,
    "com.monovore"                  %% "decline-effect"                % Versions.decline,
    "com.monovore"                  %% "decline-refined"               % Versions.decline,
    "com.snowplowanalytics"         %% "iglu-scala-client"             % Versions.igluClient,
    "dev.zio"                       %% "zio-test"                      % Versions.zio   % Test,
    "dev.zio"                       %% "zio-test-sbt"                  % Versions.zio   % Test,
    "io.circe"                      %% "circe-literal"                 % Versions.circe % Test,
    "org.slf4j"                      % "slf4j-nop"                     % "1.7.30",
    compilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    compilerPlugin(scalafixSemanticdb)
  )

  val scalafixDependencies: Seq[ModuleID] = Seq(
    "com.github.liancheng" %% "organize-imports" % "0.6.0"
  )

  val welcomeMessage: String = {
    import scala.Console._

    def item(text: String): String = s"${GREEN}▶ ${CYAN}$text${RESET}"

    s"""Useful sbt tasks:
       |${item("fmtCheck")}      - Check source files formatting using scalafmt
       |${item("fmt")}           - Formats source files using scalafmt
       |${item("clean")}         - Clean target directory
       |${item("test")}          - Run tests
       |${item("assembly")}      - Package the app as a fat JAR
      """.stripMargin
  }

  lazy val sbtAssemblySettings: Seq[Setting[_]] = Seq(
    assembly / assemblyJarName := name.value,
    assembly / mainClass := Some("com.snowplowanalytics.datastructures.ci.Main"),
    assembly / assemblyOption ~= { _.withPrependShellScript(defaultShellScript) },
    crossPaths := false,
    assembly / test := {},
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _                        => MergeStrategy.first
    }
  )

  lazy val sbtBuildInfoSettings: Seq[Setting[_]] = Seq(
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      description
    ),
    buildInfoPackage := "com.snowplowanalytics.datastructures.ci"
  )

}
