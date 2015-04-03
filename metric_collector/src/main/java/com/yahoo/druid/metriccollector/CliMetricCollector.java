package com.yahoo.druid.metriccollector;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.metamx.common.logger.Logger;
import com.yahoo.druid.metriccollector.annotations.MetricCollector;
import io.airlift.command.Command;
import io.druid.cli.QueryJettyServerInitializer;
import io.druid.cli.ServerRunnable;
import io.druid.guice.Jerseys;
import io.druid.guice.JsonConfigProvider;
import io.druid.guice.LazySingleton;
import io.druid.guice.LifecycleModule;
import io.druid.server.initialization.jetty.JettyServerInitializer;
import kafka.javaapi.producer.Producer;
import org.eclipse.jetty.server.Server;

import java.util.List;

/**
 */
@Command(
    name = "metricCollector",
    description = "Runs a metrics collector node pushing metrics to kafka"
)
public class CliMetricCollector extends ServerRunnable
{
  private static final Logger log = new Logger(CliMetricCollector.class);

  public CliMetricCollector()
  {
    super(log);
  }

  @Override
  protected List<? extends Module> getModules()
  {
    return ImmutableList.of(
        new Module()
        {
          @Override
          public void configure(Binder binder)
          {
            binder.bindConstant().annotatedWith(Names.named("serviceName")).to("druid/metricCollector");
            binder.bindConstant().annotatedWith(Names.named("servicePort")).to(4080);
            JsonConfigProvider.bind(binder, "druid.metricCollector", MetricCollectorConfig.class);
            binder.bind(JettyServerInitializer.class).to(QueryJettyServerInitializer.class).in(LazySingleton.class);
            binder.bind(new TypeLiteral<Producer<String,String>>(){})
              .annotatedWith(MetricCollector.class)
              .toProvider(KafkaProducerProvider.class).in(LazySingleton.class);

            Jerseys.addResource(binder, MetricCollectorResource.class);
            LifecycleModule.register(binder, MetricCollectorResource.class);
            LifecycleModule.register(binder, Server.class);
          }
        }
    );
  }
}
