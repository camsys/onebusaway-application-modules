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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AddTrip implements TripChange {

    private AgencyAndId tripId;

    private TripEntryImpl tripEntry;

    private TripNarrative tripNarrative;

    private LocalDate serviceDate;

    @Override
    public AgencyAndId getTripId() {
        return tripId;
    }

    public void setTripId(AgencyAndId tripId) {
        this.tripId = tripId;
    }

    public TripEntryImpl getTripEntry() {
        return tripEntry;
    }

    public void setTripEntry(TripEntryImpl tripEntry) {
        this.tripEntry = tripEntry;
    }

    public TripNarrative getTripNarrative() {
        return tripNarrative;
    }

    public void setTripNarrative(TripNarrative tripNarrative) {
        this.tripNarrative = tripNarrative;
    }

    @Override
    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    @Override
    public LocalDateTime getEndTime() {
        StopTimeEntry stopTime = tripEntry.getStopTimes().get(tripEntry.getStopTimes().size() - 1);
        return serviceDate.atStartOfDay().plusSeconds(stopTime.getArrivalTime());
    }
}
