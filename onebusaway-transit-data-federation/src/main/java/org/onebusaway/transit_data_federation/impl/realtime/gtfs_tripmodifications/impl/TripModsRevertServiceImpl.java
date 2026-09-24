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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShapesResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedStopsResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTripsResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsRevertService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsShapeUpdateService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripModificationUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TripModsRevertServiceImpl implements TripModsRevertService {
    private static final Logger _log = LoggerFactory.getLogger(TripModsRevertServiceImpl.class);

    private final TripModsShapeUpdateService _tripModsShapeUpdateService;
    private final TripModsTripModificationUpdateService _tripModificationUpdateService;

    // Tracked per feed id so that reverting one feed's changes can never touch another feed's
    // currently-applied state.
    private final Map<String, AddedStopsResult> _lastKnownStopResultsByFeed = new ConcurrentHashMap<>();
    private final Map<String, AddedShapesResult> _lastKnownShapeResultsByFeed = new ConcurrentHashMap<>();
    private final Map<String, ModifiedTripsResult> _lastKnownTripModificationResultsByFeed = new ConcurrentHashMap<>();


    @Autowired
    public TripModsRevertServiceImpl(TripModsTripModificationUpdateService tripModificationUpdateService,
                                     TripModsShapeUpdateService tripModsShapeUpdateService){
        _tripModsShapeUpdateService = tripModsShapeUpdateService;
        _tripModificationUpdateService = tripModificationUpdateService;
    }

    @Override
    public AddedStopsResult getLastKnownStopResults(String feedId) {
        return _lastKnownStopResultsByFeed.get(feedId);
    }

    @Override
    public void setLastKnownStopResults(String feedId, AddedStopsResult lastKnownStopResults) {
        putOrRemove(_lastKnownStopResultsByFeed, feedId, lastKnownStopResults);
    }

    @Override
    public AddedShapesResult getLastKnownShapeResults(String feedId) {
        return _lastKnownShapeResultsByFeed.get(feedId);
    }

    @Override
    public void setLastKnownShapeResults(String feedId, AddedShapesResult lastKnownShapeResults) {
        putOrRemove(_lastKnownShapeResultsByFeed, feedId, lastKnownShapeResults);
    }

    @Override
    public ModifiedTripsResult getLastKnownTripModificationResults(String feedId) {
        return _lastKnownTripModificationResultsByFeed.get(feedId);
    }

    @Override
    public void setLastKnownTripModificationResults(String feedId, ModifiedTripsResult lastKnownTripModificationResults) {
        putOrRemove(_lastKnownTripModificationResultsByFeed, feedId, lastKnownTripModificationResults);
    }

    @Override
    public void revertPreviousChanges(String feedId) {
        revertAddedTrips(feedId);
        revertAddedShapes(feedId);
        // TODO implement revert Added Stops
        revertAddedStops(feedId);
    }

    @Override
    public void clearAll() {
        _lastKnownStopResultsByFeed.clear();
        _lastKnownShapeResultsByFeed.clear();
        _lastKnownTripModificationResultsByFeed.clear();
    }

    private <T> void putOrRemove(Map<String, T> map, String feedId, T value) {
        if (value == null) {
            map.remove(feedId);
        } else {
            map.put(feedId, value);
        }
    }

    void revertAddedTrips(String feedId) {
        ModifiedTripsResult lastKnownTripModificationResults = _lastKnownTripModificationResultsByFeed.get(feedId);
        if (lastKnownTripModificationResults != null) {
            ModifiedTripsResult result = _tripModificationUpdateService.updateTrips(
                    lastKnownTripModificationResults.getOriginalTrips());

            _log.info("Successfully reverted {} previously added trips for feed {}: {}.",
                    result.getSuccessfullyUpdatedTripsCount(),
                    feedId,
                    result.getSuccessfullyUpdatedTripIds());

            if (result.getFailedUpdatedTripsCount() > 0) {
                _log.warn("Failed to revert {} previously added trips for feed {}: {}.",
                        result.getFailedUpdatedTripsCount(),
                        feedId,
                        result.getFailedUpdatedTripIdsAsString());
            }
        }
    }

    void revertAddedShapes(String feedId) {
        AddedShapesResult lastKnownShapeResults = _lastKnownShapeResultsByFeed.get(feedId);
        if (lastKnownShapeResults != null) {
            List<AgencyAndId> shapeIdsToRemove = lastKnownShapeResults.getSuccessfullyUpdatedShapeIds();
            _tripModsShapeUpdateService.removeShapes(shapeIdsToRemove);
            _log.info("Successfully reverted {} previously added shapes for feed {}: {}.",
                    lastKnownShapeResults.getSuccessfullyUpdatedShapeCount(),
                    feedId,
                    lastKnownShapeResults.getSuccessfullyUpdatedShapeIdsAsString());
        }
    }

    void revertAddedStops(String feedId) {
        _log.info("Reverting added stops not yet implemented (feed {}).", feedId);
        if (_lastKnownStopResultsByFeed.get(feedId) != null) {
        }
    }
}
