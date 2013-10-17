/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
/**
 * 
 */
package org.onebusaway.transit_data_federation.bundle.tasks.stop_transfers;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.collections.tuple.Pair;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StopWalkPlanCache {

  private static final Logger _log = LoggerFactory.getLogger(StopWalkPlanCache.class);

  private PathService _pathService;

  private Map<Pair<StopEntry>, Double> _cache = new HashMap<Pair<StopEntry>, Double>();

  private int _cacheHits = 0;

  private int _totalHits = 0;

  public void setPathService(PathService pathService) {
    _pathService = pathService;
  }

  public int getCacheHits() {
    return _cacheHits;
  }

  public int getTotalHits() {
    return _totalHits;
  }

  /**
   * 
   * @param pair
   * @return -1 if no path can be found between the two stops
   */
  public double getWalkPlanDistanceForStopToStop(Pair<StopEntry> pair) {

    _totalHits++;

    if (!_cache.containsKey(pair)) {

      StopEntry from = pair.getFirst();
      StopEntry to = pair.getSecond();

      String fromPlace = from.getStopLat() + "," + from.getStopLon();
      String toPlace = to.getStopLat() + "," + to.getStopLon();
      Date targetTime = new Date();

      TraverseOptions options = new TraverseOptions();
      options.optimizeFor = OptimizeType.WALK;
      options.setModes(new TraverseModeSet(TraverseMode.WALK));
      options.useServiceDays = false;

      try {
        List<GraphPath> paths = _pathService.plan(fromPlace, toPlace,
            targetTime, options, 1);

        Double distance = getPathsAsDistance(paths);
        _cache.put(pair, distance);
      } catch (VertexNotFoundException ex) {
        _log.warn("walk path error between stops=" + pair, ex);
        _cache.put(pair, null);
      }

    } else {
      _cacheHits++;
    }

    Double distance = _cache.get(pair);

    if (distance == null)
      return -1;

    return distance;
  }

  private Double getPathsAsDistance(List<GraphPath> paths) {

    if (paths == null || paths.isEmpty())
      return null;

    GraphPath path = paths.get(0);

    double totalDistance = 0;

    State prev = null;

    for (State state : path.states) {
      if (prev != null) {
        Vertex vFrom = prev.getVertex();
        Vertex vTo = state.getVertex();
        double d = vFrom.distance(vTo);
        totalDistance += d;
      }
      prev = state;
    }

    return totalDistance;
  }
}