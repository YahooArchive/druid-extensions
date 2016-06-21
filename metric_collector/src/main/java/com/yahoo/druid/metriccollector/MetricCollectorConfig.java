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

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class MetricCollectorConfig
{
  private final static long DEFAULT_LOG_FREQUENCY = 60 * 1000; // 1 MINUTE

  @JsonProperty
  @NotNull
  private Map<String, String> kafkaProducerConfig;

  @JsonProperty
  @NotNull
  private String kafkaTopicPrefix;

  @JsonProperty
  @Min(1)
  private long logFrequency = DEFAULT_LOG_FREQUENCY;

  public Map<String, String> getKafkaProducerConfig()
  {
    return kafkaProducerConfig;
  }

  public String getKafkaTopicPrefix()
  {
    return kafkaTopicPrefix;
  }

  public long getLogFrequency()
  {
    return logFrequency;
  }

  public String getBootstrapServerConfig()
  {
    String bootstrapServerConfig = kafkaProducerConfig.get("bootstrap.servers");
    if (bootstrapServerConfig != null) {
      return bootstrapServerConfig;
    }
    throw new IllegalArgumentException("Please provide kafka bootstrap server config");
  }
}
