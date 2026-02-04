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

import com.camsys.transit.servicechange.DateDescriptor;
import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.field_descriptors.StopTimesFields;
import com.camsys.transit.servicechange.field_descriptors.TripsFields;
import com.google.transit.realtime.GtfsRealtime.ReplacementStop;
import com.google.transit.realtime.GtfsRealtime.TripModifications;
import com.google.transit.realtime.GtfsRealtime.StopSelector;
import com.google.transit.realtime.GtfsRealtime.TripModifications.SelectedTrips;
import com.google.transit.realtime.GtfsRealtime.TripModifications.Modification;
import org.onebusaway.container.cache.CacheableMethodManager;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.*;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripChangeHandler;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.StopTimeInstance;
import org.onebusaway.transit_data_federation.model.narrative.RouteCollectionNarrative;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.StopTimeService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl.GtfsTripModsServiceChangeLibrary.*;

import static org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl.GtfsTripModsServiceChangeLibrary.*;

@Component
public class TripModsTripChangeHandlerImpl implements TripModsTripChangeHandler {

    private static final Logger _log = LoggerFactory.getLogger(org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl.TripChangeHandlerImpl.class);

    private TransitGraphDao _dao;

    private EntityIdService _entityIdService;

    private NarrativeService _narrativeService;

    private StopTimeService _stopTimeService;

    private TimeService _timeService;

    private BlockCalendarService _blockCalendarService;

    private CalendarService _calendarService;

    private TripChangeSet revertTripChanges;

    private RefreshService _refreshService;

    private CacheableMethodManager _cacheableMethodManager;

    private CacheableMethodManager _cacheableAnnotationInterceptor;

    private boolean _isApplying = false;

    private DateTimeFormatter SERVICE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    //TODO what is the service date format

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
    public void setStopTimeService(StopTimeService stopTimeService) {
        _stopTimeService = stopTimeService;
    }

    @Autowired
    public void setTimeService(TimeService timeService) {
        _timeService = timeService;
    }

    @Autowired
    public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
        _blockCalendarService = blockCalendarService;
    }

    @Autowired
    public void setCalendarService(CalendarService calendarService) {
        _calendarService = calendarService;
    }

    @Override
    public TripChangeSet getAllTripChanges(TripModifications tripModifications) {
        TripChangeSet changeSet = new TripChangeSet();

        List<LocalDate> serviceDates = parseServiceDates(tripModifications.getServiceDatesList());
        List<Modification> modifications = tripModifications.getModificationsList();

        for (SelectedTrips selectedTrips : tripModifications.getSelectedTripsList()) {
            String shapeId = selectedTrips.hasShapeId() ? selectedTrips.getShapeId() : null;

            for (String tripId : selectedTrips.getTripIdsList()) {
                String agencyId = _entityIdService.getTripId(tripId).getAgencyId();
                AgencyAndId agencyTripId = new AgencyAndId(agencyId, tripId);
                AgencyAndId agencyShapeId = shapeId != null ? new AgencyAndId(agencyId, shapeId) : null;

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

        return modifyTrip;
    }

    private List<StopTimeEntry> buildModifiedStopTimes(TripEntryImpl originalTrip,
                                                       List<Modification> modifications) {
        List<StopTimeEntry> result = new ArrayList<>();
        List<StopTimeEntry> originalStopTimes = originalTrip.getStopTimes();

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

            for (int i = endIndex + 1; i < originalStopTimes.size(); i++) {
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
        stopTimeEntry.setSequence(stopSequence);
        stopTimeEntry.setArrivalTime(arrivalTime);
        stopTimeEntry.setDepartureTime(arrivalTime);  // departure = arrival per spec
        StopEntryImpl stopEntry = new StopEntryImpl(_entityIdService.getStopId(stopId),lat,lon);
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