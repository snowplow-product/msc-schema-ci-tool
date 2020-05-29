package com.snowplowanalytics.datastructures.ci.commands

import cats.effect.ExitCode

import com.snowplowanalytics.datastructures.ci.CliTask

trait CliSubcommand {
  def process: CliTask[ExitCode]
}
