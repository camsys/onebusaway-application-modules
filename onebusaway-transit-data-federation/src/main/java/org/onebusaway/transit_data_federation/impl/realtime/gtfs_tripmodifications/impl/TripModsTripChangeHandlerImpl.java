package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import com.camsys.transit.servicechange.DateDescriptor;
import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.field_descriptors.StopTimesFields;
import com.camsys.transit.servicechange.field_descriptors.TripsFields;
import com.google.transit.realtime.GtfsRealtime.ReplacementStop;
import com.google.transit.realtime.GtfsRealtime.TripModifications;
import com.google.transit.realtime.GtfsRealtime.StopSelector;
import com.google.transit.realtime.GtfsRealtime.TripModifications.SelectedTrips;
import com.google.transit.realtime.GtfsRealtime.TripModifications.Modification;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl.GtfsServiceChangeLibrary.*;

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

    private DateTimeFormatter SERVICE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    //TODO what is the service date format

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
                    TripChange change = createTripChange(
                            agencyTripId,
                            agencyShapeId,
                            serviceDate,
                            modifications
                    );

                    if (change instanceof DeleteTrip) {
                        changeSet.addDeletedTrip((DeleteTrip) change);
                    } else if (change instanceof ModifyTrip) {
                        changeSet.addModifiedTrip((ModifyTrip) change);
                    }
                }
            }
        }

        return changeSet;
    }

    private TripChange createTripChange(AgencyAndId tripId,
                                        AgencyAndId shapeId,
                                        LocalDate serviceDate,
                                        List<Modification> modifications) {

        // effectively a deletion? (all stops removed, no replacements)
        if (isEffectiveDeletion(modifications)) {
            return createDeleteTrip(tripId, serviceDate);
        }

        return createModifyTrip(tripId, shapeId, serviceDate, modifications);
    }

    private boolean isEffectiveDeletion(List<Modification> modifications) {
        // A trip is effectively deleted if modifications remove all stops
        // and provide no replacement stops
        if (modifications.size() == 1) {
            Modification mod = modifications.get(0);
            // If there's a single modification with no replacement stops,
            // and it spans the whole trip, treat as deletion
            return mod.getReplacementStopsCount() == 0;
        }
        return false;
    }

    private DeleteTrip createDeleteTrip(AgencyAndId tripId, LocalDate serviceDate) {
        //TODO fix endtime below
        DeleteTrip deleteTrip = new DeleteTrip(tripId, serviceDate, LocalDateTime.now());
        // endTime maybe could be set based on the original trip's end time if needed
        return deleteTrip;
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
                        result.size() + 1
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
                                              int stopSequence) {
        String stopId = replacementStop.getStopId();

        Double lat = null;
        Double lon = null;
        String stopName = null; //TODO not needed?

        StopEntry se = _dao.getStopEntryForId(AgencyAndId.convertFromString(stopId));
        if (se == null) {
            throw new IllegalArgumentException("Stop entry not found for id: " + stopId);
        }
        StopNarrative sn = _narrativeService.getStopForId(se.getId());
        if (sn == null) {
            throw new IllegalArgumentException("Stop narrative not found for id: " + stopId);
        }

        lat = se.getStopLat();
        lon = se.getStopLon();
        stopName = _narrativeService.getStopForId(se.getId()).getName();

        // Calculate arrival time
        int arrivalTime = referenceTime;
        if (replacementStop.hasTravelTimeToStop()) {
            arrivalTime = referenceTime + replacementStop.getTravelTimeToStop();
        }

        // Build your StopTimeEntry - adjust to match your actual class
        StopTimeEntryImpl entry = new StopTimeEntryImpl();
        entry.setSequence(stopSequence);
        entry.setArrivalTime(arrivalTime);
        entry.setDepartureTime(arrivalTime);  // departure = arrival per spec
        StopEntryImpl stopEntry = new StopEntryImpl(_entityIdService.getStopId(stopId),lat,lon);
        entry.setStop(stopEntry);

        return entry;
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