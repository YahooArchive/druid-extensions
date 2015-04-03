package com.yahoo.druid.metriccollector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.metamx.common.logger.Logger;
import com.yahoo.druid.metriccollector.annotations.MetricCollector;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/druid/metricCollector/v1/")
public class MetricCollectorResource
{
  private static final Logger log = new Logger(MetricCollectorResource.class);

  private final ObjectMapper jsonMapper;
  private final Producer<String,String> producer;
  private final String kafkaTopicPrefix;

  private static final String METRIC_FEED_KEY = "feed";
  private static final String _CLUSTER = "_cluster";
  private static final Set<String> VALID_FEED_NAMES = Sets.newHashSet("metrics", "alerts");

  @Inject
  public MetricCollectorResource(ObjectMapper jsonMapper,
      MetricCollectorConfig config,
      @MetricCollector Producer<String,String> producer)
  {
    this.jsonMapper = jsonMapper;
    this.producer = producer;
    this.kafkaTopicPrefix = config.getKafkaTopicPrefix();
    log.info("Metric collection initialized to emit to kafka topic prefix: %s", kafkaTopicPrefix);
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  public Response doPost(List<Map<String,Object>> events,
      @QueryParam(_CLUSTER) String cluster) throws IOException
  {
    if(Strings.isNullOrEmpty(cluster)) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity(String.format("request must have %s query param", _CLUSTER)).build();
    }

    if(events == null || events.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity("request must have one or more metric events").build();
    }

    List<KeyedMessage<String,String>> msgs = Lists.newArrayList();
    for(Map<String,Object> event: events) {
      Object feedName = event.get(METRIC_FEED_KEY);
      if(feedName == null || !VALID_FEED_NAMES.contains(feedName)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Each metric event must have a valid feed name").build();
      }

      event.put(_CLUSTER, cluster);
      msgs.add(new KeyedMessage<String, String>(kafkaTopicPrefix + "-" + feedName,
            null, jsonMapper.writeValueAsString(event)));
    }

    producer.send(msgs);
    return Response.ok().build();
  }
}

