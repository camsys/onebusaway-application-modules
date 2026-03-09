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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModifiedTripsResult {
    private final List<AgencyAndId> failedUpdatedTripIds = new ArrayList<>();
    private final List<AgencyAndId> successfullyUpdatedTripIds = new ArrayList<>();
    private final List<ModifiedTrip> originalTrips = new ArrayList<>();

    public void addFailedUpdatedTripId(AgencyAndId tripId) {
        this.failedUpdatedTripIds.add(tripId);
    }

    public List<AgencyAndId> getFailedUpdatedTripIds() {
        return Collections.unmodifiableList(failedUpdatedTripIds);
    }

    public void addSuccessfullyUpdatedTripId(AgencyAndId tripId) {
        this.successfullyUpdatedTripIds.add(tripId);
    }

    public List<AgencyAndId> getSuccessfullyUpdatedTripIds() {
        return Collections.unmodifiableList(successfullyUpdatedTripIds);
    }

    public int getSuccessfullyUpdatedTripsCount() {
        return successfullyUpdatedTripIds.size();
    }

    public int getFailedUpdatedTripsCount() {
        return failedUpdatedTripIds.size();
    }

    public void addOriginalTrip(ModifiedTrip modifiedTrip) {
        this.originalTrips.add(modifiedTrip);
    }

    public List<ModifiedTrip> getOriginalTrips() {
        return Collections.unmodifiableList(originalTrips);
    }

    public String getSuccessfullyUpdatedTripIdsAsString() {
        return successfullyUpdatedTripIds.stream()
                .filter(Objects::nonNull)
                .map(AgencyAndId::convertToString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public String getFailedUpdatedTripIdsAsString() {
        return failedUpdatedTripIds.stream()
                .filter(Objects::nonNull)
                .map(AgencyAndId::convertToString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

}
