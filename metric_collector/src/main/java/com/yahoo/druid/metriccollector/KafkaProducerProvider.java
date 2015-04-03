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
