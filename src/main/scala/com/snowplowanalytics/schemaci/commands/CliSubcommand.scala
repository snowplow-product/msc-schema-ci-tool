package com.snowplowanalytics.schemaci.commands

import cats.effect.ExitCode

import com.snowplowanalytics.schemaci.CliTask

trait CliSubcommand {
  def process: CliTask[ExitCode]
}
