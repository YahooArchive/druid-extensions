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
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MetricCollectorResourceTest
{
  private static final String METRIC_FEED_KEY = "feed";
  private static final String _CLUSTER = "_cluster";

  private final static String TEST_CLUSTER = "test-cluster";
  private final static String KAFKA_TOPIC_PREFIX = "druid";

  private Producer<String,String> producer;
  private MetricCollectorResource resource;

  private final ObjectMapper jsonMapper = new ObjectMapper();

  @Before
  public void setUp() {
    MetricCollectorConfig config = EasyMock.createMock(MetricCollectorConfig.class);
    EasyMock.expect(config.getKafkaTopicPrefix()).andReturn(KAFKA_TOPIC_PREFIX);
    EasyMock.replay(config);

    this.producer = EasyMock.createMock(Producer.class);
    this.resource = new MetricCollectorResource(jsonMapper, config, producer);
  }

  @Test
  public void testDoPostNoEvents() throws IOException {
    Response resp = resource.doPost(Lists.<Map<String,Object>>newArrayList(), TEST_CLUSTER);
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
  }

  @Test
  public void testDoPostNoCluster() throws IOException {
    List<Map<String,Object>> in = buildTestEvent("metrics");
    Response resp = resource.doPost(in, "");
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
  }

  @Test
  public void testDoPostInvalidFeed() throws IOException {
    List<Map<String,Object>> in = buildTestEvent("xxx");
    Response resp = resource.doPost(in, TEST_CLUSTER);

    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
  }

  @Test
  public void testDoPost() throws IOException {
    Capture<List<KeyedMessage<String,String>>> capturedMsgs = new Capture<List<KeyedMessage<String,String>>>();
    producer.send(EasyMock.<List<KeyedMessage<String, String>>>capture(capturedMsgs));
    EasyMock.replay(producer);

    List<Map<String,Object>> in = buildTestEvent("metrics");
    Response resp = resource.doPost(in, TEST_CLUSTER);

    Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

    List<KeyedMessage<String,String>> msgs = capturedMsgs.getValue();
    List<Map<String,Object>> actualEvents = Lists.newArrayList();
    for(KeyedMessage<String,String> msg : msgs) {
      Map<String,Object> event = jsonMapper.readValue(msg.message(), new TypeReference<Map<String,Object>>(){});
      Assert.assertEquals(KAFKA_TOPIC_PREFIX + "-" + event.get(METRIC_FEED_KEY), msg.topic());
      Assert.assertEquals(TEST_CLUSTER, event.get(_CLUSTER));
      actualEvents.add(event);
    }
    Assert.assertEquals(in, actualEvents);
  }

  private List<Map<String,Object>> buildTestEvent(String feedName) {
    List<Map<String,Object>> in = Lists.newArrayList();
    Map<String,Object> metricEvent = Maps.newHashMap();
    metricEvent.put(METRIC_FEED_KEY, feedName);
    in.add(metricEvent);
    return in;
  }
}
