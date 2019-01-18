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

import com.camsys.transit.servicechange.DateDescriptor;
import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import com.camsys.transit.servicechange.field_descriptors.AbstractFieldDescriptor;
import com.camsys.transit.servicechange.field_descriptors.StopTimesFields;
import com.camsys.transit.servicechange.field_descriptors.StopsFields;
import com.camsys.transit.servicechange.field_descriptors.TripsFields;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.AddTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.DeleteTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.IntermediateTripChange;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ModifyTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TripChangeHandler;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.narrative.RouteCollectionNarrative;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.StopTimeService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl.GtfsServiceChangeLibrary.*;

@Component
public class TripChangeHandlerImpl implements TripChangeHandler {

    private static final Logger _log = LoggerFactory.getLogger(TripChangeHandlerImpl.class);

    private TransitGraphDao _dao;

    private EntityIdService _entityIdService;

    private NarrativeService _narrativeService;

    private StopTimeService _stopTimeService;

    private TimeService _timeService;

    private BlockCalendarService _blockCalendarService;

    private CalendarService _calendarService;

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
    public TripChangeSet getAllTripChanges(Collection<ServiceChange> changes) {
        return getChangeset(getTripChanges(changes));
    }

    TripChangeSet getChangeset(List<IntermediateTripChange> changes) {
        TripChangeSet changeset = new TripChangeSet();
        for (IntermediateTripChange change : changes) {
            String trip = change.getTripId();
            AgencyAndId tripId = _entityIdService.getTripId(trip);
            if (change.isDelete()) {
                LocalDate serviceDate = getServiceDateForTrip(change);
                if (serviceDate != null && dateIsApplicable(serviceDate, change.getDates())) {
                    TripEntry tripEntry = _dao.getTripEntryForId(tripId);
                    StopTimeEntry stopTime = tripEntry.getStopTimes().get(tripEntry.getStopTimes().size() - 1);
                    LocalDateTime endTime = serviceDate.atStartOfDay().plusSeconds(stopTime.getArrivalTime());
                    changeset.addDeletedTrip(new DeleteTrip(tripId, serviceDate, endTime));
                }
            } else if (change.isAdded()) {
                LocalDate serviceDate = getServiceDateForAddedTrip(change);
                if (!dateIsApplicable(serviceDate, change.getDates())) {
                    continue;
                }
                if (change.getAddedTripsFields().getServiceId() == null) {
                    _log.info("No service ID, skipping.");
                    continue;
                }
                TripEntryImpl tripEntry = convertTripFieldsToTripEntry(change.getAddedTripsFields());
                List<StopTimeEntry> stopTimes = new ArrayList<>();
                stopTimes = computeNewStopTimes(change, tripEntry, stopTimes);
                tripEntry.setStopTimes(stopTimes);
                TripNarrative narrative = convertTripFieldsToTripNarrative(change.getAddedTripsFields());
                AddTrip addTrip = new AddTrip();
                addTrip.setTripId(tripId);
                addTrip.setTripEntry(tripEntry);
                addTrip.setTripNarrative(narrative);
                addTrip.setServiceDate(serviceDate);
                changeset.addAddedTrip(addTrip);

            } else if (change.isModify()) {
                LocalDate serviceDate = getServiceDateForTrip(change);
                // This is tricky: the dateIsApplicable check means that some blocks will have some stops with older data,
                // if there is a StopEntryImpl change. Currently handled via normalizing the StopEntry when creating
                // indices in BlockIndexServiceImpl, but could also pull the dateIsApplicable check here.
                if (serviceDate == null || !dateIsApplicable(serviceDate, change.getDates())) {
                    continue;
                }
                TripEntryImpl tripEntry = (TripEntryImpl) _dao.getTripEntryForId(tripId);
                if (tripEntry == null) {
                    _log.info("No trip found for trip change {}", tripId);
                    continue;
                }
                List<StopTimeEntry> stopTimes = new ArrayList<>(tripEntry.getStopTimes());
                // Ensure stops are fresh
                for (StopTimeEntry stopTimeEntry : stopTimes) {
                    StopEntryImpl stopEntry = (StopEntryImpl) _dao.getStopEntryForId(stopTimeEntry.getStop().getId());
                    ((StopTimeEntryImpl) stopTimeEntry).setStop(stopEntry);
                }

                AgencyAndId shapeId = change.getNewShapeId();
                if (shapeId == null) {
                    shapeId = tripEntry.getShapeId();
                }
                stopTimes = computeNewStopTimes(change, tripEntry, stopTimes);
                ModifyTrip modify = new ModifyTrip();
                modify.setTripId(tripId);
                modify.setShapeId(shapeId);
                modify.setStopTimes(stopTimes);
                modify.setTripEntry(tripEntry);
                modify.setServiceDate(serviceDate);
                changeset.addModifiedTrip(modify);
            }
        }
        return changeset;
    }

    List<IntermediateTripChange> getTripChanges(Collection<ServiceChange> changes) {
        Map<String, IntermediateTripChange> changesByTrip = new HashMap<>();
        for (ServiceChange serviceChange : changes) {
            if (Table.TRIPS.equals(serviceChange.getTable())) {
                if (ServiceChangeType.ALTER.equals(serviceChange.getServiceChangeType())) {
                    String shapeId = ((TripsFields) serviceChange.getAffectedField().get(0)).getShapeId();
                    if (shapeId != null) {
                        for (EntityDescriptor desc : serviceChange.getAffectedEntity()) {
                            String tripId = desc.getTripId();
                            if (tripId != null) {
                                AgencyAndId shape = _entityIdService.getShapeId(shapeId);
                                IntermediateTripChange change = changesByTrip.computeIfAbsent(tripId, IntermediateTripChange::new);
                                change.setNewShapeId(shape);
                                change.setDates(serviceChange.getAffectedDates());
                            }
                        }
                    }
                } else if (ServiceChangeType.ADD.equals(serviceChange.getServiceChangeType())) {
                    for (AbstractFieldDescriptor fd : serviceChange.getAffectedField()) {
                        TripsFields tripsFields = (TripsFields) fd;
                        String tripId = tripsFields.getTripId();
                        if (tripId != null) {
                            IntermediateTripChange change = changesByTrip.computeIfAbsent(tripId, IntermediateTripChange::new);
                            change.setAddedTripsFields(tripsFields);
                            change.setDates(serviceChange.getAffectedDates());
                            if (change.getAddedTripsFields().getServiceId() == null) {
                                String serviceId = lookupActiveServiceId();
                                change.getAddedTripsFields().setServiceId(serviceId);
                                _log.warn("Added serviceId={} to added trip={}", serviceId, tripId);
                            }
                        }
                    }
                } else if (ServiceChangeType.DELETE.equals(serviceChange.getServiceChangeType())) {
                    for (EntityDescriptor desc : serviceChange.getAffectedEntity()) {
                        String tripId = desc.getTripId();
                        if (tripId != null) {
                            IntermediateTripChange change = changesByTrip.computeIfAbsent(tripId, IntermediateTripChange::new);
                            change.setDelete();
                            change.setDates(serviceChange.getAffectedDates());
                        }
                    }
                }
            } else if (Table.STOP_TIMES.equals(serviceChange.getTable())) {
                switch (serviceChange.getServiceChangeType()) {
                    case ADD:
                        for (AbstractFieldDescriptor desc : serviceChange.getAffectedField()) {
                            StopTimesFields stopTimesFields = (StopTimesFields) desc;
                            if (stopTimesFields.getTripId() != null) {
                                IntermediateTripChange change = changesByTrip.computeIfAbsent(stopTimesFields.getTripId(), IntermediateTripChange::new);
                                change.addInsertedStop(stopTimesFields);
                                change.setDates(serviceChange.getAffectedDates());
                            }
                        }
                        break;
                    case ALTER:
                        StopTimesFields stopTimesFields = (StopTimesFields) serviceChange.getAffectedField().get(0);
                        for (EntityDescriptor desc : serviceChange.getAffectedEntity()) {
                            StopTimesFields fields = new StopTimesFields();
                            fields.setArrivalTime(stopTimesFields.getArrivalTime());
                            fields.setDepartureTime(stopTimesFields.getDepartureTime());
                            String tripId = desc.getTripId();
                            fields.setTripId(tripId);
                            fields.setStopId(desc.getStopId());
                            if (tripId != null) {
                                IntermediateTripChange change = changesByTrip.computeIfAbsent(tripId, IntermediateTripChange::new);
                                change.addModifiedStop(fields);
                                change.setDates(serviceChange.getAffectedDates());
                            }
                        }
                        break;
                    case DELETE:
                        for (EntityDescriptor descriptor : serviceChange.getAffectedEntity()) {
                            String tripId = descriptor.getTripId();
                            String stopId = descriptor.getStopId();
                            if (tripId != null && stopId != null) {
                                IntermediateTripChange change = changesByTrip.computeIfAbsent(tripId, IntermediateTripChange::new);
                                change.addDeletedStop(descriptor);
                                change.setDates(serviceChange.getAffectedDates());
                            }
                        }
                        break;
                }
            } else if (Table.STOPS.equals(serviceChange.getTable())
                    && ServiceChangeType.ALTER.equals(serviceChange.getServiceChangeType())) {
                // If a stop location has changed, all incident trips must have their distances recalculated
                // So just make sure a TripChange object is created
                StopsFields stopsFields = (StopsFields) serviceChange.getAffectedField().get(0);
                String bareStopId = serviceChange.getAffectedEntity().get(0).getStopId();
                AgencyAndId stopId = _entityIdService.getStopId(bareStopId);
                if (stopsFields.getStopLat() != null || stopsFields.getStopLon() != null || stopsFields.getStopName() != null) {
                    for (DateDescriptor date : serviceChange.getAffectedDates()) {
                        for (AgencyAndId tripId : getTripsForStopAndDateRange(stopId, date)) {
                            IntermediateTripChange change = changesByTrip.computeIfAbsent(tripId.getId(), IntermediateTripChange::new);
                            if (change.getDates() == null) {
                                change.setDates(serviceChange.getAffectedDates());
                            }
                        }
                    }
                }
            }
        }
        List<IntermediateTripChange> tripChanges = new ArrayList<>(changesByTrip.values());
        return tripChanges;
    }

    @Override
    public TripChangeSet handleTripChanges(TripChangeSet changeset) {
        TripChangeSet revertSet = new TripChangeSet();
        for (DeleteTrip deleteTrip : changeset.getDeletedTrips()) {
            AgencyAndId tripId = deleteTrip.getTripId();
            _log.info("Handling changes for trip {}", tripId);
            // If deleting a trip, we need to add one.
            AddTrip addTrip = getAddTripForExistingTrip(tripId);
            if (_dao.deleteTripEntryForId(tripId)) {
                revertSet.addAddedTrip(addTrip);
            } else {
                _log.info("Unable to apply changes for trip {}", tripId);
            }
        }
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

    List<StopTimeEntry> computeNewStopTimes(IntermediateTripChange change, TripEntryImpl tripEntry) {
        return computeNewStopTimes(change, tripEntry, new ArrayList<>(tripEntry.getStopTimes()));
    }

    List<StopTimeEntry> computeNewStopTimes(IntermediateTripChange change, TripEntryImpl tripEntry, List<StopTimeEntry> stopTimes) {

        // Removed stops

        Set<AgencyAndId> stopsToRemove = new HashSet<>();
        for (EntityDescriptor descriptor : change.getDeletedStops()) {
            AgencyAndId stopId = _entityIdService.getStopId(descriptor.getStopId());
            stopsToRemove.add(stopId);
        }
        if (!stopsToRemove.isEmpty()) {
            if (!stopTimes.removeIf(ste -> stopsToRemove.contains(ste.getStop().getId()))) {
                _log.error("unable to remove stops for trip {}", tripEntry.getId());
            }
        }

        // Alter - only support changing arrival time/departure time

        for (StopTimesFields stopTimesFields : change.getModifiedStops()) {
            AgencyAndId stopId = _entityIdService.getStopId(stopTimesFields.getStopId());
            for (int i = 0; i < stopTimes.size(); i++) {
                StopTimeEntry stopTime = stopTimes.get(i);
                if (stopTime.getStop().getId().equals(stopId)) {
                    StopTimeEntryImpl newStopTime = new StopTimeEntryImpl(stopTime);
                    newStopTime.setArrivalTime(stopTimesFields.getArrivalTime());
                    newStopTime.setDepartureTime(stopTimesFields.getDepartureTime());
                    stopTimes.set(i, newStopTime);
                    break;
                }
            }
        }

        // Inserted stops

        for (StopTimesFields stopTimesFields : change.getInsertedStops()) {
            AgencyAndId stopId = _entityIdService.getStopId(stopTimesFields.getStopId());
            StopEntryImpl stopEntry = (StopEntryImpl) _dao.getStopEntryForId(stopId);
            Double shapeDistanceTravelled = stopTimesFields.getShapeDistTraveled();
            int arrivalTime = stopTimesFields.getArrivalTime();
            int departureTime = stopTimesFields.getDepartureTime();
            if (stopEntry != null) {
                StopTimeEntry newEntry = createStopTimeEntry(tripEntry, stopEntry, arrivalTime, departureTime, shapeDistanceTravelled == null ? -999 : shapeDistanceTravelled, -999);
                int insertPosition = 0;
                for (int i = stopTimes.size() - 1; i >= 0; i--) {
                    StopTimeEntry ste = stopTimes.get(i);
                    if (shapeDistanceTravelled != null && shapeDistanceTravelled > 0) {
                        // we have shape distance, use it to determine insertion position
                        if (shapeDistanceTravelled > ste.getShapeDistTraveled()) {
                            insertPosition = i + 1;
                            break;
                        }
                    } else if (arrivalTime >= 0) {
                        // try arrivalTime
                        if (arrivalTime > ste.getArrivalTime()) {
                            insertPosition = i + 1;
                            break;
                        }
                    } else {
                        // use departureTime
                        if (departureTime > ste.getDepartureTime()) {
                            insertPosition = i + 1;
                            break;
                        }
                    }
                }
                stopTimes.add(insertPosition, newEntry);
            }
        }
        return stopTimes;
    }

    private List<AgencyAndId> getTripsForStopAndDateRange(AgencyAndId stopId, DateDescriptor range) {
        Date from, to;
        LocalDate fromDate = range.getFrom() != null ? range.getFrom() : range.getDate();
        from = Date.from(fromDate.atStartOfDay(_timeService.getTimeZone()).toInstant());
        if (range.getTo() != null) {
            to = Date.from(range.getTo().atStartOfDay(_timeService.getTimeZone()).toInstant());
        } else {
            // If there is no "to", end tonight at midnight.
            to = Date.from(_timeService.getCurrentDate().plusDays(1).atStartOfDay(_timeService.getTimeZone()).toInstant());
        }
        List<AgencyAndId> tripIds = new ArrayList<>();
        List<StopTimeInstance> instances = _stopTimeService.getStopTimeInstancesInTimeRange(stopId, from, to);
        for (StopTimeInstance instance : instances) {
            tripIds.add(instance.getTrip().getTrip().getId());
        }
        return tripIds;
    }

    private TripEntryImpl convertTripFieldsToTripEntry(TripsFields fields) {
        TripEntryImpl trip = new TripEntryImpl();
        String agencyId = _entityIdService.getDefaultAgencyId();
        if (fields.getRouteId() != null) {
            AgencyAndId routeId = _entityIdService.getRouteId(fields.getRouteId());
            RouteEntry routeEntry = _dao.getRouteForId(routeId);
            trip.setRoute((RouteEntryImpl) routeEntry);
            agencyId = routeId.getAgencyId();
        }

        AgencyAndId tripId = new AgencyAndId(agencyId, fields.getTripId());
        trip.setId(tripId);

        if (fields.getShapeId() != null) {
            AgencyAndId shapeId = _entityIdService.getShapeId(fields.getShapeId());
            trip.setShapeId(shapeId);
        }
        if (fields.getServiceId() != null) {
            AgencyAndId serviceId = _entityIdService.getServiceId(fields.getServiceId());
            trip.setServiceId(new LocalizedServiceId(serviceId, TimeZone.getDefault()));
        }
        BlockEntryImpl blockEntry = null;
        AgencyAndId blockId = null;
        if (fields.getBlockId() != null) {
            blockId = new AgencyAndId(agencyId, fields.getBlockId());
            blockEntry = (BlockEntryImpl) _dao.getBlockEntryForId(blockId);
        }
        if (blockEntry == null) {
            if (blockId == null) {
                blockId = new AgencyAndId(agencyId, fields.getTripId());
            }
            blockEntry = new BlockEntryImpl();
            blockEntry.setId(blockId);
        }
        trip.setBlock(blockEntry);
        return trip;
    }

    private TripNarrative convertTripFieldsToTripNarrative(TripsFields fields) {
        TripNarrative.Builder builder = TripNarrative.builder();
        if (fields.getTripHeadsign() != null) {
            builder.setTripHeadsign(fields.getTripHeadsign());
        }
        if (fields.getTripShortName() != null) {
            builder.setTripShortName(fields.getTripShortName());
        }
        if (fields.getRouteId() != null) {
            AgencyAndId routeId = _entityIdService.getRouteId(fields.getRouteId());
            RouteCollectionNarrative routeNarrative = _narrativeService.getRouteCollectionForId(routeId);
            builder.setRouteShortName(routeNarrative.getShortName());
        }
        return builder.create();
    }

    private StopTimeEntry createStopTimeEntry(TripEntryImpl tripEntry, StopEntryImpl stopEntry, int arrivalTime, int departureTime, double shapeDistanceTravelled, int gtfsSequence) {
        StopTimeEntryImpl stei = new StopTimeEntryImpl();
        stei.setTrip(tripEntry);
        stei.setStop(stopEntry);
        stei.setArrivalTime(arrivalTime);
        stei.setDepartureTime(departureTime);
        stei.setShapeDistTraveled(shapeDistanceTravelled);
        stei.setGtfsSequence(gtfsSequence);
        return stei;
    }

    private AddTrip getAddTripForExistingTrip(AgencyAndId tripId) {
        TripEntryImpl tripEntry = (TripEntryImpl) _dao.getTripEntryForId(tripId);
        TripNarrative narrative = _narrativeService.getTripForId(tripId);
        AddTrip addTrip = new AddTrip();
        addTrip.setTripId(tripId);
        addTrip.setTripEntry(tripEntry);
        addTrip.setTripNarrative(narrative);
        addTrip.setServiceDate(getServiceDateForTrip(tripEntry));
        return addTrip;
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

    private LocalDate getServiceDateForAddedTrip(IntermediateTripChange change) {
        LocalDateTime now = _timeService.getCurrentTime();
        LocalDate today = _timeService.getCurrentDate();
        LocalDate yesterday = today.minusDays(1);
        // Find the next active "block" (just this trip), and use that time.
        int maxTime = change.getInsertedStops().stream().mapToInt(StopTimesFields::getArrivalTime).max().getAsInt();
        if (yesterday.atStartOfDay().plus(maxTime, ChronoUnit.SECONDS).isAfter(now)) {
            if (dateIsApplicable(yesterday, change.getDates())) {
                return yesterday;
            }
        }
        return today;
    }

    private LocalDate getServiceDateForTrip(IntermediateTripChange change) {
        // Trip is valid if the next service date for the trip is valid for any of the dates.
        AgencyAndId tripId = _entityIdService.getTripId(change.getTripId());
        TripEntry trip = _dao.getTripEntryForId(tripId);
        if (trip == null) {
            return null;
        }
        return getServiceDateForTrip(trip);
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

    private String lookupActiveServiceId() {
        ServiceDate today = toServiceDate(_timeService.getCurrentDate());
        Set<AgencyAndId> serviceIds = _calendarService.getServiceIdsOnDate(today);
        return serviceIds.isEmpty() ? null : serviceIds.iterator().next().getId();
    }
}