/**
 * Copyright (C) 2018 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.AgencyService;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EntityIdServiceImpl implements EntityIdService {
    private static final Logger _log = LoggerFactory.getLogger(EntityIdServiceImpl.class);

    private TransitGraphDao _transitGraphDao;

    private CalendarService _calendarService;

    private AgencyService _agencyService;

    private List<String> _agencyIds;

    @Autowired
    public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
        _transitGraphDao = transitGraphDao;
    }

    @Autowired
    public void setCalendarService(CalendarService calendarService) {
        _calendarService = calendarService;
    }

    @Autowired
    public void setAgencyService(AgencyService agencyService) {
        _agencyService = agencyService;
    }

    public void setAgencyIds(List<String> agencyIds) {
        _agencyIds = agencyIds;
    }

    @Override
    public AgencyAndId getTripId(String tripId) {
        TripEntry trip = getTrip(tripId);
        if (trip != null)
            return trip.getId();

        _log.warn("trip not found with id \"{}\"", tripId);

        return new AgencyAndId(getDefaultAgencyId(), tripId);
    }

    @Override
    public AgencyAndId getStopId(String stopId) {
        for (String agencyId : getAgencyIds()) {
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

        return new AgencyAndId(getDefaultAgencyId(), stopId);
    }

    @Override
    public AgencyAndId getRouteId(String routeId) {
        for (String agencyId : getAgencyIds()) {
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

        return new AgencyAndId(getDefaultAgencyId(), routeId);
    }

    @Override
    public AgencyAndId getShapeId(String shapeId) {
        for (String agencyId : getAgencyIds()) {
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

        return new AgencyAndId(getDefaultAgencyId(), shapeId);
    }

    private TripEntry getTrip(String tripId) {

        for (String agencyId : getAgencyIds()) {
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

    @Override
    public AgencyAndId getServiceId(String serviceId) {
        if (_calendarService != null) {
            for (String agencyId : getAgencyIds()) {
                AgencyAndId id = new AgencyAndId(agencyId, serviceId);
                if (!_calendarService.getServiceDatesForServiceId(id).isEmpty()) {
                    return id;
                }
            }
        }

        _log.warn("serviceId not found with id \"{}\"", serviceId);

        return new AgencyAndId(getDefaultAgencyId(), serviceId);
    }

    @Override
    public String getDefaultAgencyId() {
        return getAgencyIds().get(0);
    }

    private List<String> getAgencyIds() {
        if (_agencyIds != null) {
            return _agencyIds;
        }
        return _agencyService.getAllAgencyIds();
    }

}
