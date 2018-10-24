/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.transit_data_federation.impl.transit_graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.google.common.collect.Sets;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.NoSuchStopServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.StopTimeEntriesProcessor;
import org.onebusaway.transit_data_federation.services.beans.GeospatialBeanService;
import org.onebusaway.transit_data_federation.services.beans.NearbyStopsBeanService;
import org.onebusaway.transit_data_federation.services.beans.RoutesBeanService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.revenue.RevenueSearchService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.AgencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.transit_data_federation.model.transit_graph.TransitGraph;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransitGraphDaoImpl implements TransitGraphDao {

  private Logger _log = LoggerFactory.getLogger(TransitGraphDaoImpl.class);

  private FederatedTransitDataBundle _bundle;

  private TransitGraph _graph;

  private NarrativeService _narrativeService;

  private ExtendedCalendarService _calendarService;

  private BlockIndexService _blockIndexService;

  private ShapePointService _shapePointService;

  private NearbyStopsBeanService _nearbyStopsBeanService;

  private GeospatialBeanService _whereGeospatialService;

  private RoutesBeanService _routesBeanService;

  private RevenueSearchService _revenueSearchService;

  private StopTimeEntriesProcessor _stopTimesFactory;

  @Autowired
  public void setBundle(FederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  public void setTransitGraph(TransitGraph graph) {
    _graph = graph;
  }

  @Autowired
  public void setNarrativeService(NarrativeService narrativeService) {
    _narrativeService = narrativeService;
  }

  @Autowired
  public void setCalendarService(ExtendedCalendarService calendarService) {
    _calendarService = calendarService;
  }

  @Autowired
  public void setBlockIndexService(BlockIndexService blockIndexService) {
    _blockIndexService = blockIndexService;
  }

  @Autowired
  public void setShapePointService(ShapePointService shapePointService) {
    _shapePointService = shapePointService;
  }

  @Autowired
  public void setGeospatialBeanService(GeospatialBeanService geospatialBeanService) {
    _whereGeospatialService = geospatialBeanService;
  }

  @Autowired
  public void setNearbyStopsBeanService(NearbyStopsBeanService nearbyStopsBeanService) {
    _nearbyStopsBeanService = nearbyStopsBeanService;
  }

  @Autowired
  public void setRoutesBeanService(RoutesBeanService routesBeanService) {
    _routesBeanService = routesBeanService;
  }

  @Autowired
  public void setRevenueSearchService(RevenueSearchService revenueSearchService) {
    _revenueSearchService = revenueSearchService;
  }

  @Autowired
  public void setStopTimesFactory(StopTimeEntriesProcessor stopTimesFactory) {
    _stopTimesFactory = stopTimesFactory;
  }

  @PostConstruct
  @Refreshable(dependsOn = RefreshableResources.TRANSIT_GRAPH)
  public void setup() throws IOException, ClassNotFoundException {
    File path = _bundle.getTransitGraphPath();

    if(_graph != null) {
      TransitGraphImpl graph = (TransitGraphImpl)_graph;
      graph.empty();
      _graph = null;
    }
    
    if (path.exists()) {
      TransitGraphImpl graph = ObjectSerializationLibrary.readObject(path);
      graph.initialize();
      _graph = graph;
    } else {
      _graph = new TransitGraphImpl();
    }
  }

  /****
   * {@link TransitGraphDao} Interface
   ****/

  @Override
  public List<AgencyEntry> getAllAgencies() {
    if (_graph == null) return new ArrayList<AgencyEntry>();
    return _graph.getAllAgencies();
  }

  @Override
  public AgencyEntry getAgencyForId(String id) {
    return _graph.getAgencyForId(id);
  }

  public boolean addAgencyEntry(AgencyEntryImpl agency) {
    boolean rc = _graph.addAgencyEntry(agency);
    if (rc) {
      rc =_narrativeService.addAgency(agency);
    }
    return rc;
  }

  @Override
  public List<StopEntry> getAllStops() {
    if (_graph == null) return new ArrayList<StopEntry>();
    return _graph.getAllStops();
  }

  @Override
  public StopEntry getStopEntryForId(AgencyAndId id) {
    return _graph.getStopEntryForId(id);
  }

  @Override
  public StopEntry getStopEntryForId(AgencyAndId id,
      boolean throwExceptionIfNotFound) {
    StopEntry stop = _graph.getStopEntryForId(id);
    if (stop == null && throwExceptionIfNotFound)
      throw new NoSuchStopServiceException(
          AgencyAndIdLibrary.convertToString(id));
    return stop;
  }

  @Override
  public List<StopEntry> getStopsByLocation(CoordinateBounds bounds) {
    return _graph.getStopsByLocation(bounds);
  }

  @Override
  public boolean addStopEntry(StopEntryImpl stop) {
    boolean rc = _graph.addStopEntry(stop);
    if (rc)
      _nearbyStopsBeanService.clearCache();

    if (rc)
      rc = _whereGeospatialService.refresh();
    return rc;
  }

  @Override
  public boolean removeStopEntry(AgencyAndId stopId) {
    if (getStopEntryForId(stopId) != null) {
      boolean rc = _graph.removeStopEntry(stopId);
      if (rc)
         _nearbyStopsBeanService.clearCache();
      if (rc)
        rc = _whereGeospatialService.refresh();
      return rc;
    }
    return false;
  }
  @Override
  public List<BlockEntry> getAllBlocks() {
    if (_graph == null) return new ArrayList<BlockEntry>();
    return _graph.getAllBlocks();
  }

  @Override
  public BlockEntry getBlockEntryForId(AgencyAndId blockId) {
    return _graph.getBlockEntryForId(blockId);
  }

  @Override
  public List<TripEntry> getAllTrips() {
    return _graph.getAllTrips();
  }

  @Override
  public TripEntry getTripEntryForId(AgencyAndId id) {
    return _graph.getTripEntryForId(id);
  }

  @Override
  public List<RouteCollectionEntry> getAllRouteCollections() {
    return _graph.getAllRouteCollections();
  }

  @Override
  public RouteCollectionEntry getRouteCollectionForId(AgencyAndId id) {
    return _graph.getRouteCollectionForId(id);
  }

  @Override
  public List<RouteEntry> getAllRoutes() {
    return _graph.getAllRoutes();
  }

  @Override
  public RouteEntry getRouteForId(AgencyAndId id) {
    return _graph.getRouteForId(id);
  }

  @Override
  public boolean deleteTripEntryForId(AgencyAndId id) {
    return _graph.removeTripEntryForId(id);
  }

  @Override
  public boolean addTripEntry(TripEntryImpl trip) {
    return addTripEntry(trip, null);
  }

  @Override
  public boolean addTripEntry(TripEntryImpl trip, TripNarrative narrative) {

    // Calculate distance along shape
    AgencyAndId shapeId = trip.getShapeId();
    ShapePoints shape = getShape(shapeId);
    List<StopTimeEntryImpl> processedStopTimeEntries;
    try {
      processedStopTimeEntries = _stopTimesFactory.processStopTimeEntries(_graph, trip.getStopTimes(), trip, shape);
    } catch (Exception ex) {
      return false;
    }
    trip.setStopTimes(new ArrayList<>(processedStopTimeEntries));

    boolean rc = _graph.addTripEntry(trip);
    // adding a trip requires updating other parts of the TDS
    if (rc && _narrativeService != null) {
       _narrativeService.addTrip(trip, narrative);
    }

    if (rc) {
      _routesBeanService.refresh();
    }

    if (rc) {
      for (StopTimeEntry stopTimeEntry : trip.getStopTimes()) {
        addRevenueService(trip, stopTimeEntry.getStop().getId());
      }
    }
    return rc;
  }

  @Override
  public boolean updateStopTimesForTrip(TripEntryImpl trip, List<StopTimeEntry> stopTimeEntries, AgencyAndId shapeId) {
    if (_graph.getTripEntryForId(trip.getId()) != null) {
      AgencyAndId originalShapeId = trip.getShapeId();
      TripNarrative narrative = _narrativeService.removeTrip(trip);
      trip.setShapeId(shapeId);

      // recalculate distance along shape
      ShapePoints shape = getShape(shapeId);
      List<StopTimeEntry> oldStopTimeEntries = trip.getStopTimes();
      List<StopTimeEntryImpl> processedStopTimeEntries;
      try {
        processedStopTimeEntries = _stopTimesFactory.processStopTimeEntries(_graph, stopTimeEntries, trip, shape);
      } catch (Exception ex) {
        // reset...
        ex.printStackTrace();
        trip.setShapeId(originalShapeId);
        _narrativeService.addTrip(trip, narrative);
        return false;
      }

      trip.setStopTimes(new ArrayList<>(processedStopTimeEntries));
      _narrativeService.addTrip(trip, narrative);

      if (!processedStopTimeEntries.equals(oldStopTimeEntries)) {
        Set<StopEntry> oldStops = oldStopTimeEntries.stream().map(StopTimeEntry::getStop).collect(Collectors.toSet());
        Set<StopEntry> newStops = processedStopTimeEntries.stream().map(StopTimeEntry::getStop).collect(Collectors.toSet());
        for (StopEntry stop : Sets.difference(oldStops, newStops)) {
          if (!stopHasServiceOnRouteExcludingTrip(stop, trip)) {
            removeRevenueService(trip, stop.getId());
          }
        }
        for (StopEntry stop : Sets.difference(newStops, oldStops)) {
          addRevenueService(trip, stop.getId());
        }
      }

      return _graph.updateBlockIndices(trip);
    }
    return false;
  }

  @Override
  public boolean removeTripEntry(TripEntryImpl trip) {
    if (_graph.getTripEntryForId(trip.getId()) == null) {
      return false;
    }
    boolean rc = _graph.removeTripEntryForId(trip.getId());
    if (rc && _narrativeService != null) {
      _narrativeService.removeTrip(trip);
    }
    if (rc) {
      for (StopTimeEntry stopTimeEntry : trip.getStopTimes()) {
        if (!stopHasServiceOnRouteExcludingTrip(stopTimeEntry.getStop(), trip)) {
          removeRevenueService(trip, stopTimeEntry.getStop().getId());
        }
      }
    }
    return rc;
  }

  public void updateCalendarServiceData(CalendarServiceData data) {
    _calendarService.setData(data);
  }

  public boolean addShape(ShapePoints shape) {
    return _shapePointService.addShape(shape);
  }

  @Override
  public ShapePoints getShape(AgencyAndId shapeId) {
    return _shapePointService.getShapePointsForShapeId(shapeId);
  }

  @Override
  public void removeShape(AgencyAndId shapeId) {
    _shapePointService.removeShape(shapeId);
  }

  public List<AgencyAndId> getAllReferencedShapeIds() {
    Set<AgencyAndId> shapeIds = new HashSet<AgencyAndId>();
    for (TripEntry trip : getAllTrips()) {
      if (trip.getShapeId() != null) {
        shapeIds.add(trip.getShapeId());
      }
    }
    return new ArrayList<AgencyAndId>(shapeIds);
  }

  private boolean stopHasServiceOnRouteExcludingTrip(StopEntry stop, TripEntry trip) {
    return stopHasServiceOnRouteExcludingTrip(stop, trip.getRoute().getId(), trip.getId());
  }

  private boolean stopHasServiceOnRouteExcludingTrip(StopEntry stop, AgencyAndId routeId, AgencyAndId tripId) {
    for (BlockStopTimeIndex index : _blockIndexService.getStopTimeIndicesForStop(stop)) {
      // Can't look at the stop time index directly because it hasn't been rebuilt yet.
      for (BlockConfigurationEntry entry : index.getBlockConfigs()) {
        for (BlockTripEntry trip : entry.getTrips()) {
          if (trip.getTrip().getId().equals(tripId) || trip.getTrip().getRoute().getId().equals(routeId)) {
            continue;
          }
          for (BlockStopTimeEntry bst : trip.getStopTimes()) {
            if (bst.getStopTime().getStop().getId().equals(stop.getId())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private void addRevenueService(TripEntry trip, AgencyAndId stopId) {
    String directionId = trip.getDirectionId() == null ? "0" : trip.getDirectionId();
    _revenueSearchService.addRevenueService(trip.getId().getAgencyId(), stopId.toString(),
            trip.getRoute().getId().toString(), directionId);
  }

  public void removeRevenueService(TripEntry trip, AgencyAndId stopId) {
    String directionId = trip.getDirectionId() == null ? "0" : trip.getDirectionId();
    _revenueSearchService.removeRevenueService(trip.getId().getAgencyId(), stopId.toString(),
            trip.getRoute().getId().toString(), directionId);
  }

  public TransitGraphImpl getGraph() {
    return _graph;
  }
}
