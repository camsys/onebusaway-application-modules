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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes;

import com.camsys.transit.servicechange.DateDescriptor;
import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.field_descriptors.AbstractFieldDescriptor;
import com.camsys.transit.servicechange.field_descriptors.StopTimesFields;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.GtfsRealtimeEntitySource;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphDaoImpl;
import org.onebusaway.transit_data_federation.services.AgencyService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GtfsSometimesHandlerImpl implements GtfsSometimesHandler {

    private TransitGraphDao _dao;

    private List<String> _agencyIds = new ArrayList<>();

    private AgencyService _agencyService;

    private GtfsRealtimeEntitySource _entitySource;

    // for debugging
    private long _time = -1;

    private static final Logger _log = LoggerFactory.getLogger(GtfsSometimesHandlerImpl.class);

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Autowired
    public void setAgencyService(AgencyService agencyService) {
        _agencyService = agencyService;
    }

    public void setAgencyId(String agencyId) {
        _agencyIds = Collections.singletonList(agencyId);
    }

    public void setAgencyIds(List<String> agencyIds) {
        _agencyIds = agencyIds;
    }

    public void setEntitySource(GtfsRealtimeEntitySource entitySource) {
        _entitySource = entitySource;
    }

    public void setTime(long time) {
        _time = time;
    }


    @PostConstruct
    public void init() {
        // Borrowed from GtfsRealtimeSource. TODO - make this behavior reusbale.
        if (_agencyIds.isEmpty()) {
            _log.info("no agency ids specified for GtfsSometimesHandlerImpl, so defaulting to full agency id set");
            List<String> agencyIds = _agencyService.getAllAgencyIds();
            _agencyIds.addAll(agencyIds);
            if (_agencyIds.size() > 3) {
                _log.warn("The default agency id set is quite large (n="
                        + _agencyIds.size()
                        + ").  You might consider specifying the applicable agencies for your GtfsSometimesHandlerImpl.");
            }
        }
        _entitySource = new GtfsRealtimeEntitySource();
        _entitySource.setAgencyIds(_agencyIds);
        _entitySource.setTransitGraphDao(_dao);
    }

    @Override
    public int handleServiceChanges(Collection<ServiceChange> serviceChanges) {
        int nSuccess = 0;
        for (ServiceChange serviceChange : serviceChanges) {
            if (!validateServiceChange(serviceChange)) {
                _log.debug("service change is invalid");
                continue;
            }
            if (!dateIsApplicable(serviceChange)) {
                _log.debug("Service change is not applicable to date.");
                continue;
            }
            switch (serviceChange.getTable()) {
                case STOP_TIMES:
                    if (handleStopTimesChange(serviceChange)) {
                        nSuccess++;
                    }
                    break;
                case STOPS:
                case ROUTES:
                case TRIPS:
                case SHAPES:
                case TRANSFERS:
                default:
                    _log.error("Table not implemented: {}", serviceChange.getTable());
            }
        }

        forceFlush();
        return nSuccess;
    }

    @Override
    public boolean handleServiceChange(ServiceChange change) {
        return handleServiceChanges(Collections.singleton(change)) == 1;
    }

    private void forceFlush() {
        // TODO - this ultimately should be part of the dao methods, and will be specific to a trip.
        if (_dao instanceof TransitGraphDaoImpl) {
            ((TransitGraphDaoImpl) _dao).updateBlockIndices(null);
            ((TransitGraphDaoImpl) _dao).flushCache();
        }
    }

    private boolean validateServiceChange(ServiceChange change) {
        if (change.getAffectedDates().isEmpty()) {
            _log.info("affected dates is empty");
            return false;
        }
        switch(change.getServiceChangeType()) {
            case ADD:
                return change.getAffectedEntity().isEmpty() && !change.getAffectedField().isEmpty();
            case ALTER:
                return !change.getAffectedEntity().isEmpty() && change.getAffectedField().size() == 1;
            case DELETE:
                return !change.getAffectedEntity().isEmpty() && change.getAffectedField().isEmpty();
        }
        return false;
    }

    private boolean dateIsApplicable(ServiceChange change) {
        LocalDate date = getCurrentDate();
        for (DateDescriptor dateDescriptor : change.getAffectedDates()) {
            if (dateDescriptor.getDate() != null && dateDescriptor.getDate().isEqual(date)) {
                return true;
            }
            if (dateDescriptor.getFrom() != null) {
                if (dateDescriptor.getTo() != null) {
                    LocalDate from = dateDescriptor.getFrom();
                    LocalDate to = dateDescriptor.getTo();
                    if ((date.isEqual(from) || date.isAfter(from)) && (date.isEqual(to) || date.isBefore(to))) {
                        return true;
                    }
                } else {
                    LocalDate from = dateDescriptor.getFrom();
                    if ((date.isEqual(from) || date.isAfter(from))) {
                        return true;
                    }
                }
            } else if (dateDescriptor.getTo() != null) {
                _log.error("Not supported: to-date with no from-date specified.");
            }
        }
        return false;
    }

    private boolean handleStopTimesChange(ServiceChange change) {
        switch(change.getServiceChangeType()) {
            case ADD:
                return handleAddStopTimes(change);
            case ALTER:
                return handleAlterStopTimes(change);
            case DELETE:
                return handleDeleteStopTimes(change);
            default:
                return false;
        }
    }

    private boolean handleAddStopTimes(ServiceChange change) {
        boolean success = true;
        for (AbstractFieldDescriptor abstractFieldDescriptor : change.getAffectedField()) {
            if (!(abstractFieldDescriptor instanceof StopTimesFields)) {
                return false;
            }
            StopTimesFields fields = (StopTimesFields) abstractFieldDescriptor;
            AgencyAndId tripId = _entitySource.getObaTripId(fields.getTripId());
            AgencyAndId stopId = _entitySource.getObaStopId(fields.getStopId());
            success &= _dao.insertStopTime(tripId, stopId, fields.getArrivalTime().toSecondOfDay(), fields.getDepartureTime().toSecondOfDay(), -1);
        }
        return success;
    }

    private boolean handleDeleteStopTimes(ServiceChange change) {
        boolean success = true;
        int nSuccess = 0, nTotal = 0;
        for (EntityDescriptor descriptor : change.getAffectedEntity()) {
            String bareTripId = descriptor.getTripId();
            String bareStopId = descriptor.getStopId();
            if (bareTripId == null || bareStopId == null) {
                _log.info("Service Change not fully applied; not enough info for stop_time");
                success = false;
                continue;
            }
            AgencyAndId tripId = _entitySource.getObaTripId(bareTripId);
            AgencyAndId stopId = _entitySource.getObaStopId(bareStopId);
            if (_dao.deleteStopTime(tripId, stopId)) {
                nSuccess++;
            } else {
                success = false;
            }
            nTotal++;
        }
        _log.info("Delete stop times: success in {} / {}", nSuccess, nTotal);
        return success;
    }

    private boolean handleAlterStopTimes(ServiceChange change) {
        boolean success = true;
        for (EntityDescriptor descriptor : change.getAffectedEntity()) {
            String bareTripId = descriptor.getTripId();
            String bareStopId = descriptor.getStopId();
            if (bareTripId == null || bareStopId == null) {
                _log.info("Service Change not fully applied; not enough info for stop_time");
                success = false;
                continue;
            }
            AgencyAndId tripId = _entitySource.getObaTripId(bareTripId);
            AgencyAndId stopId = _entitySource.getObaStopId(bareStopId);

            TripEntry trip = _dao.getTripEntryForId(tripId);
            success = false;
            if (trip != null) {
                // TODO: loop trips?
                for (StopTimeEntry stopTime : trip.getStopTimes()) {
                    if (stopTime.getStop().getId().equals(stopId)) {
                        StopTimesFields fields = (StopTimesFields) change.getAffectedField().iterator().next();
                        int arrivalTime = fields.getArrivalTime().toSecondOfDay();
                        int departureTime = fields.getDepartureTime().toSecondOfDay();
                        success = _dao.updateStopTime(tripId, stopId, stopTime.getArrivalTime(),
                                stopTime.getDepartureTime(), arrivalTime, departureTime);
                    }
                }
            }
            break;
        }
        return success;
    }

    private long getCurrentTime() {
        if (_time != -1)
            return _time;
        return new Date().getTime();
    }

    private LocalDate getCurrentDate() {
        return Instant.ofEpochMilli(getCurrentTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
