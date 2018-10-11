/**
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.transit_data_federation.impl.service_alerts.ServiceAlertLibrary;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.Id;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsRealtimeEntitySource {

  private static final Logger _log = LoggerFactory.getLogger(GtfsRealtimeEntitySource.class);

  private TransitGraphDao _transitGraphDao;

  private CalendarService _calendarService;

  private List<String> _agencyIds;

  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  public void setCalendarService(CalendarService calendarService) {
    _calendarService = calendarService;
  }

  public void setAgencyIds(List<String> agencyIds) {
    _agencyIds = agencyIds;
  }

  public AgencyAndId getObaRouteId(String routeId) {
    for (String agencyId : _agencyIds) {
      AgencyAndId id = new AgencyAndId(agencyId, routeId);
      RouteEntry route = _transitGraphDao.getRouteForId(id);
      if (route != null)
        return route.getParent().getId();
    }

    try {
      AgencyAndId id = AgencyAndId.convertFromString(routeId);
      RouteEntry route = _transitGraphDao.getRouteForId(id);
      if (route != null)
        return route.getParent().getId();
    } catch (IllegalArgumentException ex) {

    }

    _log.warn("route not found with id \"{}\"", routeId);

    return new AgencyAndId(_agencyIds.get(0), routeId);
  }

  /**
   * Given a route id without an agency prefix, we attempt to find a
   * {@link RouteEntry} with the specified id by cycling through the set of
   * agency ids specified in {@link #setAgencyIds(List)}. If a
   * {@link RouteEntry} is found, we return the id of the parent
   * {@link RouteCollectionEntry} as the matching id. This is to deal with the
   * fact that while GTFS deals with underlying routes, internally OneBusAway
   * mostly deals with RouteCollections.
   * 
   * If no route is found in the {@link TransitGraphDao} with the specified id
   * for any of the configured agencies, a {@link Id} will be constructed with
   * the first agency id from the agency list.
   * 
   * @param routeId
   * @return an Id for {@link RouteCollectionEntry} with a matching
   *         {@link RouteEntry} id
   */
  public Id getRouteId(String routeId) {
    AgencyAndId id = getObaRouteId(routeId);
    return ServiceAlertLibrary.id(id);
  }

  public AgencyAndId getObaTripId(String tripId) {

    TripEntry trip = getTrip(tripId);
    if (trip != null)
      return trip.getId();

    _log.warn("trip not found with id \"{}\"", tripId);

    return new AgencyAndId(_agencyIds.get(0), tripId);
  }

  public Id getTripId(String tripId) {
    return ServiceAlertLibrary.id(getObaTripId(tripId));
  }

  public TripEntry getTrip(String tripId) {

    for (String agencyId : _agencyIds) {
      AgencyAndId id = new AgencyAndId(agencyId, tripId);
      TripEntry trip = _transitGraphDao.getTripEntryForId(id);
      if (trip != null)
        return trip;
    }

    try {
      AgencyAndId id = AgencyAndId.convertFromString(tripId);
      TripEntry trip = _transitGraphDao.getTripEntryForId(id);
      if (trip != null)
        return trip;
    } catch (IllegalArgumentException ex) {

    }
    return null;
  }

  public AgencyAndId getObaStopId(String stopId) {

    for (String agencyId : _agencyIds) {
      AgencyAndId id = new AgencyAndId(agencyId, stopId);
      StopEntry stop = _transitGraphDao.getStopEntryForId(id);
      if (stop != null)
        return id;
    }

    try {
      AgencyAndId id = AgencyAndId.convertFromString(stopId);
      StopEntry stop = _transitGraphDao.getStopEntryForId(id);
      if (stop != null)
        return id;
    } catch (IllegalArgumentException ex) {

    }

    _log.warn("stop not found with id \"{}\"", stopId);

    AgencyAndId id = new AgencyAndId(_agencyIds.get(0), stopId);
    return id;
  }

  public Id getStopId(String stopId) {
    return ServiceAlertLibrary.id(getObaStopId(stopId));
  }

  public AgencyAndId getObaShapeId(String shapeId) {

    for (String agencyId : _agencyIds) {
      AgencyAndId id = new AgencyAndId(agencyId, shapeId);
      ShapePoints shape = _transitGraphDao.getShape(id);
      if (shape != null)
        return id;
    }

    try {
      AgencyAndId id = AgencyAndId.convertFromString(shapeId);
      ShapePoints shape = _transitGraphDao.getShape(id);
      if (shape != null)
        return id;
    } catch (IllegalArgumentException ex) {

    }

    _log.warn("shape not found with id \"{}\"", shapeId);

    AgencyAndId id = new AgencyAndId(_agencyIds.get(0), shapeId);
    return id;
  }

  public AgencyAndId getObaServiceId(String serviceId) {
    if (_calendarService != null) {
      for (String agencyId : _agencyIds) {
        AgencyAndId id = new AgencyAndId(agencyId, serviceId);
        if (!_calendarService.getServiceDatesForServiceId(id).isEmpty()) {
          return id;
        }
      }
    }

    _log.warn("serviceId not found with id \"{}\"", serviceId);

    return new AgencyAndId(_agencyIds.get(0), serviceId);
  }

  public String getDefaultAgencyId() {
    return _agencyIds.get(0);
  }
}
