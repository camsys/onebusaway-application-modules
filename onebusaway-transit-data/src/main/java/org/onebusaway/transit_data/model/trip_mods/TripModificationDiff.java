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
package org.onebusaway.transit_data.model.trip_mods;

import org.onebusaway.gtfs.model.AgencyAndId;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class TripModificationDiff implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String entityId;
    private final String tripId;
    private final LocalDate effectiveServiceDate;
    private final long lastUpdated;
    private final Map<AgencyAndId, StopTimeSnapshot>  originalStopTimes;
    private final Map<AgencyAndId, StopTimeSnapshot>  modifiedStopTimes;
    private final List<StopChangeDiff> changes;
    private final ShapeModificationDiff shapeDiff;
    private final Map<Integer, StopTimeSnapshot> removedBySequence;
    private final Map<Integer, StopTimeSnapshot> addedBySequence;

    public TripModificationDiff(String entityId,
                                String tripId,
                                LocalDate effectiveServiceDate,
                                long lastUpdated,
                                Map<AgencyAndId, StopTimeSnapshot> originalStopTimes,
                                Map<AgencyAndId, StopTimeSnapshot> modifiedStopTimes,
                                List<StopChangeDiff> changes,
                                ShapeModificationDiff shapeDiff,
                                Map<Integer, StopTimeSnapshot> removedBySequence,
                                Map<Integer, StopTimeSnapshot> addedBySequence) {
        this.entityId = entityId;
        this.tripId = tripId;
        this.effectiveServiceDate = effectiveServiceDate;
        this.lastUpdated = lastUpdated;
        this.originalStopTimes = originalStopTimes;
        this.modifiedStopTimes = modifiedStopTimes;
        this.changes = changes;
        this.shapeDiff = shapeDiff;
        this.removedBySequence = removedBySequence;
        this.addedBySequence = addedBySequence;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getTripId() { return tripId; }

    public LocalDate getEffectiveServiceDate() {
        return effectiveServiceDate;
    }

    public long getLastUpdated() { return lastUpdated; }

    public Map<AgencyAndId, StopTimeSnapshot> getOriginalStopTimes() { return originalStopTimes; }

    public Map<AgencyAndId, StopTimeSnapshot> getModifiedStopTimes() { return modifiedStopTimes; }

    public List<StopChangeDiff> getChanges() { return changes; }

    public ShapeModificationDiff getShapeDiff() { return shapeDiff; }

    public Map<Integer, StopTimeSnapshot> getRemovedBySequence() { return removedBySequence; }

    public Map<Integer, StopTimeSnapshot> getAddedBySequence() { return addedBySequence; }

}