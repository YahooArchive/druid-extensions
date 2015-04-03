package com.yahoo.druid.metriccollector;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.metamx.common.logger.Logger;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

import java.util.Properties;

public class KafkaProducerProvider implements Provider<Producer<String, String>>
{
  private static final Logger log = new Logger(KafkaProducerProvider.class);
  
  private final MetricCollectorConfig config;

  @Inject
  public KafkaProducerProvider(MetricCollectorConfig config) {
    this.config = config;
  }

  @Override
  public Producer<String, String> get()
  {
    Properties props = new Properties();
    props.put("request.required.acks", "1");
    props.put("producer.type", "async");

    props.putAll(config.getKafkaProducerConfig());

    props.put("serializer.class", "kafka.serializer.StringEncoder");
    log.info("Creating kafka producer with properties: %s", props);

    return new Producer<String, String>(new ProducerConfig(props));
  }

}
