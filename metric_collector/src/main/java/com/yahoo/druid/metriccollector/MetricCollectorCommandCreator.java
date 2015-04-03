package com.yahoo.druid.metriccollector;

import io.airlift.command.Cli.CliBuilder;
import io.druid.cli.CliCommandCreator;

public class MetricCollectorCommandCreator implements CliCommandCreator
{
  @Override
  public void addCommands(CliBuilder builder)
  {
    builder.withGroup("server")
      .withCommand(CliMetricCollector.class);
  }
}
