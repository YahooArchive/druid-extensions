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

import java.io.IOException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.metamx.emitter.core.Emitter;
import com.metamx.emitter.core.Event;

public class ComposingEmitterTest
{

  private Emitter childEmitter;
  private ComposingEmitter composingEmitter;

  @Before
  public void setup() {
    this.childEmitter = EasyMock.createMock(Emitter.class);
    this.composingEmitter = new ComposingEmitter(Lists.newArrayList(childEmitter));
  }

  @Test
  public void testStart() {
    childEmitter.start();
    EasyMock.replay(childEmitter);
    composingEmitter.start();
    EasyMock.verify(childEmitter);
  }

  @Test
  public void testEmit() {
    Event e = EasyMock.createMock(Event.class);
    childEmitter.emit(e);
    EasyMock.replay(childEmitter);
    composingEmitter.emit(e);
    EasyMock.verify(childEmitter);
  }

  @Test
  public void testFlush() throws IOException {
    childEmitter.flush();
    EasyMock.replay(childEmitter);
    composingEmitter.flush();
    EasyMock.verify(childEmitter);
  }

  @Test
  public void testClose() throws IOException {
    childEmitter.close();
    EasyMock.replay(childEmitter);
    composingEmitter.close();
    EasyMock.verify(childEmitter);
  }
}
