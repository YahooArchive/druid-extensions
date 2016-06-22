/*
 * Copyright (c) 2015 Yahoo Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yahoo.druid.metriccollector;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.metamx.common.logger.Logger;
import com.yahoo.druid.metriccollector.annotations.MetricCollector;
import io.airlift.airline.Command;
import io.druid.cli.QueryJettyServerInitializer;
import io.druid.cli.ServerRunnable;
import io.druid.guice.Jerseys;
import io.druid.guice.JsonConfigProvider;
import io.druid.guice.LazySingleton;
import io.druid.guice.LifecycleModule;
import io.druid.guice.ManageLifecycle;
import io.druid.server.initialization.jetty.JettyServerInitializer;
import io.druid.server.initialization.jetty.ServletFilterHolder;
import org.apache.kafka.clients.producer.Producer;
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
  public static final String LISTEN_PATH = "/druid/metricCollector/v1/";

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
            binder.bind(MetricCollectorResource.class).in(ManageLifecycle.class);
            binder.bind(
                new TypeLiteral<Producer<String, String>>()
                {
                }
            )
                  .annotatedWith(MetricCollector.class)
                  .toProvider(KafkaProducerProvider.class).in(Singleton.class);
            Jerseys.addResource(binder, MetricCollectorResource.class);
            LifecycleModule.register(binder, MetricCollectorResource.class);
            LifecycleModule.register(binder, Server.class);

            Multibinder.newSetBinder(binder, ServletFilterHolder.class)
                       .addBinding()
                       .to(QoSServletFilterHolder.class)
                       .in(Singleton.class);
          }
        }
    );
  }
}
