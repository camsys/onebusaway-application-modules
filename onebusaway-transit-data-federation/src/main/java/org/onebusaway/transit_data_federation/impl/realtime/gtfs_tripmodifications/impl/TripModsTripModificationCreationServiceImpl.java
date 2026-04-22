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

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedStopTimes;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffCache;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffComputer;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.StopEntryData;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsStopTimeEntryFactory;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsStopTimeFetcher;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTimeService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrips;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripModificationCreationService;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
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
import java.util.stream.IntStream;


@Component
public class TripModsTripModificationCreationServiceImpl implements TripModsTripModificationCreationService {

    private static final Logger _log = LoggerFactory.getLogger(TripModsTripModificationCreationServiceImpl.class);

    private static final int DEFAULT_UPDATED_GTFS_STOP_SEQUENCE = -999;

    private static final double DEFAULT_UPDATED_SHAPE_DIST_TRAVELED = -999;

    private static final String NULL_ENTITY_ID = null;

    private final TransitGraphDao _dao;

    private final EntityIdService _entityIdService;

    private final NarrativeService _narrativeService;

    private final TripModsStopTimeFetcher _stopTimeFetcher;

    private final TripModsStopTimeEntryFactory  _stopTimeEntryFactory;

    private final GtfsTripModificationsUtil _util;

    private final TripModsTimeService _timeService;

    private final TripModificationDiffComputer _tripModificationDiffComputer;

    private final TripModificationDiffCache _diffCache;


    @Autowired
    public TripModsTripModificationCreationServiceImpl(TransitGraphDao dao,
                                                       EntityIdService entityIdService,
                                                       NarrativeService narrativeService,
                                                       TripModsStopTimeFetcher stopTimeFetcher,
                                                       TripModsStopTimeEntryFactory  stopTimeEntryFactory,
                                                       GtfsTripModificationsUtil gtfsTripModificationsUtil,
                                                       TripModsTimeService timeService,
                                                       TripModificationDiffComputer tripModificationDiffComputer,
                                                       TripModificationDiffCache tripModificationDiffCache) {
        _dao = dao;
        _entityIdService = entityIdService;
        _narrativeService = narrativeService;
        _stopTimeFetcher = stopTimeFetcher;
        _stopTimeEntryFactory = stopTimeEntryFactory;
        _util = gtfsTripModificationsUtil;
        _timeService = timeService;
        _tripModificationDiffComputer = tripModificationDiffComputer;
        _diffCache = tripModificationDiffCache;

    }

    @Override
    public ModifiedTrips createModifiedTrips(Map<String, GtfsRealtime.TripModifications> tripModificationsMap) {
        ModifiedTrips modifiedTrips = new ModifiedTrips();

        Map<String, GtfsRealtime.TripModifications> filteredTripModifications = filterTripModifications(tripModificationsMap);

        for (Map.Entry<String,GtfsRealtime.TripModifications> tripModificationEntry : filteredTripModifications.entrySet()) {
            String entityId = tripModificationEntry.getKey();
            GtfsRealtime.TripModifications tripModifications = tripModificationEntry.getValue();

            List<GtfsRealtime.TripModifications.Modification> modifications = tripModifications.getModificationsList();
            List<GtfsRealtime.TripModifications.SelectedTrips> selectedTripsList = tripModifications.getSelectedTripsList();
            Set<LocalDate> selectedTripServiceDates = _util.parseServiceDates(tripModifications.getServiceDatesList());


            for (GtfsRealtime.TripModifications.SelectedTrips selectedTrips : selectedTripsList) {
                List<ModifiedTrip> modifiedTripList = createModifiedTripsForSelectedTrips(selectedTrips,
                        selectedTripServiceDates, modifications, entityId);

                modifiedTrips.addAllModifiedTrip(modifiedTripList);
                Set<String> failedModifiedTripIds = getFailedModifiedTripIds(selectedTrips, modifiedTrips);
                modifiedTrips.addAllFailedModifiedTripIds(failedModifiedTripIds);
            }
        }

        return modifiedTrips;
    }

    private Set<String> getFailedModifiedTripIds(GtfsRealtime.TripModifications.SelectedTrips selectedTrips,
                                                  ModifiedTrips modifiedTrips) {
        List<String> allTripIds = selectedTrips.getTripIdsList();
        Set<String> successfullyModifiedTripIds = modifiedTrips.getSuccessfullyModifiedTripIds();
        return allTripIds.stream()
                .filter(item -> !successfullyModifiedTripIds.contains(item))
                .collect(Collectors.toSet());
    }

    private List<ModifiedTrip> createModifiedTripsForSelectedTrips(GtfsRealtime.TripModifications.SelectedTrips selectedTrips,
                                                                   Set<LocalDate> selectedTripServiceDates,
                                                                   List<GtfsRealtime.TripModifications.Modification> modifications,
                                                                   String entityId) {
        List<ModifiedTrip> modifiedTrips = new ArrayList<>();
        AgencyAndId shapeId = getSelectedTripsShapeId(selectedTrips);
        for(String tripId: selectedTrips.getTripIdsList()){
            TripEntryImpl tripEntry = convertSelectedTripToTripEntry(tripId);
            if(tripEntry == null) {
                continue;
            }
            ModifiedTrip modifiedTrip = createModifiedTrip(tripEntry, shapeId, modifications, entityId);
            if(isValidTripModifiedTrip(modifiedTrip, selectedTripServiceDates)){
                modifiedTrips.add(modifiedTrip);
            }
        }
        return modifiedTrips;
    }

    private boolean isValidTripModifiedTrip(ModifiedTrip modifiedTrip, Set<LocalDate> selectedTripServiceDates) {
        if(modifiedTrip == null) {
            _log.warn("Unable to create ModifiedTrip. ModifiedTrip is null.");
            return false;
        }
        if(modifiedTrip.getServiceDate() == null) {
            _log.warn("Unable to create ModifiedTrip. No service date for tripId {}.", modifiedTrip.getTripId());
            return false;
        }
        if(modifiedTrip.getModifiedStopTimes() == null || modifiedTrip.getModifiedStopTimes().getUpdatedStopTimes().isEmpty()) {
            _log.warn("Unable to create ModifiedTrip. No stop times for tripId {}.", modifiedTrip.getTripId());
            return false;
        }
        if(!selectedTripServiceDates.contains(modifiedTrip.getServiceDate())) {
            _log.warn("Unable to create ModifiedTrip. Trip {} does not have an active service date {}.", modifiedTrip.getTripId(), modifiedTrip.getServiceDate());
            return false;
        }
        return true;
    }

    private TripEntryImpl convertSelectedTripToTripEntry(String tripId) {
        AgencyAndId agencyAndTripId = _entityIdService.getTripId(tripId);
        return getTripEntry(agencyAndTripId);
    }


    Map<String, GtfsRealtime.TripModifications> filterTripModifications(Map<String, GtfsRealtime.TripModifications> tripModificationsMap) {
        return tripModificationsMap.entrySet().stream()
                .filter(entry -> isValidTripModification(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    boolean isValidTripModification(GtfsRealtime.TripModifications tripModifications) {
        if (!isDateApplicable(tripModifications)) {
            _log.debug("service change is invalid");
            return false;
        }
        return true;
    }

    boolean isDateApplicable(GtfsRealtime.TripModifications tripModifications) {
        if (tripModifications.getServiceDatesList().isEmpty()) {
            _log.info("affected dates is empty");
            //TODO change this to false- keeping it as true for now to allow testing with existing data
            return false;
        }
        return true;
    }

    AgencyAndId getSelectedTripsShapeId(GtfsRealtime.TripModifications.SelectedTrips selectedTrips) {
        String shapeId = selectedTrips.hasShapeId() ? selectedTrips.getShapeId() : null;
        return shapeId != null ? _entityIdService.getShapeId(shapeId) : null;
    }

    /**
     *
     * @param tripEntry
     * @param agencyAndShapeId
     * @param modifications
     * @return ModifiedTrip
     */
    ModifiedTrip createModifiedTrip(TripEntryImpl tripEntry,
                                    AgencyAndId agencyAndShapeId,
                                    List<GtfsRealtime.TripModifications.Modification> modifications,
                                    String entityId) {

        ModifiedStopTimes modifiedStopTimes = getModifiedStopTimeEntries(tripEntry, modifications);
        agencyAndShapeId = getModifiedShapeId(tripEntry, agencyAndShapeId);
        LocalDate serviceDate = _util.getActiveServiceDateForTrip(tripEntry);

        return new ModifiedTrip(
                entityId,
                tripEntry.getId(),
                agencyAndShapeId,
                modifiedStopTimes,
                tripEntry,
                modifications,
                serviceDate);

    }

    AgencyAndId getModifiedShapeId(TripEntryImpl tripEntry, AgencyAndId agencyAndShapeId) {
        if (agencyAndShapeId == null) {
            return tripEntry.getShapeId();
        }
        return agencyAndShapeId;
    }

    TripEntryImpl getTripEntry(AgencyAndId tripId) {
        TripEntry tripEntry =  _dao.getTripEntryForId(tripId);
        if (tripEntry == null) {
            return null;
        }
        return (TripEntryImpl) tripEntry;
    }

    ModifiedStopTimes getModifiedStopTimeEntries(TripEntryImpl originalTrip,
                                                 List<GtfsRealtime.TripModifications.Modification> modifications) {
        ModifiedStopTimes modifiedStopTimes = new ModifiedStopTimes();
        List<StopTimeEntry> originalStopTimes = originalTrip.getStopTimes();

        refreshStopTimes(originalStopTimes);

        for (GtfsRealtime.TripModifications.Modification mod : modifications) {
            int startSelectorStopTimesIndex = _util.findStopTimeIndexForSelector(originalStopTimes, mod.getStartStopSelector());
            int endSelectorStopTimesIndex = _util.findStopTimeIndexForSelector(originalStopTimes, mod.getEndStopSelector());
            if (startSelectorStopTimesIndex == -1 || endSelectorStopTimesIndex == -1) {
                _log.warn("Skipping modification due to unresolved stop selector: start={}, end={}",
                        mod.getStartStopSelector(), mod.getEndStopSelector());
                continue;
            }
            int postModificationStopTimesIndex = endSelectorStopTimesIndex + 1;


            List<GtfsRealtime.ReplacementStop> replacementStops = mod.getReplacementStopsList();
            int propagatedModificationDelay =  mod.getPropagatedModificationDelay();

            // Stop Times to be replaced
            List<Integer> oldReplacedStopTimeIndices =  getAllStopTimeIndicesToBeReplaced(startSelectorStopTimesIndex,
                    endSelectorStopTimesIndex);
            modifiedStopTimes.addOriginalRemovedStopTimeIndices(oldReplacedStopTimeIndices);

            // New Stop Times
            List<StopTimeEntry> preReplacementStopTimes = getAllStopTimesBeforeSelection(startSelectorStopTimesIndex, originalStopTimes);
            modifiedStopTimes.addUpdatedStopTimes(preReplacementStopTimes);

            int lastKnownStopSequence = getLastKnownStopSequence(modifiedStopTimes.getUpdatedStopTimes());
            Map<Integer, StopTimeEntry> newReplacementStopTimes =  getAllNewReplacementStopTimes(
                    startSelectorStopTimesIndex, lastKnownStopSequence, originalStopTimes, replacementStops, originalTrip);
            modifiedStopTimes.addUpdatedStopTimes(newReplacementStopTimes.values());

            int stopSequenceOffset = replacementStops.size() - (postModificationStopTimesIndex - startSelectorStopTimesIndex);

            if(startSelectorStopTimesIndex != 19) {
                System.out.println("test");
            }

            List<StopTimeEntry> postReplacementStopTimes = getAllStopTimesAfterSelection(postModificationStopTimesIndex,
                    stopSequenceOffset, originalStopTimes, propagatedModificationDelay);

            modifiedStopTimes.addUpdatedStopTimes(postReplacementStopTimes);


            modifiedStopTimes.addModifiedAddedStopTimeIndices(newReplacementStopTimes.keySet());
        }

        return modifiedStopTimes;

    }

    private int getInitialPostReplacementStopSequence(int initialAddedStopSequence, int replacementStopsSize) {
        if(replacementStopsSize == 0){
            return initialAddedStopSequence;
        }
        return initialAddedStopSequence + replacementStopsSize;
    }

    private int getLastKnownStopSequence(List<StopTimeEntry> stopTimes) {
        if(stopTimes.isEmpty()) {
            return 0;
        }
        return stopTimes.get(stopTimes.size()-1).getGtfsSequence();
    }


    private void refreshStopTimes(List<StopTimeEntry> originalStopTimes) {
        for (StopTimeEntry stopTimeEntry : originalStopTimes) {
            StopEntryImpl stopEntry = (StopEntryImpl) _dao.getStopEntryForId(stopTimeEntry.getStop().getId());
            ((StopTimeEntryImpl) stopTimeEntry).setStop(stopEntry);
        }
    }

    List<StopTimeEntry> getAllStopTimesBeforeSelection(int startSelectorStopTimesIndex,
                                                               List<StopTimeEntry> originalStopTimes) {
        return originalStopTimes.subList(0, startSelectorStopTimesIndex);
    }

    private List<Integer> getAllStopTimeIndicesToBeReplaced(int startSelectorStopTimesIndex, int endSelectorStopTimesIndex) {
        return IntStream.rangeClosed(startSelectorStopTimesIndex, endSelectorStopTimesIndex)
                    .boxed()
                    .collect(Collectors.toList());
    }

    Map<Integer, StopTimeEntry> getAllNewReplacementStopTimes(int startSelectorStopTimesIndex,
                                                              int lastKnownStopSequence,
                                                              List<StopTimeEntry> originalStopTimes,
                                                              List<GtfsRealtime.ReplacementStop> replacementStops,
                                                              TripEntryImpl originalTrip) {

        Map<Integer,StopTimeEntry> modifiedStopTimes = new LinkedHashMap<>();
        int currentIndex = startSelectorStopTimesIndex;
        int referenceTime = _util.getReferenceTime(originalStopTimes, startSelectorStopTimesIndex);

        for (var replacementStop : replacementStops) {
            lastKnownStopSequence++;
            double shapeDistanceTraveled = DEFAULT_UPDATED_SHAPE_DIST_TRAVELED;

            StopTimeEntry newStopTime = createStopTimeEntry(
                    replacementStop,
                    referenceTime,
                    lastKnownStopSequence,
                    shapeDistanceTraveled,
                    originalTrip
            );
            modifiedStopTimes.put(currentIndex, newStopTime);
            currentIndex++;
        }
        return modifiedStopTimes;
    }

    List<StopTimeEntry> getAllStopTimesAfterSelection(int postModificationStopTimesIndex,
                                                      int stopSequenceOffset,
                                                      List<StopTimeEntry> originalStopTimes,
                                                      int propagatedModificationDelay) {

        List<StopTimeEntry> modifiedStopTimes = new ArrayList<>();

        for (int i = postModificationStopTimesIndex; i < originalStopTimes.size(); i++) {
            StopTimeEntry originalStopTime = originalStopTimes.get(i);
            int updatedGtfsStopSequence = originalStopTime.getGtfsSequence() + stopSequenceOffset;
            int updatedStopSequence = -999;//originalStopTime.getSequence() + stopSequenceOffset;

            StopTimeEntry adjusted = adjustStopTime(
                    originalStopTime,
                    updatedGtfsStopSequence,
                    updatedStopSequence,
                    propagatedModificationDelay
            );
            modifiedStopTimes.add(adjusted);
        }

        return modifiedStopTimes;
    }


    StopTimeEntry createStopTimeEntry(GtfsRealtime.ReplacementStop replacementStop,
                                      int referenceTime,
                                      int gtfsStopSequence,
                                      double shapeDistanceTraveled,
                                      TripEntryImpl tripEntry) throws IllegalStateException {

        StopEntryData stopEntryData = _stopTimeFetcher.getStopEntry(replacementStop.getStopId());
        int arrivalTime = _util.calculateReplacementStopArrivalTime(replacementStop, referenceTime);
        return _stopTimeEntryFactory.create(stopEntryData, tripEntry, arrivalTime,
                                            gtfsStopSequence, shapeDistanceTraveled);

    }


    StopTimeEntry adjustStopTime(StopTimeEntry original,
                                 int newGtfsSequence,
                                 int newStopSequence,
                                 int propagatedDelay) {
        StopTimeEntryImpl adjusted = new StopTimeEntryImpl(original);
        adjusted.setGtfsSequence(newGtfsSequence);
        adjusted.setSequence(newStopSequence);
        adjusted.setArrivalTime(original.getArrivalTime() + propagatedDelay);
        adjusted.setDepartureTime(original.getDepartureTime() + propagatedDelay);
        return adjusted;
    }

    @Override
    public ModifiedTrip createModifiedTripForExistingTrip(AgencyAndId tripId) {
        TripEntry transitGraphTripEntry = _dao.getTripEntryForId(tripId);

        if(transitGraphTripEntry != null){
            TripEntryImpl tripEntry = (TripEntryImpl) transitGraphTripEntry;

            List<GtfsRealtime.TripModifications.Modification> modifications = Collections.emptyList();
            ModifiedStopTimes modifiedStopTimes = new ModifiedStopTimes();
            modifiedStopTimes.addUpdatedStopTimes(tripEntry.getStopTimes());

            return new ModifiedTrip(
                    NULL_ENTITY_ID,
                    tripId,
                    tripEntry.getShapeId(),
                    modifiedStopTimes,
                    tripEntry,
                    modifications,
                    _util.getActiveServiceDateForTrip(tripEntry));
        } else {
            return null;
        }



    }


}
