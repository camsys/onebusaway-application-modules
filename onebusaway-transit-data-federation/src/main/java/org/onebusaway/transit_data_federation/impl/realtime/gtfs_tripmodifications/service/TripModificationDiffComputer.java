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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface TripModificationDiffComputer {

    /**
     * Compute the diff between the original and modified stop times for a trip, and return a TripModificationDiff object that captures the differences.
     * @param entityId GTFSRT entity id
     * @param tripId trip id
     * @param originalStopTimes original stop times for the trip
     * @param modifiedStopTimes modified stop times for the trip
     * @param originalShape the original shape for the trip
     * @param replacementShapeId the shape id for the replacement shape, if the shape is modified; null otherwise
     * @param effectiveServiceDate the service date for the trip
     * @return
     */
    TripModificationDiff computeDiff(
            String entityId,
            AgencyAndId tripId,
            List<StopTimeEntry> originalStopTimes,
            ShapePoints originalShape,
            Set<Integer> originalRemovedStopTimeIndices,
            List<StopTimeEntry> modifiedStopTimes,
            AgencyAndId replacementShapeId,
            Set<Integer> modifiedAddedStopTimeIndices,
            LocalDate effectiveServiceDate);

}
