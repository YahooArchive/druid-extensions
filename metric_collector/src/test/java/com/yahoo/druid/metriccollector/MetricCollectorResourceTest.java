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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.metamx.emitter.core.NoopEmitter;
import com.metamx.emitter.service.ServiceEmitter;
import io.druid.guice.GuiceInjectors;
import io.druid.guice.JsonConfigProvider;
import io.druid.initialization.Initialization;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MetricCollectorResourceTest
{
  private static final String METRIC_FEED_KEY = "feed";
  private static final String _CLUSTER = "_cluster";

  private final static String TEST_CLUSTER = "test-cluster";
  private final static String KAFKA_TOPIC_PREFIX = "druid";

  private Producer<String, String> producer;
  private MetricCollectorResource resource;

  private final ObjectMapper jsonMapper = new ObjectMapper();

  @Before
  public void setUp()
  {
    MetricCollectorConfig config = EasyMock.createMock(MetricCollectorConfig.class);
    EasyMock.expect(config.getKafkaTopicPrefix()).andReturn(KAFKA_TOPIC_PREFIX);
    EasyMock.expect(config.getLogFrequency()).andReturn(1000L);
    EasyMock.replay(config);
    ServiceEmitter serviceEmitter = new ServiceEmitter("MetricCollector", "localhost", new NoopEmitter());

    this.producer = EasyMock.createMock(Producer.class);
    this.resource = new MetricCollectorResource(jsonMapper, config, producer, serviceEmitter);
  }

  @Test
  public void testDoPostNoEvents() throws IOException
  {
    Response resp = resource.doPost(Lists.<Map<String, Object>>newArrayList(), TEST_CLUSTER);
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
  }

  @Test
  public void testDoPostNoCluster() throws IOException
  {
    List<Map<String, Object>> in = buildTestEvent("metrics");
    Response resp = resource.doPost(in, "");
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
  }

  @Test
  public void testDoPostInvalidFeed() throws IOException
  {
    List<Map<String, Object>> in = buildTestEvent("xxx");
    Response resp = resource.doPost(in, TEST_CLUSTER);

    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
  }

  @Test
  public void testDoPost() throws IOException
  {
    Capture<ProducerRecord<String, String>> capturedMsg = new Capture<ProducerRecord<String, String>>();
    EasyMock.expect(
        producer.send(
            EasyMock.<ProducerRecord<String, String>>capture(capturedMsg),
            EasyMock.anyObject(Callback.class)
        )
    ).andReturn(null);
    EasyMock.replay(producer);

    List<Map<String, Object>> in = buildTestEvent("metrics");
    Response resp = resource.doPost(in, TEST_CLUSTER);

    Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

    ProducerRecord<String, String> msg = capturedMsg.getValue();

    List<Map<String, Object>> actualEvents = Lists.newArrayList();
    Map<String, Object> event = jsonMapper.readValue(
        msg.value(), new TypeReference<Map<String, Object>>()
        {
        }
    );
    Assert.assertEquals(KAFKA_TOPIC_PREFIX + "-" + event.get(METRIC_FEED_KEY), msg.topic());
    Assert.assertEquals(TEST_CLUSTER, event.get(_CLUSTER));
    actualEvents.add(event);
    Assert.assertEquals(in, actualEvents);
  }

  private List<Map<String, Object>> buildTestEvent(String feedName)
  {
    List<Map<String, Object>> in = Lists.newArrayList();
    Map<String, Object> metricEvent = Maps.newHashMap();
    metricEvent.put(METRIC_FEED_KEY, feedName);
    in.add(metricEvent);
    return in;
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExceptionBrokerList() throws Throwable
  {
    Injector injector = GuiceInjectors.makeStartupInjectorWithModules(
        ImmutableList.<Module>of()
    );

    injector.getInstance(Properties.class).put("druid.metricCollector.kafkaProducerConfig", "{}");
    injector.getInstance(Properties.class).put("druid.metricCollector.kafkaTopicPrefix", "testPrefix");
    try {
      injector = Initialization.makeInjectorWithModules(
          injector,
          ImmutableList.of(
              new Module()
              {
                @Override
                public void configure(Binder binder)
                {
                  binder.bindConstant().annotatedWith(Names.named("serviceName")).to("test");
                  binder.bindConstant().annotatedWith(Names.named("servicePort")).to(0);
                  binder.bind(Producer.class).toProvider(KafkaProducerProvider.class);
                  JsonConfigProvider.bind(binder, "druid.metricCollector", MetricCollectorConfig.class);
                }
              }
          )
      );
      injector.getInstance(Producer.class);
    }
    catch (Exception e) {
      throw e.getCause();
    }

  }

  @Test
  public void testMetricCollectorConfig() throws Throwable
  {
    Injector injector = GuiceInjectors.makeStartupInjectorWithModules(
        ImmutableList.<Module>of()
    );

    injector.getInstance(Properties.class)
            .put(
                "druid.metricCollector.kafkaProducerConfig",
                "{\"" + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG + "\":\"localhost:8080\"}"
            );
    injector.getInstance(Properties.class).put("druid.metricCollector.kafkaTopicPrefix", "testPrefix");

    injector = Initialization.makeInjectorWithModules(
        injector,
        ImmutableList.of(
            new Module()
            {
              @Override
              public void configure(Binder binder)
              {
                binder.bindConstant().annotatedWith(Names.named("serviceName")).to("test");
                binder.bindConstant().annotatedWith(Names.named("servicePort")).to(0);
                binder.bind(Producer.class).toProvider(KafkaProducerProvider.class);
                JsonConfigProvider.bind(binder, "druid.metricCollector", MetricCollectorConfig.class);
              }
            }
        )
    );

    MetricCollectorConfig metricCollectorConfig = injector.getInstance(MetricCollectorConfig.class);
    Assert.assertEquals("localhost:8080", metricCollectorConfig.getBootstrapServerConfig());
    Assert.assertEquals("testPrefix", metricCollectorConfig.getKafkaTopicPrefix());
  }
}
