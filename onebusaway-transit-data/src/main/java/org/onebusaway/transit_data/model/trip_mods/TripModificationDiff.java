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

import java.io.Serializable;
import java.util.List;

public class TripModificationDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tripId;
    private String routeId;
    private String effectiveServiceDate;
    private long lastUpdated;
    private List<StopTimeSnapshot> originalStopTimes;
    private List<StopTimeSnapshot> modifiedStopTimes;
    private List<StopChangeDiff> changes;
    private ShapeModificationDiff shapeDiff;

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getEffectiveServiceDate() {
        return effectiveServiceDate;
    }

    public void setEffectiveServiceDate(String effectiveServiceDate) {
        this.effectiveServiceDate = effectiveServiceDate;
    }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public List<StopTimeSnapshot> getOriginalStopTimes() { return originalStopTimes; }
    public void setOriginalStopTimes(List<StopTimeSnapshot> originalStopTimes) { this.originalStopTimes = originalStopTimes; }

    public List<StopTimeSnapshot> getModifiedStopTimes() { return modifiedStopTimes; }
    public void setModifiedStopTimes(List<StopTimeSnapshot> modifiedStopTimes) { this.modifiedStopTimes = modifiedStopTimes; }

    public List<StopChangeDiff> getChanges() { return changes; }
    public void setChanges(List<StopChangeDiff> changes) { this.changes = changes; }

    public ShapeModificationDiff getShapeDiff() { return shapeDiff; }
    public void setShapeDiff(ShapeModificationDiff shapeDiff) { this.shapeDiff = shapeDiff; }
}