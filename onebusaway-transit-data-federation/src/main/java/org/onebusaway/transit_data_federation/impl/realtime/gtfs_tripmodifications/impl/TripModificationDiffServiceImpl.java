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
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedStopTimes;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrips;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffComputer;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffCache;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TripModificationDiffServiceImpl implements TripModificationDiffService {

    private static final Logger _log = LoggerFactory.getLogger(TripModificationDiffServiceImpl.class);

    @Autowired
    private TripModificationDiffCache _diffCache;

    private final TransitGraphDao _dao;

    private final TripModificationDiffComputer _tripModificationDiffComputer;

    @Autowired
    public TripModificationDiffServiceImpl(TransitGraphDao dao, TripModificationDiffComputer tripModificationDiffComputer) {
        _dao = dao;
        _tripModificationDiffComputer = tripModificationDiffComputer;
    }

    @Override
    public Collection<TripModificationDiff> getAllTripModificationDiffs() {
        return _diffCache.getAll();
    }

    @Override
    public Collection<TripModificationDiff> getAllTripModificationDiffs(LocalDate serviceDate) {
        return _diffCache.getAll().stream()
                .filter(tripDiff -> matchesServiceDate(tripDiff, serviceDate))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TripModificationDiff> getTripModificationDiffs(AgencyAndId tripId) {
        return Optional.ofNullable(_diffCache.get(tripId));
    }

    @Override
    public Optional<TripModificationDiff> getTripModificationDiffs(AgencyAndId tripId, LocalDate serviceDate) {
        Optional<TripModificationDiff> tripModificationDiff = Optional.ofNullable(_diffCache.get(tripId));
        return tripModificationDiff.filter(tripDiff -> matchesServiceDate(tripDiff, serviceDate));
    }

    @Override
    public Map<AgencyAndId, TripModificationDiff> getAllTripModificationDiffsById() {
        return _diffCache.getAllById();
    }

    @Override
    public Map<AgencyAndId, TripModificationDiff> getAllTripModificationDiffsById(LocalDate serviceDate) {
        return _diffCache.getAllById().entrySet().stream()
                .filter(entry -> matchesServiceDate(entry.getValue(), serviceDate))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Collection<TripModificationDiff> createDiffsFromModifications(ModifiedTrips modifiedTrips) {

        Map<AgencyAndId, TripModificationDiff> newCache = new HashMap<>();

        for (ModifiedTrip modifiedTrip : modifiedTrips.getModifiedTrips()) {
            TripEntry tripEntry = modifiedTrip.getTripEntry();

            String entityId = modifiedTrip.getEntityId();
            AgencyAndId tripId = modifiedTrip.getTripEntry().getId();
            LocalDate effectiveServiceDate = modifiedTrip.getServiceDate();
            ModifiedStopTimes modifiedStopTimes = modifiedTrip.getModifiedStopTimes();

            // Original StopTimes
            AgencyAndId originalShapeId = tripEntry.getShapeId();
            List<StopTimeEntry> originalTripStopTimes = tripEntry.getStopTimes();
            ShapePoints originalTripShape = _dao.getShape(originalShapeId);
            Set<Integer> originalTripRemovedStopIndices = modifiedStopTimes.getOriginalRemovedStopTimeIndices();

            // Updated StopTimes
            List<StopTimeEntry> modifiedTripStopTimesList = modifiedStopTimes.getUpdatedStopTimes();
            AgencyAndId modifiedTripShapeId = modifiedTrip.getShapeId();
            Set<Integer> modifiedTripAddedStopIndices = modifiedStopTimes.getModifiedAddedStopTimeIndices();


            Optional<TripModificationDiff> diff = _tripModificationDiffComputer.computeDiff(
                    entityId,
                    tripId,
                    originalTripStopTimes,
                    originalTripShape,
                    originalTripRemovedStopIndices,
                    modifiedTripStopTimesList,
                    modifiedTripShapeId,
                    modifiedTripAddedStopIndices,
                    effectiveServiceDate
            );

            if (diff.isEmpty()) {
                _log.warn("Unable to compute TripModificationDiff for tripId {}. Skipping caching of diff.", tripEntry.getId());
                continue;
            }
            newCache.put(tripEntry.getId(), diff.get());
        }

        _diffCache.replaceAll(newCache);

        return _diffCache.getAll();
    }


    private boolean matchesServiceDate(TripModificationDiff tripDiff,
                                       LocalDate serviceDate) {
        return serviceDate == null || (tripDiff.getEffectiveServiceDate() != null &&
                serviceDate.equals(tripDiff.getEffectiveServiceDate()));
    }
}