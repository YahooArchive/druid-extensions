druid_extensions
=============

This repository holds a number of extensions to Druid that we have created at Yahoo.  


Composing Emitter
==============

This extension adds a "composing" emitter to Druid's set of emitters.  This allows you to have multiple emitters
configured at the same time.

This extension moved to the [druid.io](https://github.com/druid-io/druid/)

Metric Collector
===============

This extension adds a new CLI option that can collect metrics emitted by Druid and push them into kafka.

[Read more in the extension's README](./metric_collector/README.md)

Static UI
===============

This is an extension that allows a Druid node to serve up some static content under the path `druid/v1/ui`.

[Read more in the extension's README](./static_ui/README.md)

