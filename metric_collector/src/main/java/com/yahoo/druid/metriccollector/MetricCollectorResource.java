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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.metamx.common.lifecycle.LifecycleStart;
import com.metamx.common.lifecycle.LifecycleStop;
import com.metamx.common.logger.Logger;
import com.metamx.emitter.service.ServiceEmitter;
import com.metamx.emitter.service.ServiceMetricEvent;
import com.yahoo.druid.metriccollector.annotations.MetricCollector;
import io.druid.guice.ManageLifecycle;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.yahoo.druid.metriccollector.CliMetricCollector.LISTEN_PATH;

@Path(LISTEN_PATH)
@ManageLifecycle
public class MetricCollectorResource
{
  private static final Logger log = new Logger(MetricCollectorResource.class);

  private final ObjectMapper jsonMapper;
  private final Producer<String, String> producer;
  private final String kafkaTopicPrefix;
  private final long logFrequency;
  private final EventLogger eventLogger;
  private final ServiceEmitter emitter;

  private static final String METRIC_FEED_KEY = "feed";
  private static final String _CLUSTER = "_cluster";
  private static final Set<String> VALID_FEED_NAMES = Sets.newHashSet("metrics", "alerts");

  private final ScheduledExecutorService loggingService = Executors.newSingleThreadScheduledExecutor();

  @Inject
  public MetricCollectorResource(
      ObjectMapper jsonMapper,
      MetricCollectorConfig config,
      @MetricCollector Producer<String, String> producer,
      ServiceEmitter emitter
  )
  {
    this.jsonMapper = jsonMapper;
    this.producer = producer;
    this.kafkaTopicPrefix = config.getKafkaTopicPrefix();
    this.logFrequency = config.getLogFrequency();
    this.eventLogger = new EventLogger();
    this.emitter = emitter;
    log.info("Metric collection initialized to emit to kafka topic prefix: %s", kafkaTopicPrefix);
  }

  @LifecycleStart
  public void start()
  {
    log.info("Starting thread to logAndClear number of events sent");
    loggingService.scheduleAtFixedRate(
        new Runnable()
        {
          final String feedDimName = "feedSource";

          @Override
          public void run()
          {
            try {
              ImmutableMap<String, EventCounter> eventsCounterMetrics = eventLogger.snapshotAndClear();
              if (eventsCounterMetrics.isEmpty()) {
                log.info("Event Counter Metrics is Empty");
              }
              for (String clusterKey : eventsCounterMetrics.keySet()) {
                final EventCounter eventCounter = eventsCounterMetrics.get(clusterKey);
                emitter.emit(
                    ServiceMetricEvent.builder()
                                      .setDimension(feedDimName, clusterKey)
                                      .build(
                                          "metricCollector/lostEvents",
                                          eventCounter.getEventLost().longValue()
                                      )
                );
                if (eventCounter.getEventLost().longValue() > 0) {
                  log.error(
                      "Kafka Producer lost [%d] events from cluster [%s]",
                      eventCounter.getEventLost().longValue(),
                      clusterKey
                  );
                }
                emitter.emit(
                    ServiceMetricEvent.builder()
                                      .setDimension(feedDimName, clusterKey)
                                      .build(
                                          "metricCollector/sentEvents",
                                          eventCounter.getEventSent().longValue()
                                      )
                );
                emitter.emit(
                    ServiceMetricEvent.builder()
                                      .setDimension(feedDimName, clusterKey)
                                      .build(
                                          "metricCollector/eventsSize",
                                          eventCounter.getEventSize().longValue()
                                      )
                );
                log.info(
                    "Kafka Producer successfully sent [%d] events from cluster [%s]",
                    eventCounter.getEventSent().longValue(),
                    clusterKey
                );

                log.info(
                    "Total size of received events for cluster [%s] is [%d] bytes during a window of [%d] ms",
                    clusterKey,
                    eventCounter.getEventSize().longValue(),
                    logFrequency
                );
              }

              /* Information and documentation about kafka metrics's descriptions can be viewed at https://kafka.apache.org/082/ops.html */
              final Map<MetricName, ? extends Metric> kafkaMetrics = producer.metrics();
              for (Map.Entry<MetricName, ? extends Metric> metric : kafkaMetrics.entrySet()) {
                double value = metric.getValue().value();
                if (Double.isFinite(value)) {
                  emitter.emit(
                      ServiceMetricEvent.builder()
                                        .build(
                                            "metricCollector/kafka/" + metric.getKey().name(),
                                            value
                                        )
                  );
                } else {
                  log.debug("Could not emit metric [%s] since its value is NaN", metric.getKey().name());
                }
              }
            }
            catch (Exception e) {
              log.error(e, "Got an exception at the logging thread");
            }
          }
        },
        1000,
        logFrequency,
        TimeUnit.MILLISECONDS
    );
  }

  @LifecycleStop
  public void stop()
  {
    loggingService.shutdownNow();
    producer.close();
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  public Response doPost(
      List<Map<String, Object>> events,
      @QueryParam(_CLUSTER) final String cluster
  ) throws IOException
  {

    if (Strings.isNullOrEmpty(cluster)) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity(String.format("request must have %s query param", _CLUSTER)).build();
    }

    if (events == null || events.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("request must have one or more metric events").build();
    }

    final Callback callback = new Callback()
    {
      @Override
      public void onCompletion(RecordMetadata metadata, Exception exception)
      {
        if (exception == null) {
          eventLogger.countAsSent(cluster);
        } else {
          eventLogger.countAsLost(cluster);
          log.error(
              exception,
              "Error to send kafka record from cluster [%s], error message [%s] ",
              cluster,
              exception.getMessage()
          );
        }
      }
    };

    for (final Map<String, Object> event : events) {
      Object feedName = event.get(METRIC_FEED_KEY);

      if (feedName == null || !VALID_FEED_NAMES.contains(feedName)) {
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity("Each metric event must have a valid feed name").build();
      }

      event.put(_CLUSTER, cluster);
      final String topic = kafkaTopicPrefix + "-" + feedName;

      final String eventString = jsonMapper.writeValueAsString(event);
      eventLogger.incrementEventSize(cluster, eventString.getBytes().length);

      final ProducerRecord<String, String> record = new ProducerRecord(
          topic,
          null,
          eventString
      );

      try {
        producer.send(record, callback);
      }
      catch (Exception exception) {
        /**
         * if the producer is in non-blocking mode @code BufferExhaustedException will be thrown in case
         * the producer rate exceeds the rate at which data can be sent to brokers.
         */
        eventLogger.countAsLost(cluster);
        log.error(exception, "Got Exception");
        // Do not return "Kafka Exceptions" to the clients. Prevent log pollution.
        return Response.serverError().entity("Something went wrong in the POST request, emitter will retry !").build();
      }
    }
    return Response.ok().build();
  }

  private class EventCounter
  {
    private final AtomicLong eventSent;
    private final AtomicLong eventLost;
    private final AtomicLong eventSize;

    public EventCounter()
    {
      this.eventSent = new AtomicLong(0L);
      this.eventLost = new AtomicLong(0L);
      this.eventSize = new AtomicLong(0L);
    }

    public AtomicLong getEventSent()
    {
      return eventSent;
    }

    public AtomicLong getEventLost()
    {
      return eventLost;
    }

    public AtomicLong getEventSize()
    {
      return eventSize;
    }

  }

  private class EventLogger
  {
    private ConcurrentHashMap<String, EventCounter> eventLogger = new ConcurrentHashMap<>();

    public ImmutableMap<String, EventCounter> snapshotAndClear()
    {
      ImmutableMap.Builder<String, EventCounter> builder = ImmutableMap.builder();
      for (Map.Entry<String, EventCounter> eventCounterEntry : eventLogger.entrySet()) {
        builder.put(
            eventCounterEntry.getKey(),
            eventLogger.remove(eventCounterEntry.getKey())
        );
      }
      return builder.build();
    }

    private EventCounter safeGetOrPut(String cluster)
    {
      EventCounter counter = eventLogger.get(cluster);
      if (counter == null) {
        eventLogger.putIfAbsent(cluster, new EventCounter());
        counter = eventLogger.get(cluster);
      }
      return counter;
    }

    public void incrementEventSize(String cluster, long delta)
    {
      safeGetOrPut(cluster).getEventSize().addAndGet(delta);
    }

    public void countAsLost(String cluster)
    {
      safeGetOrPut(cluster).getEventLost().incrementAndGet();
    }

    public void countAsSent(String cluster)
    {
      safeGetOrPut(cluster).getEventSent().incrementAndGet();
    }
  }

}

