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

import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;

import java.util.*;

public class ModifiedStopTimes {

    private final List<StopTimeEntry> updatedStopTimes = new ArrayList<>();
    private final Set<Integer> originalRemovedStopTimeIndices = new LinkedHashSet<>();
    private final Set<Integer> modifiedAddedStopTimeIndices = new LinkedHashSet<>();

    public List<StopTimeEntry> getUpdatedStopTimes() {
        return Collections.unmodifiableList(updatedStopTimes);
    }

    public void addUpdatedStopTimes(Collection<StopTimeEntry> modifiedStopTimes) {
        this.updatedStopTimes.addAll(modifiedStopTimes);
    }

    public Set<Integer> getModifiedAddedStopTimeIndices() {
        return modifiedAddedStopTimeIndices;
    }

    public void addModifiedAddedStopTimeIndices(Collection<Integer> modifiedAddedStopTimeIndices) {
        this.modifiedAddedStopTimeIndices.addAll(modifiedAddedStopTimeIndices);
    }

    public Set<Integer> getOriginalRemovedStopTimeIndices() {
        return originalRemovedStopTimeIndices;
    }

    public void addOriginalRemovedStopTimeIndices(Collection<Integer> originalRemovedStopTimeIndices) {
        this.originalRemovedStopTimeIndices.addAll(originalRemovedStopTimeIndices);
    }
}
