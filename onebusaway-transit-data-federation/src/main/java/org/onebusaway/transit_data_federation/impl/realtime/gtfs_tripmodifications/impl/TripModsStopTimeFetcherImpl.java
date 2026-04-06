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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.StopEntryData;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsStopTimeFetcher;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TripModsStopTimeFetcherImpl implements TripModsStopTimeFetcher {
    private final TransitGraphDao dao;
    private final NarrativeService narrativeService;
    private final EntityIdService entityIdService;

    @Autowired
    public TripModsStopTimeFetcherImpl(TransitGraphDao dao,
                                       NarrativeService narrativeService,
                                       EntityIdService entityIdService) {
        this.dao = dao;
        this.narrativeService = narrativeService;
        this.entityIdService = entityIdService;
    }

    @Override
    public StopEntryData getStopEntry(String rawStopId) {
        AgencyAndId stopId = entityIdService.getStopId(rawStopId);
        StopEntry stopEntry = dao.getStopEntryForId(stopId);
        if (stopEntry == null) {
            throw new IllegalArgumentException("Stop entry not found for id: " + stopId);
        }

        StopNarrative narrative = narrativeService.getStopForId(stopId);
        if (narrative == null) {
            throw new IllegalArgumentException("Stop narrative not found for id: " + stopId);
        }

        return new StopEntryData(stopId, stopEntry.getStopLat(), stopEntry.getStopLon(), narrative.getName());
    }

}
