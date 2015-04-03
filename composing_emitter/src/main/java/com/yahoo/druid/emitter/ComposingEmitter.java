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
