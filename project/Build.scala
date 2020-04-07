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
    val igluClient = "0.6.2"
  }

  val dependencies: Seq[ModuleID] = Seq(
    "dev.zio"                      %% "zio"                           % Versions.zio,
    "dev.zio"                      %% "zio-interop-cats"              % (Versions.catsEffect + ".0-RC12"),
    "io.circe"                     %% "circe-core"                    % Versions.circe,
    "io.circe"                     %% "circe-generic"                 % Versions.circe,
    "io.circe"                     %% "circe-generic-extras"          % Versions.circe,
    "io.circe"                     %% "circe-parser"                  % Versions.circe,
    "org.typelevel"                %% "cats-core"                     % Versions.cats,
    "org.typelevel"                %% "cats-effect"                   % Versions.catsEffect,
    "com.softwaremill.sttp.client" %% "core"                          % Versions.sttp,
    "com.softwaremill.sttp.client" %% "circe"                         % Versions.sttp,
    "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % Versions.sttp,
    "com.monovore"                 %% "decline"                       % Versions.decline,
    "com.monovore"                 %% "decline-effect"                % Versions.decline,
    "com.monovore"                 %% "decline-refined"               % Versions.decline,
    "com.snowplowanalytics"        %% "iglu-scala-client"             % Versions.igluClient,
    "dev.zio"                      %% "zio-test"                      % Versions.zio % "test",
    "dev.zio"                      %% "zio-test-sbt"                  % Versions.zio % "test",
    compilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
    compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

  val welcomeMessage: String = {
    import scala.Console._

    def item(text: String): String = s"${GREEN}▶ ${CYAN}$text${RESET}"

    s"""|Useful sbt tasks:
        |${item("checkfmt")}      - Check source files formatting using scalafmt
        |${item("fmt")}           - Formats source files using scalafmt
        |${item("clean")}         - Clean target directory
        |${item("test")}          - Run tests
        |${item("assembly")}      - Package the app as a fat JAR
      """.stripMargin
  }
}
