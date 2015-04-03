composing_emitter
=============

This extension adds a "composing" emitter to Druid's set of emitters.  This allows you to have multiple emitters
configured at the same time.  This can be useful if you want to have multiple potential landing points for your
emitter messages.

Use it by adding it as a Druid extension and then setting the following parameters

```
druid.emitter=composing
druid.emitter.composing.emitters=["http", "logging"]
```

The value on `druid.emitter.composing.emitters` is just an example.  In this example it will load up the `http` and
 the `logging` emitter and send all `Event`s to both.  The configuration for each of these emitters is done the
 exact same as if they were individually set as `druid.emitter=http` or `logging` respectively.


Thanks
=============

This module was initially created by @b-slim

Thanks Slim!