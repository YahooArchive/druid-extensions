metric_collector
=============

This extension adds a new CLI option to the Druid CLI.  You can now run

```
druid server metricCollector
```

And it will load up a server that can accept events sent from the `http` Emitter and act as a Kafka producer for two topics
 
* `<prefix>-metrics` - This topic will receive all of the metric events
* `<prefix>-alerts` - This topic will receive all of the alert events

There are two pieces of required configuration:

```
druid.metricCollector.kafkaProducerConfig={ "kafkaPropertyKey": "kafkaPropertyValue" }
druid.metricCollector.kafkaTopicPrefix=some_prefix
```

* `druid.metricCollector.kafkaProducerConfig` - This is a String => String JSON Object of properties to pass along to
 the kafka producer
* `druid.metricCollector.kafkaTopicPrefix` - This is a string prefix that will be prepended to the kafka topic names.
 For example, setting this to `staging` will create the topics `staging-metrics` and `staging-alerts`
 
By default, the collector will listen on port 4080, but that can be overridden by the normal `druid.host` or `druid.port` property


Thanks
===========

This module was initially created by @himanshug, Thanks Himanshu!