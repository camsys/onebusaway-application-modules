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

import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShapesResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedStopsResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTripsResult;

public interface TripModsRevertService {
    AddedStopsResult getLastKnownStopResults(String feedId);

    void setLastKnownStopResults(String feedId, AddedStopsResult lastKnownStopResults);

    AddedShapesResult getLastKnownShapeResults(String feedId);

    void setLastKnownShapeResults(String feedId, AddedShapesResult lastKnownShapeResults);

    ModifiedTripsResult getLastKnownTripModificationResults(String feedId);

    void setLastKnownTripModificationResults(String feedId, ModifiedTripsResult lastKnownTripModificationResults);

    void revertPreviousChanges(String feedId);

    /** Discard last-known state for every feed, e.g. because the transit graph itself was refreshed. */
    void clearAll();
}
