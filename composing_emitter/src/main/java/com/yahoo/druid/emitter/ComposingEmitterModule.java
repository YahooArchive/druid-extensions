package com.yahoo.druid.emitter;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.Module;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.metamx.common.logger.Logger;
import com.metamx.emitter.core.Emitter;

import io.druid.guice.JsonConfigProvider;
import io.druid.guice.ManageLifecycle;
import io.druid.initialization.DruidModule;

public class ComposingEmitterModule implements DruidModule
{
  private static Logger log = new Logger(ComposingEmitterModule.class);

  @Override
  public void configure(Binder binder)
  {
     JsonConfigProvider.bind(binder, "druid.emitter.composing", ComposingEmitterConfig.class);
  }

  @Override
  public List<? extends Module> getJacksonModules()
  {
    return Collections.EMPTY_LIST;
  }

  @Provides
  @ManageLifecycle
  @Named("composing")
  public Emitter getEmitter(ComposingEmitterConfig config, final Injector injector)
  {
    log.info("Creating Composing Emitter with %s", config.getEmitters());

    List<Emitter> emitters = Lists.transform(config.getEmitters(),
        new Function<String, Emitter>()
        {
          @Override
          public Emitter apply(String s)
          {
            return injector.getInstance(Key.get(Emitter.class, Names.named(s)));
          }
        });

    return new ComposingEmitter(emitters);
  }
}
