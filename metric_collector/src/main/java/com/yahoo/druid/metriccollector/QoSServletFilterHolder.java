/*
 * Copyright (c) 2016 Yahoo Inc.
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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.druid.server.initialization.ServerConfig;
import io.druid.server.initialization.jetty.ServletFilterHolder;
import org.eclipse.jetty.servlets.QoSFilter;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.util.EnumSet;
import java.util.Map;

import static com.yahoo.druid.metriccollector.CliMetricCollector.LISTEN_PATH;

public class QoSServletFilterHolder implements ServletFilterHolder
{

  private final int maxRequests;

  @Inject
  public QoSServletFilterHolder(ServerConfig serverConfig)
  {
    this.maxRequests = serverConfig.getNumThreads() - 2  > 0 ? serverConfig.getNumThreads() - 2 : 1;
  }

  @Override
  public Filter getFilter()
  {
    return new QoSFilter();
  }

  @Override
  public Class<? extends Filter> getFilterClass()
  {
    return QoSFilter.class;
  }

  @Override
  public Map<String, String> getInitParameters()
  {
    return ImmutableMap.of("maxRequests", String.valueOf(maxRequests));
  }

  @Override
  public String getPath()
  {
    return LISTEN_PATH + "*";
  }

  @Override
  public EnumSet<DispatcherType> getDispatcherType()
  {
    return null;
  }


}
