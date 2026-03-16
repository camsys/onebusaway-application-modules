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

@Component
public class TripModsRevertServiceImpl implements TripModsRevertService {
    private static final Logger _log = LoggerFactory.getLogger(TripModsRevertServiceImpl.class);

    private final TripModsShapeUpdateService _tripModsShapeUpdateService;
    private final TripModsTripModificationUpdateService _tripModificationUpdateService;
    private AddedStopsResult _lastKnownStopResults;
    private AddedShapesResult _lastKnownShapeResults;
    private ModifiedTripsResult _lastKnownTripModificationResults;


    @Autowired
    public TripModsRevertServiceImpl(TripModsTripModificationUpdateService tripModificationUpdateService,
                                     TripModsShapeUpdateService tripModsShapeUpdateService){
        _tripModsShapeUpdateService = tripModsShapeUpdateService;
        _tripModificationUpdateService = tripModificationUpdateService;
    }

    @Override
    public AddedStopsResult getLastKnownStopResults() {
        return _lastKnownStopResults;
    }

    @Override
    public void setLastKnownStopResults(AddedStopsResult lastKnownStopResults) {
        _lastKnownStopResults = lastKnownStopResults;
    }

    @Override
    public AddedShapesResult getLastKnownShapeResults() {
        return _lastKnownShapeResults;
    }

    @Override
    public void setLastKnownShapeResults(AddedShapesResult lastKnownShapeResults) {
        _lastKnownShapeResults = lastKnownShapeResults;
    }

    @Override
    public ModifiedTripsResult getLastKnownTripModificationResults() {
        return _lastKnownTripModificationResults;
    }

    @Override
    public void setLastKnownTripModificationResults(ModifiedTripsResult lastKnownTripModificationResults) {
        _lastKnownTripModificationResults = lastKnownTripModificationResults;
    }

    @Override
    public void revertPreviousChanges() {
        revertAddedTrips();
        revertAddedShapes();
        // TODO implement revert Added Stops
        revertAddedStops();
    }

    void revertAddedTrips() {
        if (_lastKnownTripModificationResults != null) {
            ModifiedTripsResult result =  _tripModificationUpdateService.updateTrips(
                    _lastKnownTripModificationResults.getOriginalTrips());

            _log.info("Successfully reverted {} previously added trips: {}.",
                    result.getSuccessfullyUpdatedTripsCount(),
                    result.getSuccessfullyUpdatedTripIds());

            if(result.getFailedUpdatedTripsCount() > 0){
                _log.warn("Failed to revert {} previously added trips: {}.",
                        result.getFailedUpdatedTripsCount(),
                        result.getFailedUpdatedTripIdsAsString());
            }
        }
    }

    void revertAddedShapes() {
        if (_lastKnownShapeResults != null) {
            List<AgencyAndId> shapeIdsToRemove = _lastKnownShapeResults.getSuccessfullyUpdatedShapeIds();
            _tripModsShapeUpdateService.removeShapes(shapeIdsToRemove);
            _log.info("Successfully reverted {} previously added shapes: {}.",
                    _lastKnownShapeResults.getSuccessfullyUpdatedShapeCount(),
                    _lastKnownShapeResults.getSuccessfullyUpdatedShapeIdsAsString());
        }
    }

    void revertAddedStops() {
        _log.info("Reverting added stops not yet implemented.");
        if (_lastKnownStopResults != null) {
        }
    }
}
