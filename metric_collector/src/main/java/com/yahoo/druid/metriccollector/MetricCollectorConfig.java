package com.yahoo.druid.metriccollector;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class MetricCollectorConfig
{
  @JsonProperty
  @NotNull
  private Map<String,String> kafkaProducerConfig;

  @JsonProperty
  @NotNull
  private String kafkaTopicPrefix;

  public Map<String,String> getKafkaProducerConfig()
  {
    return kafkaProducerConfig;
  }

  public String getKafkaTopicPrefix()
  {
    return kafkaTopicPrefix;
  }
}
