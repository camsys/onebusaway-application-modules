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

import org.onebusaway.container.cache.CacheableMethodManager;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.TripModificationConfiguration;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.*;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GtfsTripModificationsHandlerImpl implements GtfsTripModificationsHandler {

    private static final Logger _log = LoggerFactory.getLogger(GtfsTripModificationsHandlerImpl.class);

    // Tracked per feed id so that one feed's poll cycle can never revert or short-circuit
    // another feed's independently-tracked state.
    private final Map<String, byte[]> _lastKnownHashByFeed = new ConcurrentHashMap<>();

    private final Map<String, LocalDateTime> _reapplyTimeByFeed = new ConcurrentHashMap<>();

    private TimeService _timeService;

    private RefreshService _refreshService;

    private CacheableMethodManager _cacheableMethodManager;

    private CacheableMethodManager _cacheableAnnotationInterceptor;

    private TripModsStopCreationService _tripModsStopCreationService;

    private TripModsShapeCreationService _tripModsShapeCreationService;

    private TripModsShapeUpdateService _tripModsShapeUpdateService;

    private TripModsTripModificationCreationService _tripModificationCreationService;

    private TripModsTripModificationUpdateService _tripModificationUpdateService;

    private TripModsRevertService _tripModsRevertService;

    private TripModificationDiffService _tripModificationDiffService;

    private boolean _isApplying = false;

    private final Object _applyingLock = new Object();


    @Autowired
    public void setRefreshService(RefreshService refreshService) {
        _refreshService = refreshService;
    }

    @Autowired
    @Qualifier("cacheableMethodManager")
    public void setCacheableMethodManager(CacheableMethodManager cacheableMethodManager) {
        _cacheableMethodManager = cacheableMethodManager;
    }

    @Autowired
    @Qualifier("cacheableAnnotationInterceptor")
    public void setCacheableAnnotationInterceptor(CacheableMethodManager cacheableAnnotationInterceptor) {
        _cacheableAnnotationInterceptor = cacheableAnnotationInterceptor;
    }

    @Autowired
    public void setAddedStopCreationService(TripModsStopCreationService tripModsStopCreationService) {
        _tripModsStopCreationService = tripModsStopCreationService;
    }

    @Autowired
    public void setAddedShapeCreationService(TripModsShapeCreationService tripModsShapeCreationService) {
        _tripModsShapeCreationService = tripModsShapeCreationService;
    }

    @Autowired
    public void setAddedShapeUpdateService(TripModsShapeUpdateService tripModsShapeUpdateService) {
        _tripModsShapeUpdateService = tripModsShapeUpdateService;
    }

    @Autowired
    public void setTripModificationCreationService(TripModsTripModificationCreationService tripModificationCreationService) {
        _tripModificationCreationService = tripModificationCreationService;
    }

    @Autowired
    public void setTripModificationUpdateService(TripModsTripModificationUpdateService tripModificationUpdateService) {
        _tripModificationUpdateService = tripModificationUpdateService;
    }

    @Autowired
    public void setTripModsRevertService(TripModsRevertService tripModsRevertService) {
        _tripModsRevertService = tripModsRevertService;
    }

    @Autowired
    public void setTripModificationDiffService(TripModificationDiffService tripModificationDiffService) {
        _tripModificationDiffService = tripModificationDiffService;
    }

    @Autowired
    public void setTimeService(TimeService timeService) {
        _timeService = timeService;
    }

    @Override
    public void handleTripModifications(String feedId,
                                        TripModificationsChanges tripModificationsChanges,
                                        TripModificationConfiguration tripModificationConfiguration) {

        synchronized (_applyingLock) {
            // Check whether changes should be re-applied. A single shared lock across all feeds
            // is intentional here (not per-feed): concurrently mutating the graph from two feeds'
            // schedulers firing at the same instant would be unsafe regardless of feed scoping.
            if (!shouldApplyChanges(feedId, tripModificationsChanges)) {
                _log.info("Not applying Trip Modification changes for feed {}.", feedId);
                return;
            }
            try {
                _isApplying = true;
                _tripModsRevertService.revertPreviousChanges(feedId);

                //AddedStops addedStops = _tripModsStopCreationService.createAddedStops(tripModificationsChanges.getStops());
                AddedShapes addedShapes = _tripModsShapeCreationService.createAddedShapes(tripModificationsChanges.getShapes());

                //Process added shapes before trips
                AddedShapesResult addedShapesResult = _tripModsShapeUpdateService.addShapes(addedShapes.getAddedShapes());

                ModifiedTrips modifiedTrips = _tripModificationCreationService.createModifiedTrips(tripModificationsChanges.getTripModifications());
                Collection<TripModificationDiff> diffs = _tripModificationDiffService.createDiffsFromModifications(feedId, modifiedTrips, tripModificationConfiguration);

                _tripModsRevertService.setLastKnownShapeResults(feedId, addedShapesResult);

                ModifiedTripsResult modifiedTripsResult = _tripModificationUpdateService.updateTrips(modifiedTrips.getModifiedTrips());
                _tripModsRevertService.setLastKnownTripModificationResults(feedId, modifiedTripsResult);

                if (hasSuccessfulUpdates(addedShapesResult, modifiedTripsResult)) {
                    forceFlush();
                    _lastKnownHashByFeed.put(feedId, tripModificationsChanges.getHash());
                    _reapplyTimeByFeed.put(feedId, getReapplyTime(modifiedTrips));
                } else {
                    _log.warn("No trip modifications were successfully applied for feed {}; will retry on next poll.", feedId);
                }
            }
            catch (Exception ex) {
                _log.error("Error processing trip modifications for feed {}", feedId, ex);
            }
            finally {
                _isApplying = false;
            }
        }
    }

    private boolean hasSuccessfulUpdates(AddedShapesResult addedShapesResult,
                                         ModifiedTripsResult modifiedTripsResult) {
        if(addedShapesResult.getSuccessfullyUpdatedShapeCount() > 0){
            return true;
        }
        if(modifiedTripsResult.getSuccessfullyUpdatedTripsCount() > 0){
            return true;
        }
        return false;
    }

    boolean shouldApplyChanges(String feedId, TripModificationsChanges tripModificationsChanges) {
        byte[] lastKnownHash = _lastKnownHashByFeed.get(feedId);
        if (lastKnownHash == null) {
            _log.info("First update for Trip Modifications feed {}.", feedId);
            if (tripModificationsChanges.hasChanges()) {
                return true;
            } else {
                _log.info("Trip Modifications feed {} is empty, ignoring.", feedId);
                return false;
            }
        } else if (!Arrays.equals(lastKnownHash, tripModificationsChanges.getHash())) {
            _log.info("Trip Modifications feed {} changes detected, updating feed.", feedId);
            return true;
        }

        LocalDateTime reapplyTime = _reapplyTimeByFeed.get(feedId);
        if (reapplyTime != null) {
            LocalDateTime currentTime = _timeService.getCurrentTime();
            if (currentTime.isAfter(reapplyTime)) {
                _log.debug("Trip Modifications Feed {} is the same as previously processed, check reapply time ({}), current time = {}",
                        feedId, reapplyTime, currentTime);
                _log.info("The current time = {} is after reapply time {} for feed {}", currentTime, reapplyTime, feedId);
                return true;
            }
        }

        _log.debug("No changes detected in Trip Modifications feed {}.", feedId);
        return false;
    }


    // Re-apply time is earliest of:
    // 1) 3:00AM Local time tonight
    // 2) (Future) End time of a trip that begins on the previous service day
    LocalDateTime getReapplyTime(ModifiedTrips modifiedTrips) {
        LocalDate today = _timeService.getCurrentDate();
        LocalDateTime reapplyTime = today.atStartOfDay().plusDays(1).plusHours(3); // 3am tonight

        // Not sure if we need Option 2
        /*
        LocalDateTime now = _timeService.getCurrentTime();
        for (ModifiedTrip modifiedTrip : modifiedTrips.getModifiedTrips()) {
            if (modifiedTrip.getServiceDate().isBefore(today)) {
                LocalDateTime endTime = modifiedTrip.getEndTime();
                if (endTime.isAfter(now) && endTime.isBefore(reapplyTime)) {
                    reapplyTime = endTime;
                }
            }
        }
        */

        return reapplyTime;
    }

    void forceFlush() {
        _refreshService.refresh(RefreshableResources.BLOCK_INDEX_DATA_GRAPH);
        try {
            if (_cacheableMethodManager != null) {
                _cacheableMethodManager.flush();
            }
            if (_cacheableAnnotationInterceptor != null) {
                _cacheableAnnotationInterceptor.flush();
            }
        } catch (Throwable t) {
            _log.error("issue flushing cache:", t);
        }
    }

    @Refreshable(dependsOn = RefreshableResources.TRANSIT_GRAPH)
    public void onTransitGraphRefresh() {
        synchronized (_applyingLock) {
            _log.info("Transit graph refreshed; resetting Trip Modifications state for all feeds so changes are reapplied.");
            _lastKnownHashByFeed.clear();
            _reapplyTimeByFeed.clear();
            _tripModsRevertService.clearAll();
        }
    }

    @Override
    public void resetLastUpdatedTime(String feedId) {
        synchronized (_applyingLock) {
            _lastKnownHashByFeed.remove(feedId);
            _reapplyTimeByFeed.remove(feedId);
        }
    }

    @Override
    public boolean isApplying() {
        return _isApplying;
    }

}
