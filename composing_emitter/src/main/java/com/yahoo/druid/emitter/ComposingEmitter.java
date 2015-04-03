package com.yahoo.druid.emitter;

import com.metamx.common.lifecycle.LifecycleStart;
import com.metamx.common.lifecycle.LifecycleStop;
import com.metamx.common.logger.Logger;
import com.metamx.emitter.core.Emitter;
import com.metamx.emitter.core.Event;

import java.io.IOException;
import java.util.List;

public class ComposingEmitter implements Emitter
{
  private static Logger log = new Logger(ComposingEmitter.class);

  private final List<Emitter> emitters;

  public ComposingEmitter(List<Emitter> emitters) {
    this.emitters = emitters;
  }

  @Override
  @LifecycleStart
  public void start()
  {
    log.info("Starting Composing Emitter.");
    for(Emitter e : emitters) {
      log.info("Starting emitter %s.", e.getClass().getName());
      e.start();
    }
  }

  @Override
  public void emit(Event event)
  {
    for(Emitter e : emitters) {
      e.emit(event);
    }
  }

  @Override
  public void flush() throws IOException
  {
    log.info("Flushing Composing Emitter.");
    for(Emitter e : emitters) {
      log.info("Flushing emitter %s.", e.getClass().getName());
      e.flush();
    }
  }

  @Override
  @LifecycleStop
  public void close() throws IOException
  {
    log.info("Closing Composing Emitter.");
    for(Emitter e : emitters) {
      log.info("Closing emitter %s.", e.getClass().getName());
      e.close();
    }
  }
}
