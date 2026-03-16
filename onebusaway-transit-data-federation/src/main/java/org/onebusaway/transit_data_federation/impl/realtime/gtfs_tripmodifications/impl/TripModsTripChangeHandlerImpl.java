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

import com.google.transit.realtime.GtfsRealtime.ReplacementStop;
import com.google.transit.realtime.GtfsRealtime.TripModifications;
import com.google.transit.realtime.GtfsRealtime.StopSelector;
import com.google.transit.realtime.GtfsRealtime.TripModifications.SelectedTrips;
import com.google.transit.realtime.GtfsRealtime.TripModifications.Modification;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.*;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffComputer;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripChangeHandler;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl.GtfsTripModsServiceChangeLibrary.*;

@Component
public class TripModsTripChangeHandlerImpl implements TripModsTripChangeHandler {

    private static final Logger _log = LoggerFactory.getLogger(org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl.TripChangeHandlerImpl.class);

    private TransitGraphDao _dao;

    private EntityIdService _entityIdService;

    private NarrativeService _narrativeService;

    private TimeService _timeService;

    private BlockCalendarService _blockCalendarService;

    private TripModificationDiffComputer _tripModificationDiffComputer;

    private TripModificationDiffCacheImpl _diffCache;

    private DateTimeFormatter SERVICE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    //TODO what is the service date format

    @Autowired
    public void setDiffCache(TripModificationDiffCacheImpl diffCache) {
        _diffCache = diffCache;
    }

    @Autowired
    public void setTripModificationDiffComputer(TripModificationDiffComputer tripModificationDiffComputer) {
        _tripModificationDiffComputer = tripModificationDiffComputer;
    }

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Autowired
    public void setEntityIdService(EntityIdService entityIdService) {
        _entityIdService = entityIdService;
    }

    @Autowired
    public void setNarrativeService(NarrativeService narrativeService) {
        _narrativeService = narrativeService;
    }

    @Autowired
    public void setTimeService(TimeService timeService) {
        _timeService = timeService;
    }

    @Autowired
    public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
        _blockCalendarService = blockCalendarService;
    }

    @Override
    public TripChangeSet getAllTripChanges(TripModifications tripModifications) {
        TripChangeSet changeSet = new TripChangeSet();

        List<LocalDate> serviceDates = parseServiceDates(tripModifications.getServiceDatesList());
        List<Modification> modifications = tripModifications.getModificationsList();

        for (SelectedTrips selectedTrips : tripModifications.getSelectedTripsList()) {
            String shapeId = selectedTrips.hasShapeId() ? selectedTrips.getShapeId() : null;

            for (String tripId : selectedTrips.getTripIdsList()) {
                AgencyAndId agencyTripId = _entityIdService.getTripId(tripId);
                AgencyAndId agencyShapeId = _entityIdService.getShapeId(shapeId);

                for (LocalDate serviceDate : serviceDates) {
                    try {
                        TripChange change = createModifyTrip(
                                agencyTripId,
                                agencyShapeId,
                                serviceDate,
                                modifications
                        );

                        changeSet.addModifiedTrip((ModifyTrip) change);
                    } catch (IllegalArgumentException e) {
                        _log.warn("Unable to create ModifyTrip for tripId {} on serviceDate {}: {}",
                                agencyTripId, serviceDate, e.getMessage());
                    }

                }
            }
        }

        return changeSet;
    }

    private ModifyTrip createModifyTrip(AgencyAndId tripId,
                                        AgencyAndId shapeId,
                                        LocalDate serviceDate,
                                        List<Modification> modifications) {
        ModifyTrip modifyTrip = new ModifyTrip();
        modifyTrip.setTripId(tripId);
        modifyTrip.setShapeId(shapeId);
        modifyTrip.setServiceDate(serviceDate);

        TripEntryImpl tripEntry = (TripEntryImpl) _dao.getTripEntryForId(tripId);
        if (tripEntry == null) {
            throw new IllegalArgumentException("Trip entry not found for id: " + tripId);
        }
        modifyTrip.setTripEntry(tripEntry);

        List<StopTimeEntry> modifiedStopTimes = buildModifiedStopTimes(tripEntry, modifications);
        modifyTrip.setStopTimes(modifiedStopTimes);

        if (tripEntry.getStopTimes() == null || tripEntry.getStopTimes().isEmpty()) {
            _log.warn("No original stop times found for trip {}, skipping diff", tripId);
            return modifyTrip;
        }

        if (shapeId != null) {
            ShapePoints newShape = _dao.getShape(shapeId);
            if (newShape == null) {
                throw new IllegalArgumentException("Shape entry not found for id: " + shapeId);
            }
        }

        ShapePoints oldShape = _dao.getShape(tripEntry.getShapeId());

        TripModificationDiff diff = _tripModificationDiffComputer.computeDiff(
                tripEntry.getId(),
                tripEntry.getStopTimes(),
                modifiedStopTimes,
                oldShape,
                shapeId,
                Collections.singletonList(serviceDate)
        );
        _diffCache.invalidateAndReplace(tripId, diff);


        return modifyTrip;
    }

    private List<StopTimeEntry> buildModifiedStopTimes(TripEntryImpl originalTrip,
                                                       List<Modification> modifications) {
        List<StopTimeEntry> result = new ArrayList<>();
        List<StopTimeEntry> originalStopTimes = new ArrayList<>(originalTrip.getStopTimes());

        // Ensure stops are fresh
        for (StopTimeEntry stopTimeEntry : originalStopTimes) {
            StopEntryImpl stopEntry = (StopEntryImpl) _dao.getStopEntryForId(stopTimeEntry.getStop().getId());
            ((StopTimeEntryImpl) stopTimeEntry).setStop(stopEntry);
        }

        for (Modification mod : modifications) {
            int startIndex = findStopIndex(originalStopTimes, mod.getStartStopSelector());
            int endIndex = findStopIndex(originalStopTimes, mod.getEndStopSelector());

            for (int i = 0; i < startIndex; i++) {
                result.add(originalStopTimes.get(i));
            }

            int referenceTime = getReferenceTime(originalStopTimes, startIndex);
            for (var replacementStop : mod.getReplacementStopsList()) {
                StopTimeEntry newStopTime = createStopTimeEntry(
                        replacementStop,
                        referenceTime,
                        result.size() + 1,
                        originalTrip
                );
                result.add(newStopTime);
            }

            //end index is inclusive, so we need to add 1 to it to get the correct stop sequence for the next stop time
            for (int i = endIndex + 1 + 1; i < originalStopTimes.size(); i++) {
                StopTimeEntry adjusted = adjustStopTime(
                        originalStopTimes.get(i),
                        result.size() + 1,
                        mod.getPropagatedModificationDelay()
                );
                result.add(adjusted);
            }
        }

        return result;
    }

    private int getReferenceTime(List<StopTimeEntry> stopTimes, int startIndex) {
        if (startIndex > 0) {
            return stopTimes.get(startIndex - 1).getArrivalTime();
        }
        return stopTimes.get(0).getArrivalTime();
    }

    private StopTimeEntry createStopTimeEntry(ReplacementStop replacementStop,
                                              int referenceTime,
                                              int stopSequence,
                                              TripEntryImpl tripEntry) {
        AgencyAndId stopId = _entityIdService.getStopId(replacementStop.getStopId());


        StopEntry se = _dao.getStopEntryForId(stopId);
        if (se == null) {
            throw new IllegalArgumentException("Stop entry not found for id: " + stopId.toString());
        }
        StopNarrative sn = _narrativeService.getStopForId(se.getId());
        if (sn == null) {
            throw new IllegalArgumentException("Stop narrative not found for id: " + stopId.toString());
        }

        Double lat = se.getStopLat();
        Double lon = se.getStopLon();
        String stopName = _narrativeService.getStopForId(se.getId()).getName();

        // Calculate arrival time
        int arrivalTime = referenceTime;
        if (replacementStop.hasTravelTimeToStop()) {
            arrivalTime = referenceTime + replacementStop.getTravelTimeToStop();
        }

        StopTimeEntryImpl stopTimeEntry = new StopTimeEntryImpl();
        stopTimeEntry.setGtfsSequence(stopSequence + 1); // GTFS stop sequence is 1-based
        stopTimeEntry.setSequence(stopSequence);
        stopTimeEntry.setArrivalTime(arrivalTime);
        stopTimeEntry.setDepartureTime(arrivalTime);  // departure = arrival per spec
        StopEntryImpl stopEntry = new StopEntryImpl(stopId,lat,lon);
        stopTimeEntry.setStop(stopEntry);
        stopTimeEntry.setTrip(tripEntry);

        return stopTimeEntry;
    }

    private int findStopIndex(List<StopTimeEntry> stopTimes, StopSelector selector) {
        if (selector.hasStopSequence()) {
            int seq = selector.getStopSequence();
            for (int i = 0; i < stopTimes.size(); i++) {
                if (stopTimes.get(i).getSequence() == seq) {
                    return i;
                }
            }
        } else if (selector.hasStopId()) {
            String stopId = selector.getStopId();
            for (int i = 0; i < stopTimes.size(); i++) {
                if (stopTimes.get(i).getStop().getId().getId().equals(stopId)) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Stop not found for selector: " + selector);
    }

    private StopTimeEntry adjustStopTime(StopTimeEntry original,
                                         int newSequence,
                                         int propagatedDelay) {
        StopTimeEntryImpl adjusted = new StopTimeEntryImpl(original);
        adjusted.setSequence(newSequence);
        adjusted.setArrivalTime(original.getArrivalTime() + propagatedDelay);
        adjusted.setDepartureTime(original.getDepartureTime() + propagatedDelay);
        return adjusted;
    }

    private List<LocalDate> parseServiceDates(List<String> serviceDateStrings) {
        return serviceDateStrings.stream()
                .map(s -> LocalDate.parse(s, SERVICE_DATE_FORMAT))
                .collect(Collectors.toList());
    }

    @Override
    public TripChangeSet applyChanges(TripChangeSet changeset) {
        TripChangeSet revertSet = new TripChangeSet();
        for (AddTrip addTrip : changeset.getAddedTrips()) {
            _log.info("Handling changes for trip {}", addTrip.getTripId());
            if (_dao.addTripEntry(addTrip.getTripEntry(), addTrip.getTripNarrative())) {
                DeleteTrip deleteTrip = new DeleteTrip(addTrip.getTripId(), addTrip.getServiceDate(), addTrip.getEndTime());
                revertSet.addDeletedTrip(deleteTrip);
            } else {
                _log.info("Unable to apply changes for trip {}", addTrip.getTripId());
            }
        }
        for (ModifyTrip modifyTrip : changeset.getModifiedTrips()) {
            _log.info("Handling changes for trip {}", modifyTrip.getTripId());
            ModifyTrip revertTrip = getModifyTripForExistingTrip(modifyTrip.getTripId());
            if (_dao.updateStopTimesForTrip(modifyTrip.getTripEntry(), modifyTrip.getStopTimes(), modifyTrip.getShapeId())) {
                revertSet.addModifiedTrip(revertTrip);
            } else {
                _log.info("Unable to apply changes for trip {}", modifyTrip.getTripId());
            }
        }
        return revertSet;
    }

    private ModifyTrip getModifyTripForExistingTrip(AgencyAndId tripId) {
        TripEntryImpl tripEntry = (TripEntryImpl) _dao.getTripEntryForId(tripId);
        ModifyTrip modify = new ModifyTrip();
        modify.setTripId(tripId);
        modify.setShapeId(tripEntry.getShapeId());
        modify.setStopTimes(tripEntry.getStopTimes());
        modify.setTripEntry(tripEntry);
        modify.setServiceDate(getServiceDateForTrip(tripEntry));
        return modify;
    }

    // Note: If current time is 3pm, and a block ends at 1pm: it's next service date is tomorrow.
    // This has implications for block consistency.
    private LocalDate getServiceDateForTrip(TripEntry trip) {
        long now = _timeService.getCurrentTimeAsEpochMs();
        List<BlockInstance> blocks = _blockCalendarService.getActiveBlocks(trip.getBlock().getId(), now, now + (24 * 3600 * 1000));
        if (blocks.isEmpty())
            return null;
        // Get BlockInstance which is active with minimum service date
        BlockInstance block = Collections.min(blocks, Comparator.comparingLong(BlockInstance::getServiceDate));
        return toLocalDate(block.getServiceDate(), _timeService.getTimeZone());
    }
}