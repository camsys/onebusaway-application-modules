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

import org.onebusaway.gtfs.model.AgencyAndId;

import java.util.*;
import java.util.stream.Collectors;

public class ModifiedTrips {

    private final List<ModifiedTrip> modifiedTrips = new ArrayList<>();

    private final Set<String> failedModifiedTripIds = new LinkedHashSet<>();

    private final Set<String> successfullyModifiedTripIds = new LinkedHashSet<>();

    public void addModifiedTrip(ModifiedTrip modifiedTrip) {
        modifiedTrips.add(modifiedTrip);
        successfullyModifiedTripIds.add(AgencyAndId.convertToString(modifiedTrip.getTripId()));
    }

    public void addAllModifiedTrip(List<ModifiedTrip> modifiedTripList) {
        modifiedTripList.forEach(this::addModifiedTrip);
    }

    public List<ModifiedTrip> getModifiedTrips() {
        return Collections.unmodifiableList(modifiedTrips);
    }

    public int getSuccessfullyModifiedCount() {
        return successfullyModifiedTripIds.size();
    }

    public int getFailedModifiedCount() {
        return failedModifiedTripIds.size();
    }

    public Set<String> getFailedModifiedTripIds() {
        return Collections.unmodifiableSet(failedModifiedTripIds);
    }

    public Set<String> getSuccessfullyModifiedTripIds() {
        return Collections.unmodifiableSet(successfullyModifiedTripIds);
    }

    public void addSuccessfullyModifiedTripId(String tripId) {
        this.successfullyModifiedTripIds.add(tripId);
    }

    public String getSuccessfullyModifiedTripIdsAsString() {
        return successfullyModifiedTripIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public void addFailedModifiedTripId(String tripId) {
        this.failedModifiedTripIds.add(tripId);
    }

    public void addAllFailedModifiedTripIds(Set<String> modifiedTripList) {
        this.failedModifiedTripIds.addAll(modifiedTripList);
    }

    public String getFailedModifiedTripIdsAsString() {
        return failedModifiedTripIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
