/**
 * Copyright (C) 2023 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data.model;

import org.onebusaway.gtfs.model.AgencyAndId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * represents idealized/canonical data about a route.
 */
public class RouteGroupingBean implements Serializable {
  // routeId
  private AgencyAndId routeId;
  // stopGroupings
  private List<StopGroupingBean> stopGroupings;
  private List<RouteBean> routes = new ArrayList<>();
  private List<StopBean> stops = new ArrayList<>();


  public AgencyAndId getRouteId() {
    return routeId;
  }

  public void setRouteId(AgencyAndId routeId) {
    this.routeId = routeId;
  }

  public List<StopGroupingBean> getStopGroupings() {
    return stopGroupings;
  }

  public void setStopGroupings(List<StopGroupingBean> stopGroupings) {
    this.stopGroupings = stopGroupings;
  }

  public List<RouteBean> getRoutes() {
    return routes;
  }

  public List<StopBean> getStops() {
    return stops;
  }
}
