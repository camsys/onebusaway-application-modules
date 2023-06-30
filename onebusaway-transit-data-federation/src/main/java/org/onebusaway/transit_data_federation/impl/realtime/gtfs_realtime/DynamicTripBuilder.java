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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime;


import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntriesFactory;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.DynamicBlockIndexService;
import org.onebusaway.transit_data_federation.services.transit_graph.*;
import org.onebusaway.transit_data_federation.services.transit_graph.dynamic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.primitives.Doubles.toArray;
import static org.onebusaway.geospatial.services.SphericalGeometryLibrary.distance;

/**
 * Inspired by the transit-data-federation-builder, this is the entry point
 * into how dynamic trips are generated.
 */
public class DynamicTripBuilder {

  private static Logger _log = LoggerFactory.getLogger(DynamicTripBuilder.class);

  private Map<String, DynamicRouteEntry> _routeCache = new PassiveExpiringMap<>(60 * 60 * 1000);// 1 hour to support bundle changes
  private StopTimeEntriesFactory _stopTimeEntriesFactory;
  private DynamicBlockIndexService _blockIndexService;
  @Autowired
  public void setStopTimeEntriesFactory(
          StopTimeEntriesFactory stopTimeEntriesFactory) {
    _stopTimeEntriesFactory = stopTimeEntriesFactory;
  }
  public void setBlockIndexService(DynamicBlockIndexService blockIndexService) {
    _blockIndexService = blockIndexService;
  }

  private TransitGraphDao _graph;
  public void setTransitGraphDao(TransitGraphDao dao) {
    _graph = dao;
  }

  public BlockDescriptor createBlockDescriptor(AddedTripInfo addedTripInfo) {
    // from the addedTripInfo generate the trips and stops, and return in the block descriptor
    BlockDescriptor dynamicBd = new BlockDescriptor();
    dynamicBd.setScheduleRelationship(BlockDescriptor.ScheduleRelationship.ADDED);
    AgencyAndId blockId = new AgencyAndId(addedTripInfo.getAgencyId(), addedTripInfo.getTripId());
    // here we look up past blocks, and advance our position along the block
    BlockInstance instance = _blockIndexService.getDynamicBlockInstance(blockId);
    if (instance == null) {
      instance = createBlockInstance(addedTripInfo);
    }

    if (instance == null) {
      _log.error("unable to create descriptor for additional trip {}", addedTripInfo);
      return null;
    }

    dynamicBd.setBlockInstance(instance);
    dynamicBd.setStartTime(addedTripInfo.getTripStartTime());
    dynamicBd.setStartDate(new ServiceDate(new Date(addedTripInfo.getServiceDate())));
    return dynamicBd;
  }

  private BlockInstance createBlockInstance(AddedTripInfo addedTripInfo) {
    BlockConfigurationEntry blockConfiguration = createBlockConfiguration(addedTripInfo);
    if (blockConfiguration == null) return null;
    return new BlockInstance(blockConfiguration,
            addedTripInfo.getServiceDate());
  }

  private BlockConfigurationEntry createBlockConfiguration(AddedTripInfo addedTripInfo) {
    DynamicBlockConfigurationEntryImpl.Builder configBuilder = DynamicBlockConfigurationEntryImpl.builder();
    BlockEntry blockEntry = createBlockEntry(addedTripInfo);
    configBuilder.setBlock(blockEntry);
    configBuilder.setServiceIds(createServiceIdActivation(addedTripInfo));
    configBuilder.setTrips(new ArrayList<>());
    TripEntry trip = createTrip(addedTripInfo, blockEntry);
    if (trip == null) return null;
    configBuilder.getTrips().add(trip);
    DynamicBlockConfigurationEntryImpl config = new DynamicBlockConfigurationEntryImpl(configBuilder);
    blockEntry.getConfigurations().add(config);
    return config;
  }

  private TripEntry createTrip(AddedTripInfo addedTripInfo, BlockEntry block) {
    DynamicTripEntryImpl trip = new DynamicTripEntryImpl();
    trip.setId(new AgencyAndId(addedTripInfo.getAgencyId(), addedTripInfo.getTripId()));
    DynamicRouteEntry route = createRoute(addedTripInfo);
    if (route == null) return null;
    trip.setRoute(route);
    trip.setDirectionId(getGtfsDirectionId(addedTripInfo.getDirectionId()));
    trip.setBlock(block);
    trip.setServiceId(createLocalizedServiceId(addedTripInfo));
    trip.setStopTimes(createStopTimes(addedTripInfo, trip));
    trip.setTotalTripDistance(calculateTripDistance(trip));
    if (trip.getStopTimes() == null || trip.getStopTimes().isEmpty()) {
      _log.error("aborting trip creation {} with no stops", addedTripInfo.getTripId());
      return null;
    }
    return trip;
  }

  private String getGtfsDirectionId(String directionFlag) {
    if ("N".equalsIgnoreCase(directionFlag))
      return "0";
    if ("S".equalsIgnoreCase(directionFlag))
      return "1";
    return directionFlag;
  }

  private List<StopTimeEntry> createStopTimes(AddedTripInfo addedTripInfo, DynamicTripEntryImpl trip) {
    List<StopTimeEntry> stops = new ArrayList<>();
    int sequence = 0;
    for (AddedStopInfo stopInfo : addedTripInfo.getStops()) {
      StopEntry stop = findStop(addedTripInfo.getAgencyId(), stopInfo.getStopId());
      if (stop == null) {
        // some stops are timepoints/internal and not public
        // these may not have GTFS equivalent and should be dropped
        _log.debug("no such stop {}", stopInfo.getStopId());
        continue;
      }
      DynamicStopTimeEntryImpl stopTime = new DynamicStopTimeEntryImpl();
      stopTime.setStop(copyFromStop(stop));
      if (stopInfo.getArrivalTime() > 0) {
        stopTime.setArrivalTime(toSecondsInDay(stopInfo.getArrivalTime(), addedTripInfo.getServiceDate()));
      }
      if (stopInfo.getDepartureTime() > 0) {
        stopTime.setDepartureTime(toSecondsInDay(stopInfo.getDepartureTime(), addedTripInfo.getServiceDate()));
      }
      stopTime.setSequence(sequence);
      stopTime.setTrip(trip);
      if (stopTime.getArrivalTime() < 1 && stopTime.getDepartureTime() < 1) {
        _log.error("invalid stoptime -- no data: " + stopInfo);
        continue;
      }
      sequence++;
      stops.add(stopTime);
    }
    ShapePoints shapePoints = null;
    shapePoints = loadShapePoints(trip);
    _stopTimeEntriesFactory.ensureStopTimesHaveShapeDistanceTraveledSet(stops, shapePoints);
    return stops;
  }

  private ShapePoints loadShapePoints(DynamicTripEntryImpl trip) {
    ShapePoints result = new ShapePoints();
    result.setShapeId(trip.getShapeId());
    List<Double> lats = new ArrayList<>();
    List<Double> lons = new ArrayList<>();
    if (trip.getStopTimes() != null) {
      for (StopTimeEntry stopTime : trip.getStopTimes()) {
        lats.add(stopTime.getStop().getStopLat());
        lons.add(stopTime.getStop().getStopLon());
      }

      result.setLats(toArray(lats));
      result.setLons(toArray(lons));
      return result;
    }
    return null;
  }


  private int toSecondsInDay(long time, long serviceDate) {
    return Math.toIntExact((time - serviceDate) / 1000);
  }

  private DynamicStopEntryImpl copyFromStop(StopEntry staticStop) {
    return new DynamicStopEntryImpl(staticStop.getId(),
            staticStop.getStopLat(), staticStop.getStopLon(),
            staticStop.getParent());

  }

  private StopEntry findStop(String agencyId, String stopId) {
    return _graph.getStopEntryForId(new AgencyAndId(agencyId, stopId), false);
  }

  private double calculateTripDistance(DynamicTripEntryImpl trip) {
    double distance = 0.0;
    if (trip.getStopTimes() == null || trip.getStopTimes().size() < 2)
      return distance;
    CoordinatePoint lastLocation = null;
    for (StopTimeEntry stopTime : trip.getStopTimes()) {
      if (lastLocation == null) {
        lastLocation = stopTime.getStop().getStopLocation();
      } else {
        CoordinatePoint stopLocation = stopTime.getStop().getStopLocation();
        distance += distance(lastLocation, stopLocation);
        lastLocation = stopLocation;
      }
    }
    return distance;
  }


  private DynamicRouteEntry createRoute(AddedTripInfo addedTripInfo) {
    String routeId = addedTripInfo.getRouteId();
    if (!_routeCache.containsKey(routeId)) {
      RouteEntry staticRouteEntry = findRouteEntry(addedTripInfo.getAgencyId(), routeId);
      if (staticRouteEntry == null) {
        _log.error("no such route " + routeId);
        return null;
      }
      synchronized (_routeCache) {
        if (!_routeCache.containsKey(routeId)) {
          _routeCache.put(routeId, copyFromRoute(staticRouteEntry));
        }
      }
    }
    return _routeCache.get(routeId);
  }

  private DynamicRouteEntry copyFromRoute(RouteEntry staticRouteEntry) {
    DynamicRouteEntry route = new DynamicRouteEntry();
    route.setId(staticRouteEntry.getId());
    route.setParent(staticRouteEntry.getParent());
    route.setTrips(new ArrayList<>());
    route.setType(staticRouteEntry.getType());
    return route;
  }

  private RouteEntry findRouteEntry(String agency, String routeId) {
    return _graph.getRouteForId(new AgencyAndId(agency, routeId));
  }

  private BlockEntry createBlockEntry(AddedTripInfo addedTripInfo) {
    DynamicBlockEntry entry = new DynamicBlockEntry();
    entry.setId(new AgencyAndId(addedTripInfo.getAgencyId(), addedTripInfo.getTripId()));
    entry.setConfigurations(new ArrayList<>()); // bidirectional reference
    return entry;
  }

  private ServiceIdActivation createServiceIdActivation(AddedTripInfo addedTripInfo) {
    return new ServiceIdActivation(createLocalizedServiceId(addedTripInfo));
  }

  private LocalizedServiceId createLocalizedServiceId(AddedTripInfo addedTripInfo) {
    // here we default to simply printing out the service date
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String serviceIdDate = sdf.format(addedTripInfo.getServiceDate());
    LocalizedServiceId serviceId = new LocalizedServiceId(
            new AgencyAndId(addedTripInfo.getAgencyId(), "DYN-" + serviceIdDate),
            TimeZone.getDefault()); // todo this could come from GTFS
    return serviceId;
  }

}