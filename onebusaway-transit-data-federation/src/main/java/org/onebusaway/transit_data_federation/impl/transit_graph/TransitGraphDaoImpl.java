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

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.NoSuchStopServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.blocks.BlockStopTimeIndicesFactory;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.beans.GeospatialBeanService;
import org.onebusaway.transit_data_federation.services.beans.NearbyStopsBeanService;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.AgencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.TripPlannerGraph;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransitGraphDaoImpl implements TransitGraphDao {

  private FederatedTransitDataBundle _bundle;

  private TripPlannerGraph _graph;

  private NarrativeService _narrativeService;

  private ExtendedCalendarService _calendarService;

  private BlockIndexService _blockIndexService;

  private BlockGeospatialService _blockGeospatialService;

  private ShapePointService _shapePointService;

  private NearbyStopsBeanService _nearbyStopsBeanService;

  private GeospatialBeanService _whereGeospatialService;

  @Autowired
  public void setBundle(FederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  public void setTripPlannerGraph(TripPlannerGraph graph) {
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
  public void setBlockGeospatialService(
          BlockGeospatialService blockGeospatialService) {
    _blockGeospatialService = blockGeospatialService;
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
    return _graph.addAgencyEntry(agency);
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
      rc = _blockGeospatialService.addStop(stop);
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
        rc = _blockGeospatialService.removeStop(stopId);
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
  public boolean deleteStopTime(AgencyAndId tripId, AgencyAndId stopId) {
    return _graph.removeStopTime(tripId, stopId);
  }

  @Override
  public boolean addTripEntry(TripEntryImpl trip) {
    boolean rc = _graph.addTripEntry(trip);
    // adding a trip requires updating other parts of the TDS
    if (rc && _narrativeService != null) {
       _narrativeService.addTrip(trip);
    }
    if (rc) {
      rc = updateBlockStopTime(trip);
    }

    if (rc)
      rc = _blockGeospatialService.addShape(trip.getShapeId());

    if (rc) {
      AgencyEntry ae = _graph.getAgencyForId(trip.getId().getAgencyId());
      RouteCollectionEntry id = _graph.getRouteCollectionForId(trip.getRoute().getId());
      if (ae == null) {
        throw new IllegalStateException("Agency not found " + trip.getId().getAgencyId());
      }
      if (ae.getRouteCollections() == null) {
        AgencyEntryImpl aei = (AgencyEntryImpl) ae;
        aei.setRouteCollections(new ArrayList<RouteCollectionEntry>());
      }
      List<RouteCollectionEntry> routeCollectionEntries = new ArrayList<RouteCollectionEntry>(ae.getRouteCollections());
      if (!routeCollectionEntries.contains(id)) {
        routeCollectionEntries.add(id);
        ((AgencyEntryImpl) ae).setRouteCollections(routeCollectionEntries);
      }
    }

    return rc;
  }


  @Override
  public boolean updateTripEntry(TripEntryImpl trip) {
    if (_graph.getTripEntryForId(trip.getId()) != null) {
      removeTripEntry(trip);
    }
    return addTripEntry(trip);
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
      rc = updateBlockStopTime(null);
    }
    if (rc)
      rc = _blockGeospatialService.addShape(null);
    return rc;
  }

  @Override
  public boolean updateStopTime(AgencyAndId tripId, AgencyAndId stopId, int originalArrivalTime, int originalDepartureTime,
                                int newArrivalTime, int newDepartureTime) {
    return _graph.updateStopTime(tripId, stopId, originalArrivalTime, originalDepartureTime, newArrivalTime, newDepartureTime);
  }

  @Override
  public boolean insertStopTime(AgencyAndId tripId, AgencyAndId stopId, int arrivalTime, int departureTime, int shapeDistanceTravelled) {
    return _graph.insertStopTime(tripId, stopId, arrivalTime, departureTime, shapeDistanceTravelled);
  }

  /**
   * todo this is a serious performance penalty
   * todosheldonabrown
   * this needs to be redesigned to support updates not just completely rebuilding
   * @param trip
   * @return
   */
  private boolean updateBlockStopTime(TripEntryImpl trip) {

    // clear existing block indices
    for (BlockEntry block : _graph.getAllBlocks()) {
      for (BlockConfigurationEntry bce : block.getConfigurations()) {
        for (BlockStopTimeEntry bste : bce.getStopTimes()) {
          StopEntryImpl stop = (StopEntryImpl)bste.getStopTime().getStop();
          stop.getStopTimeIndices().clear();
        }
      }
    }

    BlockStopTimeIndicesFactory factory = new BlockStopTimeIndicesFactory();
    factory.setVerbose(true);
    List<BlockStopTimeIndex> indices = factory.createIndices(getAllBlocks());


    for (BlockStopTimeIndex index : indices) {
      StopEntryImpl stop = (StopEntryImpl) index.getStop();
      stop.addStopTimeIndex(index);
    }

    _blockIndexService.updateBlockStopTime(trip);
    return true;
  }

  public void updateCalendarServiceData(CalendarServiceData data) {
    _calendarService.setData(data);
  }

  public boolean addShape(ShapePoints shape) {
    return _shapePointService.addShape(shape);
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

}
