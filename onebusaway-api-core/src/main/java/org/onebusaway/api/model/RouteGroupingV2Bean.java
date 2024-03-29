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
package org.onebusaway.api.model;

import org.onebusaway.gtfs.model.AgencyAndId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RouteGroupingV2Bean implements Serializable {

  private AgencyAndId routeId;
  // stopGroupings
  private List<StopGroupingV2Bean> stopGroupings = new ArrayList<>();

  public AgencyAndId getRouteId() {
    return routeId;
  }

  public void setRouteId(AgencyAndId routeId) {
    this.routeId = routeId;
  }

  public List<StopGroupingV2Bean> getStopGroupings() {
    return stopGroupings;
  }

  public void setStopGroupings(List<StopGroupingV2Bean> stopGroupings) {
    this.stopGroupings = stopGroupings;
  }
}
