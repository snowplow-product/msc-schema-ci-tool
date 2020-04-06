import sbt._
import sbt.librarymanagement.ModuleID

object Build {
  object Versions {
    val zio        = "1.0.0-RC18-2"
    val cats       = "2.0.0"
    val catsEffect = "2.0.0"
    val sttp       = "2.0.7"
    val circe      = "0.13.0"
    val decline    = "1.0.0"
  }

  val dependencies: Seq[ModuleID] = Seq(
    "dev.zio"                      %% "zio"                           % Versions.zio,
    "dev.zio"                      %% "zio-interop-cats"              % (Versions.catsEffect + ".0-RC12"),
    "dev.zio"                      %% "zio-logging-slf4j"             % "0.2.6",
    "io.circe"                     %% "circe-core"                    % Versions.circe,
    "io.circe"                     %% "circe-literal"                 % Versions.circe,
    "io.circe"                     %% "circe-generic"                 % Versions.circe,
    "io.circe"                     %% "circe-generic-extras"          % Versions.circe,
    "io.circe"                     %% "circe-parser"                  % Versions.circe,
    "io.circe"                     %% "circe-json-schema"             % "0.1.0",
    "org.typelevel"                %% "cats-core"                     % Versions.cats,
    "org.typelevel"                %% "cats-effect"                   % Versions.catsEffect,
    "com.github.java-json-tools"   % "json-schema-validator"          % "2.2.12",
    "com.softwaremill.sttp.client" %% "core"                          % Versions.sttp,
    "com.softwaremill.sttp.client" %% "circe"                         % Versions.sttp,
    "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % Versions.sttp,
    "com.monovore"                 %% "decline"                       % Versions.decline,
    "com.monovore"                 %% "decline-effect"                % Versions.decline,
    "com.monovore"                 %% "decline-refined"               % Versions.decline,
    "dev.zio"                      %% "zio-test"                      % Versions.zio % "test",
    "dev.zio"                      %% "zio-test-sbt"                  % Versions.zio % "test",
    compilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
    compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

  val welcomeMessage: String = {
    import scala.Console._

    def item(text: String): String = s"${GREEN}▶ ${CYAN}$text${RESET}"

    s"""|Useful sbt tasks:
        |${item("check")}         - Check source files formatting using scalafmt
        |${item("fmt")}           - Formats source files using scalafmt
        |${item("clean")}         - Clean target directory
        |${item("test")}          - Run tests
        |${item("packageJar")}    - Package the app as a fat JAR
      """.stripMargin
  }
}
