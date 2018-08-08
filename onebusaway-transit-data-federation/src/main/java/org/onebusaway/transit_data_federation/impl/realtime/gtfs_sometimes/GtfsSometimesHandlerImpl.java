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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class GtfsSometimesHandlerImpl implements GtfsSometimesHandler {

    private TransitGraphDao _dao;

    private String _agencyId;

    // for debugging
    private long _time = -1;

    private static final Logger _log = LoggerFactory.getLogger(GtfsSometimesHandlerImpl.class);

    @Override
    public boolean handleServiceChange(ServiceChange serviceChange) {
        if (!dateIsApplicable(serviceChange)) {
            _log.debug("Service change is not applicable to date.");
            return false;
        }
        switch(serviceChange.getTable()) {
            case STOPS:
                break;
            case ROUTES:
                break;
            case TRIPS:
                break;
            case STOP_TIMES:
                return handleStopTimesChange(serviceChange);
            case SHAPES:
                break;
            case TRANSFERS:
                break;
        }
        _log.error("Table not implemented: {}", serviceChange.getTable());
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
                    _log.error("Not supported: from-date with no to-date specified.");
                }
            }
        }
        return false;
    }

    private boolean handleStopTimesChange(ServiceChange change) {
        boolean success = true;
        for (EntityDescriptor descriptor : change.getAffectedEntity()) {
            String tripId = descriptor.getTripId();
            String stopId = descriptor.getStopId();
            if (tripId == null || stopId == null) {
                _log.info("Service Change not fully applied; not enough info for stop_time");
                success = false;
                continue;
            }
            switch (change.getServiceChangeType()) {
                case ADD:
                    _log.info("not implemented");
                    success = false;
                    break;
                case ALTER:
                    _log.info("not implemented");
                    success = false;
                    break;
                case DELETE:
                    _dao.deleteStopTime(id(tripId), id(stopId));
                    break;
            }
        }
        return success;
    }

    private AgencyAndId id(String id) {
        return new AgencyAndId(_agencyId, id);
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

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Autowired
    public void setAgencyId(String agencyId) {
        _agencyId = agencyId;
    }

    public void setTime(long time) {
        _time = time;
    }
}
