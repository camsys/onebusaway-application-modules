/**
 * Copyright (C) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs_realtime.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.protobuf.Message;

public class TripUpdatesServlet extends AbstractGtfsRealtimeServlet {
  private static final Logger _log = LoggerFactory.getLogger(TripUpdatesServlet.class);
  private static final long serialVersionUID = 1L;
  
  @Autowired
  public void setProvider(GtfsRealtimeProvider provider) {
    _provider = provider;
  }

  @Override
  protected Message getMessage() {
    if (_provider == null) {
      ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
      GtfsRealtimeProvider provider = (GtfsRealtimeProvider) context.getBean("gtfsProvider");
      _provider = provider;
    }
    
    int count = -1;
    if (_provider != null && _provider.getTripUpdates() != null) {
      count = _provider.getTripUpdates().getEntityCount();
    }
    _log.error("getMessage() called with :" + count + " updates ready");
    return _provider.getTripUpdates();
  }
}
