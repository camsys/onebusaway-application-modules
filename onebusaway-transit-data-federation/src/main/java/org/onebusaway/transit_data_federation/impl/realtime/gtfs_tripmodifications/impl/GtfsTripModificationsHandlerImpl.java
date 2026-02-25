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

import com.google.transit.realtime.GtfsRealtime.TripModifications;
import org.onebusaway.container.cache.CacheableMethodManager;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ShapeChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.StopChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChange;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripChangeHandler;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.GtfsTripModificationsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GtfsTripModificationsHandlerImpl implements GtfsTripModificationsHandler {
    private static final Logger _log = LoggerFactory.getLogger(GtfsTripModificationsHandlerImpl.class);

    private long _lastUpdatedTimestamp = -1;

    private TimeService _timeService;

    private LocalDateTime _reapplyTime;

    private RefreshService _refreshService;

    private CacheableMethodManager _cacheableMethodManager;

    private CacheableMethodManager _cacheableAnnotationInterceptor;

    private TripModsTripChangeHandler _tripChangeHandler;

    private TripChangeSet revertTripChanges;

    private boolean _isApplying = false;

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
    public void setTripModsTripChangeHandler(TripModsTripChangeHandler tripChangeHandler) {
        _tripChangeHandler = tripChangeHandler;
    }

    @Autowired
    public void setTimeService(TimeService timeService) {
        _timeService = timeService;
    }

    @Override
    public int handleTripModifications(long timestamp, Collection<TripModifications> tripModificationsList) {

        // Check whether changes should be re-applied
        if (!shouldApplyChanges(timestamp, tripModificationsList)) {
            _log.info("Not applying changes.");
            return 0;
        }

        _isApplying = true;

        //TODO implement revert changes
        int nReverted = 0; //revertPreviousChanges();

        List<TripModifications> activeChanges = filterModifications(tripModificationsList);

        int nSuccess = 0;

        for (TripModifications tm : activeChanges) {
            _log.info("Active TripModification: {}", tm);


            TripChangeSet tripChanges = _tripChangeHandler.getAllTripChanges(tm);

            revertTripChanges = _tripChangeHandler.applyChanges(tripChanges);

            nSuccess += revertTripChanges.size();
            if (nSuccess > 0 || nReverted > 0) {
                forceFlush();
            }
            _isApplying = false;

            _lastUpdatedTimestamp = timestamp;
            _reapplyTime = getReapplyTime(tripChanges.getAllChanges());

            _log.info("Done with changes. Total internal changes: {}. Total reverted changes: {}. " +
                    "Reapply time is {}, last updated is {}", nSuccess, nReverted, _reapplyTime, _lastUpdatedTimestamp);


        }

        return nSuccess;

    }

//    int revertPreviousChanges() {
//        int nTotal = 0;
//        //TODO fix shapes
////        if (revertShapeChanges != null) {
////            int success = _shapeChangeHandler.handleShapeChanges(revertShapeChanges).size();
////            if (success != revertShapeChanges.size()) {
////                _log.error("Error reverting some shapes!");
////            }
////            nTotal += success;
////        }
//        if (revertStopChanges != null) {
//            int success = _stopChangeHandler.handleStopChanges(revertStopChanges).size();
//            if (success != revertStopChanges.size()) {
//                _log.error("Error reverting some stops!");
//            }
//            nTotal += success;
//        }
//        if (revertTripChanges != null) {
//            int success = _tripChangeHandler.handleTripChanges(revertTripChanges).size();
//            if (success != revertTripChanges.size()) {
//                _log.error("Error reverting some trips!");
//            }
//            nTotal += success;
//        }
//        return nTotal;
//    }

    boolean shouldApplyChanges(long timestamp, Collection<TripModifications> tripModifications) {
        if (_lastUpdatedTimestamp == -1) {
            _log.info("First update for feed.");
            if (tripModifications.isEmpty()) {
                _log.info("Feed is empty, ignoring.");
                return false;
            }
            return true;
        } else if (_lastUpdatedTimestamp < timestamp) {
            _log.info("Update feed.");
            return true;
        } else if (_lastUpdatedTimestamp == timestamp) {
            _log.info("Feed is the same as previously processed, check reapply time ({}), current time = {}",
                    _reapplyTime, _timeService.getCurrentTime());
            return _timeService.getCurrentTime().isAfter(_reapplyTime);
        } else {
            _log.error("Non-increasing timestamps in feed!");
            return false;
        }
    }

    List<TripModifications> filterModifications(Collection<TripModifications>  tripModificationsList) {
        return tripModificationsList.stream().filter(this::isTripModificationOk).collect(Collectors.toList());
    }

    boolean isTripModificationOk(TripModifications tripModifications) {
        if (!validateModifications(tripModifications)) {
            _log.debug("service change is invalid");
            return false;
        }
        return true;
    }

    public boolean isApplying() {
        return _isApplying;
    }

    private boolean validateModifications(TripModifications tripModifications) {
        if (tripModifications.getServiceDatesList().isEmpty()) {
            _log.info("affected dates is empty");
            //TODO change this to false- keeping it as true for now to allow testing with existing data
            return true;
        }
        //TODO - not sure if this is applicable for TripModifications
//        switch(change.getServiceChangeType()) {
//            case ADD:
//                return change.getAffectedEntity().isEmpty() && !change.getAffectedField().isEmpty();
//            case ALTER:
//                return !change.getAffectedEntity().isEmpty() && change.getAffectedField().size() == 1;
//            case DELETE:
//                return !change.getAffectedEntity().isEmpty() && change.getAffectedField().isEmpty();
//        }
//        return false;
        return true;
    }

    // Re-apply time is earliest of: midnight tonight, or (future) end time of a trip that begins on the previous service day.
    LocalDateTime getReapplyTime(List<TripChange> tripChanges) {
        LocalDate today = _timeService.getCurrentDate();
        LocalDateTime now = _timeService.getCurrentTime();
        LocalDateTime reapplyTime = today.atStartOfDay().plusDays(1); // midnight tonight
        for (TripChange tripChange : tripChanges) {
            if (tripChange.getServiceDate().isBefore(today)) {
                LocalDateTime endTime = tripChange.getEndTime();
                if (endTime.isAfter(now) && endTime.isBefore(reapplyTime)) {
                    reapplyTime = endTime;
                }
            }
        }
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

}
