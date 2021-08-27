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
import org.onebusaway.transit_data_federation.services.AgencyService;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

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
        return getId(_transitGraphDao::getTripEntryForId, tripId);
    }

    @Override
    public AgencyAndId getStopId(String stopId) {
        return getId(_transitGraphDao::getStopEntryForId, stopId);
    }

    @Override
    public AgencyAndId getRouteId(String routeId) {
        return getId(_transitGraphDao::getRouteForId, r -> r.getParent().getId(), Objects::nonNull, routeId);
    }

    @Override
    public AgencyAndId getShapeId(String shapeId) {
        return getId(_transitGraphDao::getShape, shapeId);
    }

    @Override
    public AgencyAndId getServiceId(String serviceId) {
        return getId(_calendarService::getServiceDatesForServiceId, null, s -> !s.isEmpty(), serviceId);
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

    private <T> AgencyAndId getId(Function<AgencyAndId, T> getEntity, String bareId) {
        return getId(getEntity, null, Objects::nonNull, bareId);
    }

    private <T> AgencyAndId getId(Function<AgencyAndId, T> getEntity, Function<T, AgencyAndId> getId, Predicate<T> predicate, String bareId) {
        for (String agencyId : getAgencyIds()) {
            AgencyAndId id = new AgencyAndId(agencyId, bareId);
            T entity = getEntity.apply(id);
            if (predicate.test(entity))
                return getId == null ? id : getId.apply(entity);
        }

        if (bareId.indexOf(AgencyAndId.ID_SEPARATOR) >= 0) {
            try {
                AgencyAndId id = AgencyAndId.convertFromString(bareId);
                T entity = getEntity.apply(id);
                if (entity != null)
                    return id;
            } catch (IllegalArgumentException ex) {
                // pass
            }
        }

        _log.warn("entity not found with id \"{}\"", bareId);

        return new AgencyAndId(getDefaultAgencyId(), bareId);
    }
}
