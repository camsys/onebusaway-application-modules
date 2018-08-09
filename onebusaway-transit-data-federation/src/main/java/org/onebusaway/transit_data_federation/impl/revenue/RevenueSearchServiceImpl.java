/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl.revenue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.onebusaway.container.cache.Cacheable;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.revenue.RevenueSearchService;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RevenueSearchServiceImpl implements RevenueSearchService {

  private FederatedTransitDataBundle _bundle;

  @Autowired
  public void setBundle(FederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  private Map<AgencyAndId, HashSet<String>> _revenueStopRouteIndices;
  
  private Cache _stopHasRevenueServiceCache;
  
  private Cache _stopHasRevenueServiceOnRouteCache;

  private Logger _log = LoggerFactory.getLogger(RevenueSearchServiceImpl.class);

  @PostConstruct
  @Refreshable(dependsOn = RefreshableResources.REVENUE_STOP_ROUTE_INDEX)
  public void setup() throws Exception {
    loadNonRevenueStopRouteIndices();
  }

  public Cache getStopHasRevenueServiceCache() {
    return _stopHasRevenueServiceCache;
  }

  public void setStopHasRevenueServiceCache(Cache stopHasRevenueServiceCache) {
    _stopHasRevenueServiceCache = stopHasRevenueServiceCache;
  }

  public Cache getStopHasRevenueServiceOnRouteCache() {
    return _stopHasRevenueServiceOnRouteCache;
  }

  public void setStopHasRevenueServiceOnRouteCache(
      Cache stopHasRevenueServiceOnRouteCache) {
    _stopHasRevenueServiceOnRouteCache = stopHasRevenueServiceOnRouteCache;
  }

  private void loadNonRevenueStopRouteIndices() throws IOException,
      ClassNotFoundException {

    File path = _bundle.getRevenueStopRouteIndicesPath();

    if (path.exists()) {
      _log.info("loading revenue stop route indices data");
      _revenueStopRouteIndices = ObjectSerializationLibrary.readObject(path);
      _log.info("revenue stop route data loaded");

    } else {
      _revenueStopRouteIndices = Collections.emptyMap();
    }
  }

  @Cacheable
  @Override
  public Boolean stopHasRevenueServiceOnRoute(String agencyId, String stopId,
      String routeId, String directionId) {
    if (_stopHasRevenueServiceOnRouteCache == null){
      return stopHasRevenueServiceOnRouteUncached(agencyId, stopId, routeId, directionId);
    }
    String key = getCacheKey(agencyId, stopId, routeId, directionId);
    Element element = _stopHasRevenueServiceOnRouteCache.get(key);
    if (element == null) {
      Boolean value = stopHasRevenueServiceOnRouteUncached(agencyId, stopId, routeId, directionId);
      element = new Element(key, value);
      _stopHasRevenueServiceOnRouteCache.put(element);
    }
    return (Boolean) element.getValue();
  }
  
  private Boolean stopHasRevenueServiceOnRouteUncached(String agencyId, String stopId,
      String routeId, String directionId) {
    AgencyAndId stop = AgencyAndIdLibrary.convertFromString(stopId);
    AgencyAndId route = AgencyAndIdLibrary.convertFromString(routeId);
    if((_revenueStopRouteIndices.get(stop) != null && 
        _revenueStopRouteIndices.get(stop).contains(getHash(route, directionId)))
        || _revenueStopRouteIndices.isEmpty()){
      return true;
    }
    return false;
  }
  
  @Override
  public Boolean stopHasRevenueService(String agencyId, String stopId) {
    if (_stopHasRevenueServiceCache == null){
      return stopHasRevenueServiceUncached(agencyId, stopId);
    }
    String key = getCacheKey(agencyId,stopId);
    Element element = _stopHasRevenueServiceCache.get(stopId);
    if (element == null) {   
      Boolean value = stopHasRevenueServiceUncached(agencyId, stopId);
      element = new Element(key, value);
      _stopHasRevenueServiceCache.put(element);
    }
    return (Boolean) element.getValue();
  }
  
  private Boolean stopHasRevenueServiceUncached(String agencyId, String stopId) {
    AgencyAndId stop = AgencyAndIdLibrary.convertFromString(stopId);
    if(_revenueStopRouteIndices.get(stop) != null || _revenueStopRouteIndices.isEmpty()){
      return true;
    }
    return false;
  }
  
  public String getHash(final AgencyAndId routeId, final String directionId){
    return AgencyAndId.convertToString(routeId) + "_" + directionId;
  }
  
  private String getCacheKey(String agencyId, String stopId) {
    return agencyId + "_" + stopId;
  }
  
  private String getCacheKey(String agencyId, String stopId, String routeId, String directionId) {
    return agencyId + "_" + stopId + "_" + routeId + "_" + directionId;
  }
  
}