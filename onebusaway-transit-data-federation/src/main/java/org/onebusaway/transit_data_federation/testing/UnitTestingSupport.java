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
package org.onebusaway.transit_data_federation.testing;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.realtime.api.OccupancyStatus;
import org.onebusaway.transit_data_federation.impl.blocks.BlockIndexFactoryServiceImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.AgencyEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticBlockConfigurationEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticBlockConfigurationEntryImpl.Builder;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticBlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticBlockStopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticBlockTripEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticFrequencyEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticRouteCollectionEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticRouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticStopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticStopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StaticTripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.model.bundle.HistoricalRidership;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexFactoryService;
import org.onebusaway.transit_data_federation.services.blocks.StaticBlockTripIndex;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.FrequencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import static java.util.Arrays.asList;

public class UnitTestingSupport {

  private static final DateFormat _format = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm");

  private static final TimeZone _timeZone = TimeZone.getTimeZone("America/Los_Angeles");

  static {
    _format.setTimeZone(_timeZone);
  }

  /****
   * Time and Date Methods
   ****/

  public static TimeZone timeZone() {
    return _timeZone;
  }

  /**
   * @param source format is "yyyy-MM-dd HH:mm"
   * @return
   */
  public static Date date(String source) {
    try {
      return _format.parse(source);
    } catch (ParseException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static long dateAsLong(String source) {
    return UnitTestingSupport.date(source).getTime();
  }

  public static String format(Date dateA) {
    return UnitTestingSupport._format.format(dateA);
  }

  public static Date getTimeAsDay(Date t) {
    return getTimeAsDay(t.getTime());
  }

  public static Date getTimeAsDay(long t) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeZone(timeZone());
    cal.setTimeInMillis(t);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  public static final int hourToSec(double hour) {
    return (int) (hour * 60 * 60);
  }

  public static int time(int hour, int minute, int seconds) {
    return (hour * 60 + minute) * 60 + seconds;
  }

  public static int time(int hour, int minute) {
    return time(hour, minute, 0);
  }

  /****
   * Entity Factory Methods
   ****/

  public static AgencyEntryImpl agency(String id) {
    AgencyEntryImpl agency = new AgencyEntryImpl();
    agency.setId(id);
    return agency;
  }

  public static StaticRouteEntryImpl route(String id) {
    StaticRouteEntryImpl route = new StaticRouteEntryImpl();
    route.setId(aid(id));
    return route;
  }

  public static StaticRouteCollectionEntryImpl routeCollection(String id,
                                                               RouteEntry... routes) {
    StaticRouteCollectionEntryImpl route = new StaticRouteCollectionEntryImpl();
    route.setId(aid(id));
    route.setChildren(asList(routes));
    for (RouteEntry routeEntry : routes) {
      ((StaticRouteEntryImpl) routeEntry).setParent(route);
    }
    return route;
  }

  public static StaticStopEntryImpl stop(String id) {
    return stop(id, 0.0, 0.0);
  }

  public static StaticStopEntryImpl stop(String id, double lat, double lon) {
    return new StaticStopEntryImpl(aid(id), lat, lon);
  }

  public static StaticBlockEntryImpl block(String id) {
    StaticBlockEntryImpl block = new StaticBlockEntryImpl();
    block.setId(aid(id));
    return block;
  }

  public static StaticTripEntryImpl trip(String id) {
    StaticTripEntryImpl trip = new StaticTripEntryImpl();
    trip.setId(aid(id));
    return trip;
  }

  public static StaticTripEntryImpl trip(String id, String serviceId) {
    StaticTripEntryImpl trip = trip(id);
    trip.setServiceId(new LocalizedServiceId(aid(serviceId), timeZone()));
    return trip;
  }

  public static StaticTripEntryImpl trip(String id, String serviceId,
                                         double totalTripDistance) {
    StaticTripEntryImpl trip = trip(id, serviceId);
    trip.setTotalTripDistance(totalTripDistance);
    return trip;
  }

  public static StaticTripEntryImpl trip(String id, double totalTripDistance) {
    StaticTripEntryImpl trip = trip(id);
    trip.setTotalTripDistance(totalTripDistance);
    return trip;
  }

  public static FrequencyEntry frequency(int startTime, int endTime,
      int headwaySecs, int exactTimes) {
    return new StaticFrequencyEntryImpl(startTime, endTime, headwaySecs, exactTimes);
  }

  public static BlockConfigurationEntry linkBlockTrips(StaticBlockEntryImpl block,
                                                       StaticTripEntryImpl... trips) {
    return linkBlockTrips(block, null, trips);
  }

  public static BlockConfigurationEntry linkBlockTrips(StaticBlockEntryImpl block,
                                                       List<FrequencyEntry> frequencies, StaticTripEntryImpl... trips) {

    List<TripEntry> tripEntries = new ArrayList<TripEntry>();
    Set<LocalizedServiceId> serviceIds = new TreeSet<LocalizedServiceId>();
    for (int i = 0; i < trips.length; i++) {
      StaticTripEntryImpl trip = trips[i];
      trip.setBlock(block);
      tripEntries.add(trip);
      if (trip.getServiceId() != null)
        serviceIds.add(trip.getServiceId());
    }
    Builder builder = StaticBlockConfigurationEntryImpl.builder();
    builder.setBlock(block);
    builder.setServiceIds(new ServiceIdActivation(
        new ArrayList<LocalizedServiceId>(serviceIds),
        new ArrayList<LocalizedServiceId>()));
    builder.setTrips(tripEntries);
    builder.setFrequencies(frequencies);
    builder.setTripGapDistances(new double[tripEntries.size()]);

    BlockConfigurationEntry configuration = builder.create();

    List<BlockConfigurationEntry> configurations = block.getConfigurations();
    if (configurations == null) {
      configurations = new ArrayList<BlockConfigurationEntry>();
      block.setConfigurations(configurations);
    }
    configurations.add(configuration);

    return configuration;
  }

  public static BlockConfigurationEntry linkBlockTrips(String blockId,
      StaticTripEntryImpl... trips) {
    return linkBlockTrips(block(blockId), trips);
  }

  public static BlockConfigurationEntry findBlockConfig(BlockEntry blockEntry,
      ServiceIdActivation serviceIds) {
    for (BlockConfigurationEntry blockConfig : blockEntry.getConfigurations()) {
      if (blockConfig.getServiceIds().equals(serviceIds))
        return blockConfig;
    }
    return null;
  }

  public static List<StaticBlockTripIndex> blockTripIndices(StaticBlockEntryImpl... blocks) {
    List<BlockEntry> list = new ArrayList<BlockEntry>();
    for (StaticBlockEntryImpl block : blocks)
      list.add(block);
    BlockIndexFactoryService factory = new BlockIndexFactoryServiceImpl();
    return factory.createTripIndices(list);
  }

  public static StaticStopTimeEntryImpl addStopTime(StaticTripEntryImpl trip,
                                                    StaticStopTimeEntryImpl stopTime) {

    List<StopTimeEntry> stopTimes = trip.getStopTimes();

    if (stopTimes == null) {
      stopTimes = new ArrayList<StopTimeEntry>();
      trip.setStopTimes(stopTimes);
    }

    int sequence = stopTimes.size();

    if (!stopTimes.isEmpty()) {
      StopTimeEntry prev = stopTimes.get(stopTimes.size() - 1);
      stopTime.setAccumulatedSlackTime(prev.getAccumulatedSlackTime()
          + prev.getSlackTime());
    }

    stopTimes.add(stopTime);
    stopTime.setTrip(trip);
    stopTime.setSequence(sequence);

    return stopTime;
  }

  public static StaticStopTimeEntryImpl stopTime() {
    return new StaticStopTimeEntryImpl();
  }

  public static StaticStopTimeEntryImpl stopTime(int id, StaticStopEntryImpl stop,
                                                 StaticTripEntryImpl trip, int arrivalTime, int departureTime,
                                                 double shapeDistTraveled) {
    return stopTime(id, stop, trip, arrivalTime, departureTime,
        shapeDistTraveled, -1);
  }

  public static StaticStopTimeEntryImpl stopTime(int id, StaticStopEntryImpl stop,
                                                 StaticTripEntryImpl trip, int arrivalTime, int departureTime,
                                                 double shapeDistTraveled, int shapeIndex) {

    StaticStopTimeEntryImpl stopTime = new StaticStopTimeEntryImpl();
    stopTime.setId(id);
    stopTime.setStop(stop);

    stopTime.setArrivalTime(arrivalTime);
    stopTime.setDepartureTime(departureTime);
    stopTime.setShapeDistTraveled(shapeDistTraveled);
    stopTime.setShapePointIndex(shapeIndex);

    if (trip != null)
      addStopTime(trip, stopTime);

    return stopTime;
  }

  public static StaticStopTimeEntryImpl stopTime(int id, StaticStopEntryImpl stop,
                                                 StaticTripEntryImpl trip, int arrivalTime, int departureTime,
                                                 double shapeDistTraveled, double loadFactor){

    StaticStopTimeEntryImpl stopTime = stopTime(id, stop, trip, arrivalTime, departureTime, shapeDistTraveled);

    HistoricalRidership.Builder bldr = HistoricalRidership.builder();
//    bldr.setRouteId(trip.getRoute().getId());
    bldr.setTripId(trip.getId());
    bldr.setStopId(stop.getId());
    bldr.setLoadFactor(loadFactor);
    HistoricalRidership hr = bldr.create();
    OccupancyStatus status = OccupancyStatus.toEnum(hr.getLoadFactor());

    stopTime.setHistoricalOccupancy(status);
    return stopTime;

  }
  public static StaticStopTimeEntryImpl stopTime(int id, StaticStopEntryImpl stop,
                                                 StaticTripEntryImpl trip, int arrivalTime, int departureTime,
                                                 double shapeDistTraveled, int shapeIndex, double loadFactor){

    StaticStopTimeEntryImpl stopTime = stopTime(id, stop, trip, arrivalTime, departureTime, shapeDistTraveled, shapeIndex);

    HistoricalRidership.Builder bldr = HistoricalRidership.builder();
//    bldr.setRouteId(trip.getRoute().getId());
    bldr.setTripId(trip.getId());
    bldr.setStopId(stop.getId());
    bldr.setLoadFactor(loadFactor);
    HistoricalRidership hr = bldr.create();
    OccupancyStatus status = OccupancyStatus.toEnum(hr.getLoadFactor());

    stopTime.setHistoricalOccupancy(status);
    return stopTime;

  }

  public static StaticStopTimeEntryImpl stopTime(int id, StaticStopEntryImpl stop,
                                                 StaticTripEntryImpl trip, int time, double shapeDistTraveled) {
    return stopTime(id, stop, trip, time, time, shapeDistTraveled);
  }

  public static BlockConfigurationEntry blockConfiguration(BlockEntry block,
      ServiceIdActivation serviceIds, TripEntry... trips) {
    Builder builder = StaticBlockConfigurationEntryImpl.builder();
    builder.setBlock(block);
    builder.setServiceIds(serviceIds);
    builder.setTrips(asList(trips));
    builder.setTripGapDistances(new double[trips.length]);
    BlockConfigurationEntry blockConfig = builder.create();

    StaticBlockEntryImpl blockImpl = (StaticBlockEntryImpl) block;
    List<BlockConfigurationEntry> configs = block.getConfigurations();
    if (configs == null) {
      configs = new ArrayList<BlockConfigurationEntry>();
      blockImpl.setConfigurations(configs);
    }
    configs.add(blockConfig);

    for (TripEntry trip : trips) {
      if (trip.getBlock() == null)
        ((StaticTripEntryImpl) trip).setBlock((StaticBlockEntryImpl) block);
    }

    return blockConfig;
  }

  public static StaticBlockTripEntryImpl blockTrip(
      BlockConfigurationEntry blockConfig, TripEntry trip) {
    StaticBlockTripEntryImpl blockTrip = new StaticBlockTripEntryImpl();
    blockTrip.setBlockConfiguration(blockConfig);
    blockTrip.setTrip(trip);
    return blockTrip;
  }

  public static StaticBlockStopTimeEntryImpl blockStopTime(StopTimeEntry stopTime,
                                                           int blockSequence, BlockTripEntry trip) {
    return new StaticBlockStopTimeEntryImpl(stopTime, blockSequence, trip, true);
  }

  public static LocalizedServiceId lsid(String id) {
    return new LocalizedServiceId(aid(id), timeZone());
  }

  public static List<LocalizedServiceId> lsids(String... ids) {
    List<LocalizedServiceId> serviceIds = new ArrayList<LocalizedServiceId>();
    for (String id : ids)
      serviceIds.add(lsid(id));
    return serviceIds;
  }

  public static ServiceIdActivation serviceIds(String... ids) {
    return new ServiceIdActivation(lsids(ids), lsids());
  }

  public static ServiceIdActivation serviceIds(
      List<LocalizedServiceId> activeServiceIds,
      List<LocalizedServiceId> inactiveServiceIds) {
    return new ServiceIdActivation(activeServiceIds, inactiveServiceIds);
  }

  public static AgencyAndId aid(String id) {
    return new AgencyAndId("1", id);
  }

  public static ShapePoints shapePointsFromLatLons(String id, double... values) {

    if (values.length % 2 != 0)
      throw new IllegalStateException();

    int n = values.length / 2;

    double[] lats = new double[n];
    double[] lons = new double[n];
    double[] distances = new double[n];

    double distance = 0;

    for (int i = 0; i < n; i++) {
      lats[i] = values[i * 2];
      lons[i] = values[i * 2 + 1];
      if (i > 0) {
        distance += SphericalGeometryLibrary.distance(lats[i - 1], lons[i - 1],
            lats[i], lons[i]);
      }
      distances[i] = distance;
    }

    return shapePoints(id, lats, lons, distances);
  }

  public static ShapePoints shapePoints(String id, double[] lats,
      double[] lons, double[] distTraveled) {
    ShapePoints shapePoints = new ShapePoints();
    shapePoints.setShapeId(aid(id));
    shapePoints.setLats(lats);
    shapePoints.setLons(lons);
    shapePoints.setDistTraveled(distTraveled);
    return shapePoints;
  }

  public static ShapePoint shapePoint(String id, int sequence, double lat,
      double lon) {
    ShapePoint point = new ShapePoint();
    point.setId(sequence);
    point.setSequence(sequence);
    point.setLat(lat);
    point.setLon(lon);
    point.setShapeId(aid(id));
    return point;
  }

  public static void addServiceDates(CalendarServiceData data, String sid,
      ServiceDate... serviceDates) {
    AgencyAndId serviceId = aid(sid);
    LocalizedServiceId lsid = lsid(sid);

    data.putTimeZoneForAgencyId(serviceId.getAgencyId(), timeZone());
    data.putServiceDatesForServiceId(serviceId, asList(serviceDates));

    List<Date> dates = new ArrayList<Date>();

    for (ServiceDate date : serviceDates) {
      dates.add(date.getAsDate(_timeZone));
    }

    data.putDatesForLocalizedServiceId(lsid, dates);
  }

  public static void addDates(CalendarServiceData data, String sid,
      Date... dates) {
    AgencyAndId serviceId = aid(sid);
    LocalizedServiceId lsid = lsid(sid);

    data.putTimeZoneForAgencyId(serviceId.getAgencyId(), timeZone());
    data.putDatesForLocalizedServiceId(lsid, asList(dates));

    Calendar c = Calendar.getInstance();
    c.setTimeZone(timeZone());

    List<ServiceDate> serviceDates = new ArrayList<ServiceDate>();
    for (Date date : dates) {
      c.setTime(date);
      ServiceDate serviceDate = new ServiceDate(c);
      serviceDates.add(serviceDate);
    }

    data.putServiceDatesForServiceId(serviceId, serviceDates);
  }

}
