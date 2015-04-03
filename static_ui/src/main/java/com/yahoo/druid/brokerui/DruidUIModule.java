package com.yahoo.druid.brokerui;

import com.fasterxml.jackson.databind.Module;
import com.google.inject.Binder;
import io.druid.guice.Jerseys;
import io.druid.initialization.DruidModule;

import java.util.Collections;
import java.util.List;

public class DruidUIModule implements DruidModule
{
  @Override
  public List<? extends Module> getJacksonModules()
  {
    return Collections.EMPTY_LIST;
  }

  @Override
  public void configure(Binder binder)
  {
    binder.bind(ContentTypeDetector.class).to(TikaContentTypeDetector.class);
    Jerseys.addResource(binder, DruidUIResource.class);
  }
}