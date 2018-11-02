/**
 * Copyright (C) 2018 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl;

import com.camsys.transit.servicechange.ServiceChange;
import org.onebusaway.container.cache.CacheableMethodManager;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ShapeChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.StopChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.GtfsSometimesHandler;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.ShapeChangeHandler;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.StopChangeHandler;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TripChangeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GtfsSometimesHandlerImpl implements GtfsSometimesHandler {

    private static final Logger _log = LoggerFactory.getLogger(GtfsSometimesHandlerImpl.class);

    private RefreshService _refreshService;

    private CacheableMethodManager _cacheableMethodManager;

    private CacheableMethodManager _cacheableAnnotationInterceptor;

    private ShapeChangeHandler _shapeChangeHandler;

    private StopChangeHandler _stopChangeHandler;

    private TripChangeHandler _tripChangeHandler;

    private boolean _isApplying = false;

    private TripChangeSet revertTripChanges;

    private ShapeChangeSet revertShapeChanges;

    private StopChangeSet revertStopChanges;

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
    public void setShapeChangeHandler(ShapeChangeHandler shapeChangeHandler) {
        _shapeChangeHandler = shapeChangeHandler;
    }

    @Autowired
    public void setStopChangeHandler(StopChangeHandler stopChangeHandler) {
        _stopChangeHandler = stopChangeHandler;
    }

    @Autowired
    public void setTripChangeHandler(TripChangeHandler tripChangeHandler) {
        _tripChangeHandler = tripChangeHandler;
    }

    @Override
    public int handleServiceChanges(Collection<ServiceChange> serviceChanges) {
        _isApplying = true;

        revertPreviousChanges();

        /*
        Currently handled:
         * adding shapes (do this first so trips can use the new shapes)
         * trip modifications:
         *   inserted stop times
         *   deleted stop times
         *   altered stop times
         *   shape ID change
         */
        List<ServiceChange> activeChanges = filterServiceChanges(serviceChanges);

        // The ordering is tricky. Because we need to use StopEntryImpl to look up stops for trip,
        // we cannot apply StopChanges until we create TripChanges.
        // TripChanges also need to be applied AFTER shape and stop changes so that distances along
        // trip are calculated correctly.

        ShapeChangeSet shapesChanges = _shapeChangeHandler.getAllShapeChanges(activeChanges);
        StopChangeSet stopChanges = _stopChangeHandler.getAllStopChanges(activeChanges);
        TripChangeSet tripChanges = _tripChangeHandler.getAllTripChanges(activeChanges);

        // Supecedes date applicability check for shapes. We only handle ADDED shapes anyhow. Remove shapes
        // which don't refer to trips.
        _shapeChangeHandler.filterShapeChanges(shapesChanges, tripChanges);

        revertShapeChanges = _shapeChangeHandler.handleShapeChanges(shapesChanges);
        revertStopChanges = _stopChangeHandler.handleStopChanges(stopChanges);
        revertTripChanges = _tripChangeHandler.handleTripChanges(tripChanges);

        int nSuccess = revertShapeChanges.size() + revertStopChanges.size() + revertTripChanges.size();
        if (nSuccess > 0) {
            forceFlush();
        }
        _isApplying = false;
        return nSuccess;
    }

    @Override
    public boolean handleServiceChange(ServiceChange change) {
        return handleServiceChanges(Collections.singleton(change)) > 0;
    }

    @Override
    public boolean isApplying() {
        return _isApplying;
    }

    List<ServiceChange> filterServiceChanges(Collection<ServiceChange> changes) {
       return changes.stream().filter(this::isServiceChangeOk).collect(Collectors.toList());
    }

    boolean isServiceChangeOk(ServiceChange change) {
        if (!validateServiceChange(change)) {
            _log.debug("service change is invalid");
            return false;
        }
        return true;
    }

    void revertPreviousChanges() {
        if (revertShapeChanges != null) {
            int success = _shapeChangeHandler.handleShapeChanges(revertShapeChanges).size();
            if (success != revertShapeChanges.size()) {
                _log.error("Error reverting some shapes!");
            }
        }
        if (revertStopChanges != null) {
            int success = _stopChangeHandler.handleStopChanges(revertStopChanges).size();
            if (success != revertStopChanges.size()) {
                _log.error("Error reverting some stops!");
            }
        }
        if (revertTripChanges != null) {
            int success = _tripChangeHandler.handleTripChanges(revertTripChanges).size();
            if (success != revertTripChanges.size()) {
                _log.error("Error reverting some trips!");
            }
        }
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

    private boolean validateServiceChange(ServiceChange change) {
        if (change.getAffectedDates().isEmpty()) {
            _log.info("affected dates is empty");
            return false;
        }
        switch(change.getServiceChangeType()) {
            case ADD:
                return change.getAffectedEntity().isEmpty() && !change.getAffectedField().isEmpty();
            case ALTER:
                return !change.getAffectedEntity().isEmpty() && change.getAffectedField().size() == 1;
            case DELETE:
                return !change.getAffectedEntity().isEmpty() && change.getAffectedField().isEmpty();
        }
        return false;
    }
}

