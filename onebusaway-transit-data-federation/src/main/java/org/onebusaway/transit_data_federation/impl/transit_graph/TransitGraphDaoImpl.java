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
import java.util.List;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.exceptions.NoSuchStopServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.blocks.BlockStopTimeIndicesFactory;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.AgencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.transit_data_federation.model.transit_graph.TransitGraph;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransitGraphDaoImpl implements TransitGraphDao {

  private FederatedTransitDataBundle _bundle;

  private TransitGraph _graph;

  private NarrativeService _narrativeService;

  private ExtendedCalendarService _calendarService;

  private BlockIndexService _blockIndexService;

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
    return _graph.addStopEntry(stop);
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
    return _graph.deleteTripEntryForId(id);
  }

  @Override
  public boolean deleteStopTime(AgencyAndId tripId, AgencyAndId stopId) {
    return _graph.deleteStopTime(tripId, stopId);
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
}
