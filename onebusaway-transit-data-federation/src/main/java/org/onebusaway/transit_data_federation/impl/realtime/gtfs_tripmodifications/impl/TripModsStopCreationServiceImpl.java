/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import com.google.transit.realtime.GtfsRealtime.Stop;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.EAccessibility;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedStop;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedStops;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsStopCreationService;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class TripModsStopCreationServiceImpl implements TripModsStopCreationService {

    private static final Logger _log = LoggerFactory.getLogger(TripModsStopCreationServiceImpl.class);

    private EntityIdService _entityIdService;

    private TransitGraphDao _transitGraph;

    private NarrativeService _narrativeService;

    @Autowired
    public void setEntityIdService(EntityIdService entityIdService) {
        _entityIdService = entityIdService;
    }

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _transitGraph = dao;
    }

    @Autowired
    public void setNarrativeService(NarrativeService narrativeService) {
        _narrativeService = narrativeService;
    }


    @Override
    public AddedStops createAddedStops(List<Stop> stops) {

        AddedStops addedStops = new AddedStops();

        for (Stop stop : stops) {
            if(!isValidStop(stop)){
                addedStops.addFailedAddedStopId(stop.getStopId());
                continue;
            }

            AgencyAndId stopId = _entityIdService.getStopId(stop.getStopId());
            if(stopAlreadyExists(stopId)){
                addedStops.addFailedAddedStopId(stop.getStopId());
                continue;
            }

            StopEntryImpl stopEntry = createStopEntry(stop, stopId);
            AddedStop addedStop = new AddedStop(stopEntry, stop);

            addedStops.addStop(addedStop);
            addedStops.addSuccessfullyAddedStopId(stop.getStopId());
        }

        return addedStops;
    }

    StopEntryImpl createStopEntry(Stop stop, AgencyAndId stopId) {
        StopEntryImpl stopEntry = new StopEntryImpl(stopId, stop.getStopLat(), stop.getStopLon());
        stopEntry.setIndex(getLatestStopIndex());
        stopEntry.setWheelchairBoarding(getWheelchairBoardingAccessibilityForStop(stop));

        return stopEntry;
    }

    boolean stopAlreadyExists(AgencyAndId stopId) {
        if (_transitGraph.getStopEntryForId(stopId) != null) {
            _log.info("Stop with id {} already exists, skipping addition.", stopId.getId());
            return true;
        }
        return false;
    }

    boolean isValidStop(Stop stop) {
        if (stop.getStopId() == null || stop.getStopId().isEmpty()) {
            _log.warn("Stop with empty id found, skipping addition.");
            return false;
        }

        if (!stop.hasStopLat() && !stop.hasStopLon()) {
            _log.warn("Stop with id {} missing lat/lon, skipping addition.", stop.getStopId());
            return false;
        }

        if (!stop.hasWheelchairBoarding()) {
            _log.warn("Stop with id {} missing wheelchair boarding info, skipping addition.", stop.getStopId());
            return false;
        }
        return true;
    }

    int getLatestStopIndex() {
        return _transitGraph.getAllStops().size();
    }

    EAccessibility getWheelchairBoardingAccessibilityForStop(Stop stop) {
        switch (stop.getWheelchairBoarding().getNumber()) {
            case 1:
                return EAccessibility.ACCESSIBLE;
            case 2:
                return EAccessibility.NOT_ACCESSIBLE;
            default:
                return EAccessibility.UNKNOWN;
        }
    }

    public void addStopsToSchedule(AddedStops addedStops) {
        for(AddedStop addedStop : addedStops.getAddedStops()){
            _narrativeService.addStop(addedStop.getStopId(), newStopNarrative(addedStop.getStop()));
            _transitGraph.addStopEntry(addedStop.getStopEntry());
        }
    }

    StopNarrative newStopNarrative(Stop stop) {
        StopNarrative.Builder builder = StopNarrative.builder();
        //TODO handle multiple translations
        builder.setName(stop.getStopName().getTranslation(0).getText());
        builder.setCode(stop.getStopCode().getTranslation(0).getText());
        builder.setDescription(stop.getStopDesc().getTranslation(0).getText());
        //TODO stop direction? location type"
        builder.setDirection("direction?");
        builder.setLocationType(0);
        builder.setUrl(stop.getStopUrl().getTranslation(0).getText());
        return builder.create();
    }

}
