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
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTimeService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrips;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripModificationCreationService;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
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
public class TripModsTripModificationCreationServiceImpl implements TripModsTripModificationCreationService {

    private static final Logger _log = LoggerFactory.getLogger(TripModsTripModificationCreationServiceImpl.class);

    private static final int DEFAULT_UPDATED_GTFS_STOP_SEQUENCE = -999;

    private static final double DEFAULT_UPDATED_SHAPE_DIST_TRAVELED = -999;

    private final TransitGraphDao _dao;

    private final EntityIdService _entityIdService;

    private final NarrativeService _narrativeService;


    private final GtfsTripModificationsUtil _util;

    private final TripModsTimeService _timeService;


    @Autowired
    public TripModsTripModificationCreationServiceImpl(TransitGraphDao dao,
                                                       EntityIdService entityIdService,
                                                       NarrativeService narrativeService,
                                                       GtfsTripModificationsUtil gtfsTripModificationsUtil,
                                                       TripModsTimeService timeService) {
        _dao = dao;
        _entityIdService = entityIdService;
        _narrativeService = narrativeService;
        _util = gtfsTripModificationsUtil;
        _timeService = timeService;

    }

    @Override
    public ModifiedTrips createModifiedTrips(List<GtfsRealtime.TripModifications> tripModificationsList) {
        ModifiedTrips modifiedTrips = new ModifiedTrips();

        List<GtfsRealtime.TripModifications> filteredTripModifications = filterTripModifications(tripModificationsList);

        for (GtfsRealtime.TripModifications tripModifications : filteredTripModifications) {

            List<GtfsRealtime.TripModifications.Modification> modifications = tripModifications.getModificationsList();
            List<GtfsRealtime.TripModifications.SelectedTrips> selectedTripsList = tripModifications.getSelectedTripsList();
            Set<LocalDate> selectedTripServiceDates = _util.parseServiceDates(tripModifications.getServiceDatesList());

            for (GtfsRealtime.TripModifications.SelectedTrips selectedTrips : selectedTripsList) {
                List<ModifiedTrip> modifiedTripList = createModifiedTripsForSelectedTrips(selectedTrips,
                        selectedTripServiceDates, modifications);

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
                                                                   List<GtfsRealtime.TripModifications.Modification> modifications) {
        List<ModifiedTrip> modifiedTrips = new ArrayList<>();
        AgencyAndId shapeId = getSelectedTripsShapeId(selectedTrips);
        for(String tripId: selectedTrips.getTripIdsList()){
            TripEntryImpl tripEntry = convertSelectedTripToTripEntry(tripId);
            ModifiedTrip modifiedTrip = createModifiedTrip(tripEntry, shapeId, modifications);
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
        if(modifiedTrip.getStopTimes().isEmpty()) {
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


    List<GtfsRealtime.TripModifications> filterTripModifications(Collection<GtfsRealtime.TripModifications> tripModificationsList) {
        return tripModificationsList.stream().filter(this::isValidTripModification).collect(Collectors.toList());
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
                                    List<GtfsRealtime.TripModifications.Modification> modifications) {

        List<StopTimeEntry> stopTimeEntries = getModifiedStopTimeEntries(tripEntry, modifications);
        agencyAndShapeId = getModifiedShapeId(tripEntry, agencyAndShapeId);
        LocalDate serviceDate = _util.getActiveServiceDateForTrip(tripEntry);

        return new ModifiedTrip(
                tripEntry.getId(),
                agencyAndShapeId,
                stopTimeEntries,
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

    List<StopTimeEntry> getModifiedStopTimeEntries(TripEntryImpl originalTrip,
                                                   List<GtfsRealtime.TripModifications.Modification> modifications) {
        List<StopTimeEntry> modifiedStopTimes = new ArrayList<>();
        List<StopTimeEntry> originalStopTimes = originalTrip.getStopTimes();

        refreshStopTimes(originalStopTimes);

        for (GtfsRealtime.TripModifications.Modification mod : modifications) {
            int startSelectorStopTimesIndex = _util.findStopTimeIndexForSelector(originalStopTimes, mod.getStartStopSelector());
            int endSelectorStopTimesIndex = _util.findStopTimeIndexForSelector(originalStopTimes, mod.getEndStopSelector());
            int postModificationStopTimesIndex = endSelectorStopTimesIndex + 1;

            List<GtfsRealtime.ReplacementStop> replacementStops = mod.getReplacementStopsList();
            int propagatedModificationDelay =  mod.getPropagatedModificationDelay();


            modifiedStopTimes.addAll(getAllStopTimesBeforeSelection(startSelectorStopTimesIndex, originalStopTimes));

            modifiedStopTimes.addAll(getAllReplacedStopTimes(startSelectorStopTimesIndex, originalStopTimes,
                    replacementStops, originalTrip));

            modifiedStopTimes.addAll(getAllStopTimesAfterSelection(postModificationStopTimesIndex, originalStopTimes,
                    propagatedModificationDelay));

        }

        return modifiedStopTimes;

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

    List<StopTimeEntry> getAllReplacedStopTimes(int startSelectorStopTimesIndex,
                                                        List<StopTimeEntry> originalStopTimes,
                                                        List<GtfsRealtime.ReplacementStop> replacementStops,
                                                        TripEntryImpl originalTrip) {

        List<StopTimeEntry> modifiedStopTimes = new ArrayList<>();

        int referenceTime = _util.getReferenceTime(originalStopTimes, startSelectorStopTimesIndex);
        for (var replacementStop : replacementStops) {
            int updatedGtfsStopSequence = DEFAULT_UPDATED_GTFS_STOP_SEQUENCE;
            double shapeDistanceTraveled = DEFAULT_UPDATED_SHAPE_DIST_TRAVELED;

            StopTimeEntry newStopTime = createStopTimeEntry(
                    replacementStop,
                    referenceTime,
                    updatedGtfsStopSequence,
                    shapeDistanceTraveled,
                    originalTrip
            );
            modifiedStopTimes.add(newStopTime);
        }
        return modifiedStopTimes;
    }

    List<StopTimeEntry> getAllStopTimesAfterSelection(int postModificationStopTimesIndex,
                                                              List<StopTimeEntry> originalStopTimes,
                                                              int propagatedModificationDelay) {
        List<StopTimeEntry> modifiedStopTimes = new ArrayList<>();

        for (int i = postModificationStopTimesIndex; i < originalStopTimes.size(); i++) {
            int updatedGtfsStopSequence = DEFAULT_UPDATED_GTFS_STOP_SEQUENCE;
            StopTimeEntry adjusted = adjustStopTime(
                    originalStopTimes.get(i),
                    updatedGtfsStopSequence,
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
                                      TripEntryImpl tripEntry) {

        AgencyAndId stopId = _entityIdService.getStopId(replacementStop.getStopId());

        StopEntry originalStopEntry = _dao.getStopEntryForId(stopId);
        if (originalStopEntry == null) {
            throw new IllegalArgumentException("Stop entry not found for id: " + stopId.toString());
        }

        StopNarrative stopNarrative = _narrativeService.getStopForId(originalStopEntry.getId());
        if (stopNarrative == null) {
            throw new IllegalArgumentException("Stop narrative not found for id: " + stopId.toString());
        }

        double lat = originalStopEntry.getStopLat();
        double lon = originalStopEntry.getStopLon();
        String stopName = _narrativeService.getStopForId(originalStopEntry.getId()).getName();
        StopEntryImpl stopEntry = new StopEntryImpl(stopId, lat, lon);

        int arrivalTime = _util.calculateReplacementStopArrivalTime(replacementStop, referenceTime);

        StopTimeEntryImpl stopTimeEntry = new StopTimeEntryImpl();
        stopTimeEntry.setTrip(tripEntry);
        stopTimeEntry.setStop(stopEntry);
        stopTimeEntry.setArrivalTime(arrivalTime);
        stopTimeEntry.setDepartureTime(arrivalTime); // departure = arrival per spec
        stopTimeEntry.setShapeDistTraveled(shapeDistanceTraveled);
        stopTimeEntry.setGtfsSequence(gtfsStopSequence);

        // Might not want to set this here.
        // This might be cumulative (sequence of ALL stops)
        //stopTimeEntry.setSequence(stopSequence);

        return stopTimeEntry;
    }


    StopTimeEntry adjustStopTime(StopTimeEntry original,
                                 int newSequence,
                                 int propagatedDelay) {
        StopTimeEntryImpl adjusted = new StopTimeEntryImpl(original);
        adjusted.setSequence(newSequence);
        adjusted.setArrivalTime(original.getArrivalTime() + propagatedDelay);
        adjusted.setDepartureTime(original.getDepartureTime() + propagatedDelay);
        return adjusted;
    }

    @Override
    public ModifiedTrip createModifiedTripForExistingTrip(AgencyAndId tripId) {
        TripEntryImpl tripEntry = (TripEntryImpl) _dao.getTripEntryForId(tripId);
        List<GtfsRealtime.TripModifications.Modification> modifications = Collections.EMPTY_LIST;

        return new ModifiedTrip(tripId,
                tripEntry.getShapeId(),
                tripEntry.getStopTimes(),
                tripEntry,
                modifications,
                _util.getActiveServiceDateForTrip(tripEntry));

    }


}
