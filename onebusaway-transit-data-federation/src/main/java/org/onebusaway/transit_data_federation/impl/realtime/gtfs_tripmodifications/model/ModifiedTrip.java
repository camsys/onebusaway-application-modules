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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class ModifiedTrip {
    private final String entityId;
    private final AgencyAndId tripId;
    private final AgencyAndId shapeId;
    private final LocalDate serviceDate;
    private final TripEntryImpl tripEntry;
    private final ModifiedStopTimes modifiedStopTimes;
    private final List<GtfsRealtime.TripModifications.Modification> modifications;


    public ModifiedTrip(String entityId,
                        AgencyAndId tripId,
                        AgencyAndId shapeId,
                        ModifiedStopTimes modifiedStopTimes,
                        TripEntryImpl tripEntry,
                        List<GtfsRealtime.TripModifications.Modification> modifications,
                        LocalDate serviceDate) {
        this.entityId = entityId;
        this.tripId = tripId;
        this.shapeId = shapeId;
        this.tripEntry = tripEntry;
        this.serviceDate = serviceDate;
        this.modifiedStopTimes = modifiedStopTimes;
        this.modifications = modifications;
    }

    public String getEntityId() {
        return entityId;
    }

    public AgencyAndId getTripId() {
        return tripId;
    }

    public AgencyAndId getShapeId() {
        return shapeId;
    }

    public TripEntryImpl getTripEntry() {
        return tripEntry;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public ModifiedStopTimes getModifiedStopTimes() {
        return modifiedStopTimes;
    }

    public List<GtfsRealtime.TripModifications.Modification> getModifications() {
        return modifications;
    }

    public LocalDateTime getEndTime() {
        StopTimeEntry stopTime = tripEntry.getStopTimes().get(tripEntry.getStopTimes().size() - 1);
        return serviceDate.atStartOfDay().plusSeconds(stopTime.getArrivalTime());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ModifiedTrip that = (ModifiedTrip) o;
        return Objects.equals(entityId, that.entityId) && Objects.equals(tripId, that.tripId) && Objects.equals(shapeId, that.shapeId) && Objects.equals(serviceDate, that.serviceDate) && Objects.equals(tripEntry, that.tripEntry) && Objects.equals(modifiedStopTimes, that.modifiedStopTimes) && Objects.equals(modifications, that.modifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, tripId, shapeId, serviceDate, tripEntry, modifiedStopTimes, modifications);
    }
}
