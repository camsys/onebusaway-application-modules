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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl;

import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import com.camsys.transit.servicechange.field_descriptors.StopsFields;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.StopChange;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.StopChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.StopChangeHandler;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class StopChangeHandlerImpl implements StopChangeHandler {

    private static final Logger _log = LoggerFactory.getLogger(StopChangeHandlerImpl.class);

    private EntityIdService _entityIdService;

    private TransitGraphDao _dao;

    private NarrativeService _narrativeService;

    @Autowired
    public void setEntityIdService(EntityIdService entityIdService) {
        _entityIdService = entityIdService;
    }

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Autowired
    public void setNarrativeService(NarrativeService narrativeService) {
        _narrativeService = narrativeService;
    }

    @Override
    public StopChangeSet getAllStopChanges(Collection<ServiceChange> changes) {
        List<StopChange> stopChanges = new ArrayList<>();
        for (ServiceChange change : changes) {
            if (Table.STOPS.equals(change.getTable())) {
                if (ServiceChangeType.ALTER.equals(change.getServiceChangeType())) {
                    for (EntityDescriptor entity : change.getAffectedEntity()) {
                        StopChange stopChange = new StopChange(entity.getStopId());
                        StopsFields stopsFields = (StopsFields) change.getAffectedField().get(0);
                        if (stopsFields.getStopName() != null) {
                            stopChange.setStopName(stopsFields.getStopName());
                        }
                        if (stopsFields.getStopLat() != null) {
                            stopChange.setStopLat(stopsFields.getStopLat());
                        }
                        if (stopsFields.getStopLon() != null) {
                            stopChange.setStopLon(stopsFields.getStopLon());
                        }
                        stopChanges.add(stopChange);
                    }
                } else {
                    _log.info("Type {} not handled for table {}", change.getServiceChangeType(), change.getTable());
                }
            }
        }
        return new StopChangeSet(stopChanges);
    }

    @Override
    public int handleStopChanges(StopChangeSet changeset) {
        int nSuccess = 0;
        for (StopChange stopChange : changeset.getStopChanges()) {
            if (handleStopChange(stopChange)) {
                nSuccess++;
            }
        }
        return nSuccess;
    }


    private boolean handleStopChange(StopChange change) {
        AgencyAndId stopId = _entityIdService.getStopId(change.getStopId());
        StopEntry oldStopEntry = _dao.getStopEntryForId(stopId);
        StopNarrative narrative = _narrativeService.removeStop(stopId);
        String stopName = change.hasStopName() ? change.getStopName() : narrative.getName();
        int index = oldStopEntry.getIndex();
        double lat = change.hasStopLat() ? change.getStopLat() : oldStopEntry.getStopLat();
        double lon = change.hasStopLon() ? change.getStopLon() : oldStopEntry.getStopLon();
        StopEntryImpl stopEntry = new StopEntryImpl(stopId, lat, lon);
        stopEntry.setIndex(index);
        stopEntry.setWheelchairBoarding(oldStopEntry.getWheelchairBoarding());
        _narrativeService.addStop(stopEntry, stopName);
        if (change.hasStopLat() || change.hasStopLon()) {
            _dao.removeStopEntry(stopId);
            _dao.addStopEntry(stopEntry);
        }
        return true;
    }
}
