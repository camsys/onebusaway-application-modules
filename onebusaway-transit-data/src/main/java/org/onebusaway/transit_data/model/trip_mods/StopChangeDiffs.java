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

import java.util.*;

public class StopChangeDiffs {
    private final List<StopChangeDiff> stopChangeDiffs = new ArrayList<>();
    private final Map<AgencyAndId, StopTimeSnapshot> originalStopTimeSnapshots = new LinkedHashMap<>();
    private final Map<AgencyAndId, StopTimeSnapshot>  modifiedStopTimeSnapshots = new LinkedHashMap<>();
    private final Map<Integer, StopTimeSnapshot>  addedStopTimes = new LinkedHashMap<>();
    private final Map<Integer, StopTimeSnapshot> removedStopTimes = new LinkedHashMap<>();

    public List<StopChangeDiff> getStopChangeDiffs() {
        return Collections.unmodifiableList(stopChangeDiffs);
    }

    public Map<Integer, StopTimeSnapshot> getAddedStopTimes() {
        return addedStopTimes;
    }
    public Map<Integer, StopTimeSnapshot> getRemovedStopTimes() {
        return removedStopTimes;
    }

    public Map<AgencyAndId, StopTimeSnapshot> getModifiedStopTimeSnapshots() {
        return Collections.unmodifiableMap(modifiedStopTimeSnapshots);
    }

    public Map<AgencyAndId, StopTimeSnapshot> getOriginalStopTimeSnapshots() {
        return Collections.unmodifiableMap(originalStopTimeSnapshots);
    }

    public void addAllStopChangeDiffs(List<StopChangeDiff> stopChangeDiffs) {
        this.stopChangeDiffs.addAll(stopChangeDiffs);
    }

    public void addModifiedAddedStopTimeBySequence(Integer index, StopTimeSnapshot addedStopTimeSnapshot) {
        this.addedStopTimes.put(index, addedStopTimeSnapshot);
    }

    public void addOriginalRemovedStopTimeBySequence(Integer index, StopTimeSnapshot removedStopTimeSnapshot) {
        this.removedStopTimes.put(index, removedStopTimeSnapshot);
    }

    public void addModifiedStopTimeSnapshot(AgencyAndId stopId, StopTimeSnapshot modifiedStopTimeSnapshot) {
        this.modifiedStopTimeSnapshots.put(stopId, modifiedStopTimeSnapshot);
    }

    public void addOriginalStopTimeSnapshot(AgencyAndId stopId, StopTimeSnapshot oldStopTimeSnapshot) {
        this.originalStopTimeSnapshots.put(stopId, oldStopTimeSnapshot);
    }
}
