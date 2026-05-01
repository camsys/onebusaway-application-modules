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

import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTripsResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripModificationCreationService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripModificationUpdateService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class TripModsTripModificationUpdateServiceImpl implements TripModsTripModificationUpdateService {

    private static final Logger _log = LoggerFactory.getLogger(TripModsTripModificationUpdateServiceImpl.class);

    private final TransitGraphDao _dao;

    private final TripModsTripModificationCreationService _tripModsCreationService;

    @Autowired
    public TripModsTripModificationUpdateServiceImpl(TransitGraphDao dao,
                                                     TripModsTripModificationCreationService tripModsCreationService) {
        _dao = dao;
        _tripModsCreationService = tripModsCreationService;
    }

    @Override
    public ModifiedTripsResult updateTrips(List<ModifiedTrip> modifiedTripList) {
        ModifiedTripsResult result = new ModifiedTripsResult();
        for (ModifiedTrip modifiedTrip : modifiedTripList) {
            _log.info("Handling changes for trip {}", modifiedTrip.getTripId());
            ModifiedTrip originalTrip = _tripModsCreationService.createModifiedTripForExistingTrip(modifiedTrip.getTripId());
            if (originalTrip != null && _dao.updateStopTimesForTrip(modifiedTrip.getTripEntry(), modifiedTrip.getModifiedStopTimes().getUpdatedStopTimes(),
                    modifiedTrip.getShapeId())) {
                result.addOriginalTrip(originalTrip);
                result.addSuccessfullyUpdatedTripId(modifiedTrip.getTripId());
            } else {
                _log.info("Unable to apply changes for trip {}", modifiedTrip.getTripId());
                result.addFailedUpdatedTripId(modifiedTrip.getTripId());
            }
        }
        return result;
    }
}
