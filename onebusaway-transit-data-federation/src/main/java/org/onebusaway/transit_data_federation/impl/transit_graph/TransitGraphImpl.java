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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.onebusaway.collections.adapter.IAdapter;
import org.onebusaway.collections.adapter.ListAdapter;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.serialization.EntryCallback;
import org.onebusaway.transit_data_federation.services.serialization.EntryIdAndCallback;
import org.onebusaway.transit_data_federation.services.transit_graph.AgencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.TripPlannerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.strtree.STRtree;

public class TransitGraphImpl implements Serializable, TripPlannerGraph {

  private static final long serialVersionUID = 2L;

  private static Logger _log = LoggerFactory.getLogger(TransitGraphImpl.class);

  private static final AgencyEntryAdapter _agencyEntryAdapter = new AgencyEntryAdapter();

  private static final TripEntryAdapter _tripEntryAdapter = new TripEntryAdapter();

  private static final BlockEntryAdapter _blockEntryAdapter = new BlockEntryAdapter();

  private static final StopEntryAdapter _stopEntryAdapter = new StopEntryAdapter();

  private static final RouteCollectionEntryAdapter _routeCollectionEntryAdapter = new RouteCollectionEntryAdapter();

  private static final RouteEntryAdapter _routeEntryAdapter = new RouteEntryAdapter();

  private transient static ReadHelper _helper;

  private List<AgencyEntryImpl> _agencies = new ArrayList<AgencyEntryImpl>();

  private List<StopEntryImpl> _stops = new ArrayList<StopEntryImpl>();

  private List<TripEntryImpl> _trips = new ArrayList<TripEntryImpl>();

  private List<BlockEntryImpl> _blocks = new ArrayList<BlockEntryImpl>();

  private List<RouteCollectionEntryImpl> _routeCollections = new ArrayList<RouteCollectionEntryImpl>();

  private List<RouteEntryImpl> _routes = new ArrayList<RouteEntryImpl>();

  private transient STRtree _stopLocationTree = null;

  private transient Map<String, AgencyEntryImpl> _agencyEntriesById = new HashMap<String, AgencyEntryImpl>();

  private transient Map<AgencyAndId, StopEntryImpl> _stopEntriesById = new HashMap<AgencyAndId, StopEntryImpl>();

  private transient Map<AgencyAndId, TripEntryImpl> _tripEntriesById = new HashMap<AgencyAndId, TripEntryImpl>();

  private transient Map<AgencyAndId, BlockEntryImpl> _blockEntriesById = new HashMap<AgencyAndId, BlockEntryImpl>();

  private transient Map<AgencyAndId, RouteCollectionEntryImpl> _routeCollectionEntriesById = new HashMap<AgencyAndId, RouteCollectionEntryImpl>();

  private transient Map<AgencyAndId, RouteEntryImpl> _routeEntriesById = new HashMap<AgencyAndId, RouteEntryImpl>();

  private transient ReadWriteLock _lock = new ReentrantReadWriteLock();

  public TransitGraphImpl() {

  }

  public void empty() {
    _lock.writeLock().lock();
    _agencyEntriesById.clear();
    _stopEntriesById.clear();
    _tripEntriesById.clear();
    _blockEntriesById.clear();
    _routeCollectionEntriesById.clear();
    _routeEntriesById.clear();
    
    _routeCollections.clear();
    _blocks.clear();
    _trips.clear();
    _stops.clear();
    _routes.clear();
    _agencies.clear();

    _stopLocationTree = null;
    _lock.writeLock().unlock();
  }
  
  public void initialize() {
    // serialization invocation may not have this
    if (_lock ==  null) {
      _lock = new ReentrantReadWriteLock();
    }
    _lock.writeLock().lock();
    if (_stopLocationTree == null) {
      System.out.println("initializing trip planner graph...");

      if (_stops.size() == 0) {

        _log.warn("no stops found for graph");

      } else {

        _stopLocationTree = new STRtree(_stops.size());

        for (int i = 0; i < _stops.size(); i++) {
          StopEntry stop = _stops.get(i);
          double x = stop.getStopLon();
          double y = stop.getStopLat();
          Envelope r = new Envelope(x, x, y, y);
          _stopLocationTree.insert(r, stop);
        }

        _stopLocationTree.build();
      }

      System.out.println("  stops=" + _stops.size());
      System.out.println("  trips= " + _trips.size());
    }

    if (_agencyEntriesById == null
        || _agencyEntriesById.size() < _agencies.size()) {
      refreshAgencyMapping();
    }

    if (_tripEntriesById == null || _tripEntriesById.size() < _trips.size()) {
      refreshTripMapping();
    }

    if (_blockEntriesById == null || _blockEntriesById.size() < _blocks.size()) {
      refreshBlockMapping();
    }

    if (_stopEntriesById == null || _stopEntriesById.size() < _stops.size())
      refreshStopMapping();

    if (_routeCollectionEntriesById == null
        || _routeCollectionEntriesById.size() < _routeCollections.size())
      refreshRouteCollectionMapping();

    if (_routeEntriesById == null || _routeEntriesById.size() < _routes.size())
      refreshRouteMapping();

    int i = 0;
    for (StopEntryImpl stop : _stops)
      stop.setIndex(i++);
    _lock.writeLock().unlock();
  }

  public void initializeFromExistinGraph(TransitGraphImpl graph) {
    _lock.writeLock().lock();
    _agencies.addAll(graph._agencies);
    _stops.addAll(graph._stops);
    _routes.addAll(graph._routes);
    _routeCollections.addAll(graph._routeCollections);
    _lock.writeLock().lock();
    _trips.addAll(graph._trips);
    _lock.writeLock().unlock();
    _blocks.addAll(graph._blocks);
    initialize();
    _lock.writeLock().lock();
  }

  public void putAgencyEntry(AgencyEntryImpl agencyEntry) {
    _lock.writeLock().lock();
    _agencies.add(agencyEntry);
    _lock.writeLock().unlock();
  }

  public void putStopEntry(StopEntryImpl stopEntry) {
    _lock.writeLock().lock();
    _stops.add(stopEntry);
    _lock.writeLock().unlock();
  }

  public List<StopEntryImpl> getStops() {
    _lock.readLock().lock();
    try {
      return new ArrayList<StopEntryImpl>(_stops);
    } finally {
      _lock.readLock().unlock();
    }
  }

  public void putTripEntry(TripEntryImpl tripEntry) {
    _lock.writeLock().lock();
    _trips.add(tripEntry);
    _lock.writeLock().unlock();
  }

  public List<TripEntryImpl> getTrips() {
    _lock.readLock().lock();
    try {
      return new ArrayList<TripEntryImpl>(_trips);
    } finally {
      _lock.readLock().unlock();
    }
  }

  public void putBlockEntry(BlockEntryImpl blockEntry) {
    _lock.writeLock().lock();
    _blocks.add(blockEntry);
    _lock.writeLock().unlock();
  }
  
  public List<BlockEntryImpl> getBlocks() {
    _lock.readLock().lock();
    try {
      return new ArrayList<BlockEntryImpl>(_blocks);
    } finally {
      _lock.readLock().unlock();
    }
  }

  public void putRouteEntry(RouteEntryImpl routeEntry) {
    _lock.writeLock().lock();
    _routes.add(routeEntry);
    _lock.writeLock().unlock();
  }

  public List<RouteEntryImpl> getRoutes() {
    _lock.readLock().lock();
    try {
      return new ArrayList<RouteEntryImpl>(_routes);
    } finally {
      _lock.readLock().unlock();
    }
  }

  public void putRouteCollectionEntry(RouteCollectionEntryImpl routeCollection) {
    _lock.writeLock().lock();
    _routeCollections.add(routeCollection);
    _lock.writeLock().unlock();
  }

  public void refreshAgencyMapping() {
    _lock.writeLock().lock();
    _agencyEntriesById = new HashMap<String, AgencyEntryImpl>();
    for (AgencyEntryImpl entry : _agencies)
      _agencyEntriesById.put(entry.getId(), entry);
    _lock.writeLock().unlock();
  }

  public void refreshTripMapping() {
    _lock.writeLock().lock();
    _tripEntriesById = new HashMap<AgencyAndId, TripEntryImpl>();
    for (TripEntryImpl entry : _trips)
      _tripEntriesById.put(entry.getId(), entry);
    _lock.writeLock().unlock();
  }

  public void refreshBlockMapping() {
    _lock.writeLock().lock();
    _blockEntriesById = new HashMap<AgencyAndId, BlockEntryImpl>();
    for (BlockEntryImpl entry : _blocks)
      _blockEntriesById.put(entry.getId(), entry);
    _lock.writeLock().unlock();
  }

  public void refreshStopMapping() {
    _lock.writeLock().lock();
    _stopEntriesById = new HashMap<AgencyAndId, StopEntryImpl>();
    for (StopEntryImpl entry : _stops)
      _stopEntriesById.put(entry.getId(), entry);
    _lock.writeLock().unlock();
  }

  public void refreshRouteMapping() {
    _lock.writeLock().lock();
    _routeEntriesById = new HashMap<AgencyAndId, RouteEntryImpl>();
    for (RouteEntryImpl entry : _routes)
      _routeEntriesById.put(entry.getId(), entry);
    _lock.writeLock().unlock();
  }

  public void refreshRouteCollectionMapping() {
    _lock.writeLock().lock();
    _routeCollectionEntriesById = new HashMap<AgencyAndId, RouteCollectionEntryImpl>();
    for (RouteCollectionEntryImpl entry : _routeCollections)
      _routeCollectionEntriesById.put(entry.getId(), entry);
    _lock.writeLock().unlock();
  }

  /****
   * {@link TripPlannerGraph} Interface
   ****/

  public List<AgencyEntry> getAllAgencies() {
    _lock.readLock().lock();
    try {
      return new ListAdapter<AgencyEntryImpl, AgencyEntry>(new ArrayList<AgencyEntryImpl>(_agencies),
              _agencyEntryAdapter);
    } finally {
      _lock.readLock().unlock();
    }
  }

  public AgencyEntryImpl getAgencyForId(String id) {
    _lock.readLock().lock();
    try {
      return _agencyEntriesById.get(id);
    } finally {
      _lock.readLock().unlock();
    }
  }

  public boolean addAgencyEntry(AgencyEntryImpl agency) {
    _lock.writeLock().lock();
    try {
      _agencyEntriesById.put(agency.getId(), agency);
      _agencies.add(agency);
    } finally {
      _lock.writeLock().unlock();
    }
    return true;
  }

  @Override
  public List<StopEntry> getAllStops() {
    _lock.readLock().lock();
    try {
      return new ListAdapter<StopEntryImpl, StopEntry>(new ArrayList<StopEntryImpl>(_stops), _stopEntryAdapter);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public boolean addStopEntry(StopEntryImpl stop) {
    _lock.writeLock().lock();
    try {
      if (_stopEntriesById.containsKey(stop.getId()))
        return false;
      _stopEntriesById.put(stop.getId(), stop);
      _stops.add(stop);
    } finally {
      _lock.writeLock().unlock();
    }
    return true;
  }

  @Override
  public boolean removeStopEntry(AgencyAndId stopId) {
    _lock.writeLock().lock();
    try {
      if (!_stopEntriesById.containsKey(stopId)) {
        return false;
      }
      StopEntryImpl entry = _stopEntriesById.remove(stopId);
      _stops.remove(entry);
      return true;
    } finally {
      _lock.writeLock().unlock();
    }
  }


  @Override
  public List<TripEntry> getAllTrips() {
    _lock.readLock().lock();
    try {
      return new ListAdapter<TripEntryImpl, TripEntry>(new ArrayList<TripEntryImpl>(_trips), _tripEntryAdapter);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public List<BlockEntry> getAllBlocks() {
    _lock.readLock().lock();
    try {
      return new ListAdapter<BlockEntryImpl, BlockEntry>(new ArrayList<BlockEntryImpl>(_blocks),
              _blockEntryAdapter);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public List<RouteCollectionEntry> getAllRouteCollections() {
    _lock.readLock().lock();
    try {
      return new ListAdapter<RouteCollectionEntryImpl, RouteCollectionEntry>(
              new ArrayList<RouteCollectionEntryImpl>(_routeCollections), _routeCollectionEntryAdapter);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public List<RouteEntry> getAllRoutes() {
    _lock.readLock().lock();
    try {
      return new ListAdapter<RouteEntryImpl, RouteEntry>(new ArrayList<RouteEntryImpl>(_routes),
              _routeEntryAdapter);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public StopEntryImpl getStopEntryForId(AgencyAndId id) {
    _lock.readLock().lock();
    try {
      return _stopEntriesById.get(id);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public TripEntryImpl getTripEntryForId(AgencyAndId id) {
    _lock.readLock().lock();
    try {
      return _tripEntriesById.get(id);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public boolean addTripEntry(TripEntryImpl trip) {
    if (!valid(trip)) return false;

    _lock.writeLock().lock();
    try {
      if (_tripEntriesById.containsKey(trip.getId()))
        return false;

      _tripEntriesById.put(trip.getId(), trip);
      _trips.add(trip);
      // update block
      if (!_blockEntriesById.containsKey(trip.getBlock().getId())) {
        // new block
        _blockEntriesById.put(trip.getBlock().getId(), trip.getBlock());
      }
      boolean foundBlock = false;

      for (int i =0; i<_blocks.size(); i++) {
        BlockEntryImpl bce = _blocks.get(i);
        if (bce.getId().equals(trip.getBlock().getId())) {
          foundBlock = true;

          _blocks.set(i, trip.getBlock());
        }
      }
      if (!foundBlock) {
        _blocks.add(trip.getBlock());
      }

      // update route
      if (trip.getRoute() != null) {
        if (!_routeEntriesById.containsKey(trip.getRoute().getId())) {
          _routeEntriesById.put(trip.getRoute().getId(), (RouteEntryImpl) trip.getRoute());
          _routes.add((RouteEntryImpl) trip.getRoute());
        }
        if (!_routeCollectionEntriesById.containsKey(trip.getRoute().getId())) {
          RouteCollectionEntryImpl rcei = createRouteCollectionForRoute((RouteEntryImpl) trip.getRoute());
          _routeCollectionEntriesById.put(trip.getRoute().getId(), rcei);
          _routeCollections.add(rcei);
          List<RouteCollectionEntry> routeCollections = _agencyEntriesById.get(trip.getRoute().getId().getAgencyId()).getRouteCollections();
          routeCollections.add(rcei);
        }
      }
      // rebuild block indices
      updateBlockIndices(trip);

    } finally {
      _lock.writeLock().unlock();
    }
    return true;
  }

  // maintain some minimum requirements to keep data structures consistent
  private boolean valid(TripEntryImpl trip) {
    if (trip == null) return false;
    if (trip.getRoute() == null) return false;
    if (trip.getBlock() == null) return false;
    if (trip.getBlock().getId() == null || trip.getId() == null) return false;
    // we need at least one stop time for the block configuration to be valid
    if (trip.getStopTimes() == null || trip.getStopTimes().isEmpty()) return false;
    // don't allow orphaned trips that have no calendar
    if (trip.getServiceId() == null) return false;
    return true;
  }

  private RouteCollectionEntryImpl createRouteCollectionForRoute(RouteEntryImpl routeEntry) {
    RouteCollectionEntryImpl routeCollectionEntry = new RouteCollectionEntryImpl();
    routeCollectionEntry.setId(routeEntry.getId());
    ArrayList<RouteEntry> routes = new ArrayList<RouteEntry>();
    routes.add(routeEntry);
    routes.trimToSize();
    routeCollectionEntry.setChildren(routes);
    routeEntry.setParent(routeCollectionEntry);
    return routeCollectionEntry;
  }

  @Override
  public boolean removeTripEntryForId(AgencyAndId id) {
    _lock.writeLock().lock();
    try {
      TripEntryImpl tripEntry = _tripEntriesById.get(id);
      if (tripEntry != null) {
        _trips.remove(tripEntry);
        return _tripEntriesById.remove(id) != null;
      }
      return false;
    } finally {
      _lock.writeLock().unlock();
    }
  }
  public boolean removeStopTime(AgencyAndId tripId, AgencyAndId stopId) {
    _lock.writeLock().lock();
    try {
      TripEntryImpl tripEntry = _tripEntriesById.get(tripId);
      StopTimeEntry found = null;
      if (tripEntry != null && tripEntry.getStopTimes() != null) {
        for (StopTimeEntry ste : tripEntry.getStopTimes()) {
          if (ste.getStop().getId().equals(stopId)) {
            found = ste;
          }
        }
      }
      if (found != null) {
        boolean rc = tripEntry.getStopTimes().remove(found);
        if (tripEntry.getStopTimes().size() == 0) {
          // for consistency we null out empty array
          tripEntry.setStopTimes(null);
        }
        return rc;
      }
      return false;
    } finally {
      _lock.writeLock().unlock();
    }
  }

  public boolean updateStopTime(AgencyAndId tripId, AgencyAndId stopId, int originalArrivalTime, int originalDepartureTime,
                                int newArrivalTime, int newDepartureTime) {
    _lock.writeLock().lock();
    try {
      TripEntryImpl tripEntry = _tripEntriesById.get(tripId);

      if (tripEntry != null) {
        for (StopTimeEntry ste : tripEntry.getStopTimes()) {
          if (ste.getStop().getId().equals(stopId)) {
            if ((originalArrivalTime < 0 || originalArrivalTime == ste.getArrivalTime())
                    && originalDepartureTime < 0 || originalDepartureTime == ste.getDepartureTime()) {
              StopTimeEntryImpl stei = (StopTimeEntryImpl) ste;
              stei.setArrivalTime(newArrivalTime);
              stei.setDepartureTime(newDepartureTime);
              return true;
            }
          }
        }
      }
      return false;
    } finally {
      _lock.writeLock().unlock();
    }
  }

  @Override
  public boolean insertStopTime(AgencyAndId tripId, AgencyAndId stopId, int arrivalTime, int departureTime,
                                int shapeDistanceTravelled) {
    _lock.writeLock().lock();

    try {
      TripEntryImpl tripEntry = _tripEntriesById.get(tripId);
      StopTimeEntry found = null;
      if (tripEntry != null && tripEntry.getStopTimes() != null) {
        for (int i = tripEntry.getStopTimes().size()-1; i>= 0; i--) {
          StopTimeEntry ste = tripEntry.getStopTimes().get(i);
          if (shapeDistanceTravelled >= 0) {
            // we have shape distance, use it to determine insertion position
            if (shapeDistanceTravelled > ste.getShapeDistTraveled()) {
              tripEntry.getStopTimes().add(i+1, createStopTimeEntry(tripId, stopId, arrivalTime, departureTime, shapeDistanceTravelled));
              return updateBlockIndices(tripEntry);
            }
          } else if (arrivalTime >= 0) {
            // try arrivalTime
            if (arrivalTime > ste.getArrivalTime()) {
              tripEntry.getStopTimes().add(i+1, createStopTimeEntry(tripId, stopId, arrivalTime, departureTime, shapeDistanceTravelled));
              return updateBlockIndices(tripEntry);
            }
          } else {
            // use departureTime
            if (departureTime > ste.getDepartureTime()) {
              tripEntry.getStopTimes().add(i+1, createStopTimeEntry(tripId, stopId, arrivalTime, departureTime, shapeDistanceTravelled));
              return updateBlockIndices(tripEntry);
            }
          }
        }
      }
      // if we are here we have empty stopTimes
      if (tripEntry != null) {
        if (tripEntry.getStopTimes() == null){
          tripEntry.setStopTimes(new ArrayList<StopTimeEntry>());
        }
        tripEntry.getStopTimes().add(0, createStopTimeEntry(tripId, stopId, arrivalTime, departureTime, shapeDistanceTravelled));
        return updateBlockIndices(tripEntry);
      }
      return false;
    } finally {
     _lock.writeLock().unlock();
    }
  }

  /**
   * stop times have changed on the trip, ensure the blockstoptimes are updated
   *
   * for each block configuration on trip
   *   re-generate block configuration
   *   for each block trip entry
   *     link trip to block
   */
  private boolean updateBlockIndices(TripEntryImpl tripEntry) {
    List<BlockConfigurationEntry> newBlockConfigs = new ArrayList<BlockConfigurationEntry>();
    if (tripEntry.getBlock() == null || tripEntry.getBlock().getConfigurations() == null) return false;
    for (BlockConfigurationEntry bce : tripEntry.getBlock().getConfigurations()) {
      BlockConfigurationEntryImpl.Builder builder = BlockConfigurationEntryImpl.builder();
      // the builder computes blockTrips
      builder.setBlock(getBlockEntryForId(tripEntry.getBlock().getId()));

      builder.setServiceIds(bce.getServiceIds());
      ArrayList<TripEntry> mergedTrips = new ArrayList<TripEntry>();
      boolean foundTrip = false;
      for (BlockTripEntry bte : bce.getTrips()) {
        if (bte != null) {
          if (bte.getTrip() != null) {
            if (bte.getTrip().getId().equals(tripEntry.getId())) {
              // update our trip
              foundTrip = true;
              mergedTrips.add(tripEntry);
            } else {
              mergedTrips.add(bte.getTrip());
            }
          }
        }
      }
      if (!foundTrip) {
        mergedTrips.add(tripEntry);
      }
      builder.setTrips(mergedTrips);
      builder.setTripGapDistances(new double[mergedTrips.size()]);
      BlockConfigurationEntry blockConfig = builder.create();
      newBlockConfigs.add(blockConfig);
    }
    // now replace existing block configs with our regenerated block configs
    tripEntry.getBlock().setConfigurations(newBlockConfigs);
    _blockEntriesById.put(tripEntry.getBlock().getId(), tripEntry.getBlock());
    for (int i = _blocks.size()-1; i >= 0; i--) {
      BlockEntryImpl entry = _blocks.get(i);
      if (entry.getId().equals(tripEntry.getBlock().getId())) {
        _blocks.set(i, tripEntry.getBlock());
      }
    }

    return true;
  }

  private StopTimeEntry createStopTimeEntry(AgencyAndId tripId, AgencyAndId stopId, int arrivalTime, int departureTime, int shapeDistanceTravelled) {
    StopTimeEntryImpl stei = new StopTimeEntryImpl();
    stei.setTrip(_tripEntriesById.get(tripId));
    if (stei.getTrip() == null) {
      System.out.println("no trip found for " + tripId);
    }
    stei.setStop(_stopEntriesById.get(stopId));
    if (stei.getStop() == null) {
      System.out.println("no stop found for " + stopId);
    }
    stei.setArrivalTime(arrivalTime);
    stei.setDepartureTime(departureTime);
    stei.setShapeDistTraveled(shapeDistanceTravelled);
    return stei;
  }

  @Override
  public BlockEntry getBlockEntryForId(AgencyAndId blockId) {
    _lock.readLock().lock();
    try {
      return _blockEntriesById.get(blockId);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public RouteCollectionEntry getRouteCollectionForId(AgencyAndId id) {
    _lock.readLock().lock();
    try {
      return _routeCollectionEntriesById.get(id);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public RouteEntryImpl getRouteForId(AgencyAndId id) {
    _lock.readLock().lock();
    try {
      return _routeEntriesById.get(id);
    } finally {
      _lock.readLock().unlock();
    }
  }

  @Override
  public List<StopEntry> getStopsByLocation(CoordinateBounds bounds) {
    _lock.readLock().lock();
    try {
      if (_stopLocationTree == null)
        return Collections.emptyList();
      Envelope r = new Envelope(bounds.getMinLon(), bounds.getMaxLon(),
              bounds.getMinLat(), bounds.getMaxLat());
      StopRTreeVisitor go = new StopRTreeVisitor();
      _stopLocationTree.query(r, go);
      return new ArrayList<StopEntry>(go.getStops());
    } finally {
      _lock.readLock().unlock();
    }
  }

  private class StopRTreeVisitor implements ItemVisitor {

    private List<StopEntry> _nearbyStops = new ArrayList<StopEntry>();

    public List<StopEntry> getStops() {
      return _nearbyStops;
    }

    @Override
    public void visitItem(Object obj) {
      _nearbyStops.add((StopEntry) obj);
    }
  }

  /*****************************************************************************
   * Serialization Support
   ****************************************************************************/

  public static void handleStopEntryRead(StopEntryImpl stopEntryImpl) {
    _helper.handleStopEntryRead(stopEntryImpl);
  }

  public static void handleTripEntryRead(TripEntryImpl tripEntryImpl) {
    _helper.handleTripEntryRead(tripEntryImpl);
  }

  public static void addStopEntryCallback(AgencyAndId stopEntry,
      EntryCallback<StopEntryImpl> entry) {
    _helper.addStopEntryCallback(stopEntry, entry);
  }

  public static void addTripEntryCallback(AgencyAndId tripEntry,
      EntryCallback<TripEntryImpl> entry) {
    _helper.addTripEntryCallback(tripEntry, entry);
  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    _helper = new ReadHelper();
    in.defaultReadObject();
    _helper.flush();
    _helper = null;

    initialize();

    // Do a GC run, since the graph-reading process requires a lot of data to be
    // loaded
    System.gc();
    System.gc();
  }

  private static class ReadHelper {

    private Map<AgencyAndId, StopEntryImpl> _stops = new HashMap<AgencyAndId, StopEntryImpl>();

    private Map<AgencyAndId, TripEntryImpl> _trips = new HashMap<AgencyAndId, TripEntryImpl>();

    private List<EntryIdAndCallback<AgencyAndId, StopEntryImpl>> _stopCallbacks = new ArrayList<EntryIdAndCallback<AgencyAndId, StopEntryImpl>>();
    private List<EntryIdAndCallback<AgencyAndId, TripEntryImpl>> _tripCallbacks = new ArrayList<EntryIdAndCallback<AgencyAndId, TripEntryImpl>>();

    public void handleStopEntryRead(StopEntryImpl stopEntryImpl) {
      _stops.put(stopEntryImpl.getId(), stopEntryImpl);
    }

    public void handleTripEntryRead(TripEntryImpl tripEntryImpl) {
      _trips.put(tripEntryImpl.getId(), tripEntryImpl);
    }

    public void addStopEntryCallback(AgencyAndId stopEntryId,
        EntryCallback<StopEntryImpl> callback) {
      _stopCallbacks.add(new EntryIdAndCallback<AgencyAndId, StopEntryImpl>(
          stopEntryId, callback));
    }

    public void addTripEntryCallback(AgencyAndId tripEntryId,
        EntryCallback<TripEntryImpl> callback) {
      _tripCallbacks.add(new EntryIdAndCallback<AgencyAndId, TripEntryImpl>(
          tripEntryId, callback));
    }

    public void flush() {

      for (EntryIdAndCallback<AgencyAndId, StopEntryImpl> ci : _stopCallbacks) {
        StopEntryImpl entry = _stops.get(ci.getId());
        if (entry == null)
          throw new IllegalStateException("no such stop entry: " + ci.getId());
        ci.getCallback().handle(entry);
      }

      for (EntryIdAndCallback<AgencyAndId, TripEntryImpl> ci : _tripCallbacks) {
        TripEntryImpl entry = _trips.get(ci.getId());
        if (entry == null)
          throw new IllegalStateException("no such trip entry: " + ci.getId());
        ci.getCallback().handle(entry);
      }

      _stopCallbacks.clear();
      _tripCallbacks.clear();

      _stopCallbacks = null;
      _tripCallbacks = null;

      _stops.clear();
      _trips.clear();

      _stops = null;
      _trips = null;
    }
  }

  private static class AgencyEntryAdapter implements
      IAdapter<AgencyEntryImpl, AgencyEntry> {

    @Override
    public AgencyEntry adapt(AgencyEntryImpl source) {
      return source;
    }
  }

  private static class TripEntryAdapter implements
      IAdapter<TripEntryImpl, TripEntry> {

    @Override
    public TripEntry adapt(TripEntryImpl source) {
      return source;
    }
  }

  private static class BlockEntryAdapter implements
      IAdapter<BlockEntryImpl, BlockEntry> {

    @Override
    public BlockEntry adapt(BlockEntryImpl source) {
      return source;
    }
  }

  private static class StopEntryAdapter implements
      IAdapter<StopEntryImpl, StopEntry> {

    @Override
    public StopEntry adapt(StopEntryImpl source) {
      return source;
    }
  }

  private static class RouteCollectionEntryAdapter implements
      IAdapter<RouteCollectionEntryImpl, RouteCollectionEntry> {

    @Override
    public RouteCollectionEntry adapt(RouteCollectionEntryImpl source) {
      return source;
    }
  }

  private static class RouteEntryAdapter implements
      IAdapter<RouteEntryImpl, RouteEntry> {

    @Override
    public RouteEntry adapt(RouteEntryImpl source) {
      return source;
    }
  }
}
