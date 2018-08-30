/**
 * Copyright (C) 2020 Cambridge Systematics, Inc.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

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

  private boolean _emptyMeansRevenueService = true;

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
      Map<AgencyAndId, HashSet<String>> revenueStopRouteIndices = ObjectSerializationLibrary.readObject(path);
      _revenueStopRouteIndices = new HashMap<>(revenueStopRouteIndices);
      _log.info("revenue stop route data loaded");

    } else {
      _revenueStopRouteIndices = new HashMap<>();
    }
  }

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
    if(_revenueStopRouteIndices.get(stop) != null &&
        _revenueStopRouteIndices.get(stop).contains(getHash(route, directionId))){
      return true;
    } else if (_revenueStopRouteIndices.isEmpty() && _emptyMeansRevenueService) {
      return true;
    }
    return false;
  }
  
  @Override
  public Boolean stopHasRevenueService(String agencyId, String stopId) {
    if (_stopHasRevenueServiceCache == null){
      return stopHasRevenueServiceUncached(agencyId, stopId);
    }
    String key = getCacheKey(agencyId, stopId);
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
    if(_revenueStopRouteIndices.get(stop) != null){
      return true;
    } else if (_revenueStopRouteIndices.isEmpty() && _emptyMeansRevenueService) {
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

  public void setEmptyMeansRevenueService(boolean emptyMeansRevenueService) {
    _emptyMeansRevenueService = emptyMeansRevenueService;
  }

  @Override
  public void addRevenueService(String agencyId, String stopId, String routeId, String directionId) {
    AgencyAndId stop = AgencyAndIdLibrary.convertFromString(stopId);
    AgencyAndId route = AgencyAndIdLibrary.convertFromString(routeId);
    HashSet<String> routeDirections = _revenueStopRouteIndices.get(stop);
    if (routeDirections == null) {
      routeDirections = new HashSet<>();
      _revenueStopRouteIndices.put(stop, routeDirections);
    }
    routeDirections.add(getHash(route, directionId));
    clearCache(agencyId, stopId, routeId, directionId);
  }

  @Override
  public void removeRevenueService(String agencyId, String stopId, String routeId, String directionId) {
    AgencyAndId stop = AgencyAndIdLibrary.convertFromString(stopId);
    AgencyAndId route = AgencyAndIdLibrary.convertFromString(routeId);
    HashSet<String> routeDirections = _revenueStopRouteIndices.get(stop);
    if (routeDirections == null) {
      _log.info("Unexpected - stop already does not have revenue service here.");
      return;
    }
    routeDirections.remove(getHash(route, directionId));
    if (routeDirections.isEmpty()) {
      _revenueStopRouteIndices.remove(stop);
    }
    clearCache(agencyId, stopId, routeId, directionId);
  }

  private void clearCache(String agencyId, String stopId, String routeId, String directionId) {
    String stopRouteKey = getCacheKey(agencyId, stopId, routeId, directionId);
    _stopHasRevenueServiceOnRouteCache.remove(stopRouteKey);
    String stopCacheKey = getCacheKey(agencyId, stopId);
    _stopHasRevenueServiceCache.remove(stopCacheKey);
  }
}
