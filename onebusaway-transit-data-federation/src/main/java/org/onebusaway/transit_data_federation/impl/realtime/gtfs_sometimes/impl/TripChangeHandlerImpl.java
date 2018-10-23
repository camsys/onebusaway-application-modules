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
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChange;
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
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Component
public class TripChangeHandlerImpl implements TripChangeHandler {

    private static final Logger _log = LoggerFactory.getLogger(TripChangeHandlerImpl.class);

    private TransitGraphDao _dao;

    private EntityIdService _entityIdService;

    private NarrativeService _narrativeService;

    private StopTimeService _stopTimeService;

    private TimeService _timeService;

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

    @Override
    public TripChangeSet getAllTripChanges(Collection<ServiceChange> changes) {
        Map<String, TripChange> changesByTrip = new HashMap<>();
        for (ServiceChange serviceChange : changes) {
            if (Table.TRIPS.equals(serviceChange.getTable())) {
                if (ServiceChangeType.ALTER.equals(serviceChange.getServiceChangeType())) {
                    String shapeId = ((TripsFields) serviceChange.getAffectedField().get(0)).getShapeId();
                    if (shapeId != null) {
                        for (EntityDescriptor desc : serviceChange.getAffectedEntity()) {
                            String tripId = desc.getTripId();
                            if (tripId != null) {
                                AgencyAndId shape = _entityIdService.getShapeId(shapeId);
                                changesByTrip.computeIfAbsent(tripId, TripChange::new).setNewShapeId(shape);
                            }
                        }
                    }
                } else if (ServiceChangeType.ADD.equals(serviceChange.getServiceChangeType())) {
                    for (AbstractFieldDescriptor fd : serviceChange.getAffectedField()) {
                        TripsFields tripsFields = (TripsFields) fd;
                        String tripId = tripsFields.getTripId();
                        if (tripId != null) {
                            changesByTrip.computeIfAbsent(tripId, TripChange::new).setAddedTripsFields(tripsFields);
                        }
                    }
                } else if (ServiceChangeType.DELETE.equals(serviceChange.getServiceChangeType())) {
                    for (EntityDescriptor desc : serviceChange.getAffectedEntity()) {
                        String tripId = desc.getTripId();
                        if (tripId != null) {
                            changesByTrip.computeIfAbsent(tripId, TripChange::new).setDelete();
                        }
                    }
                }
            } else if (Table.STOP_TIMES.equals(serviceChange.getTable())) {
                switch (serviceChange.getServiceChangeType()) {
                    case ADD:
                        for (AbstractFieldDescriptor desc : serviceChange.getAffectedField()) {
                            StopTimesFields stopTimesFields = (StopTimesFields) desc;
                            if (stopTimesFields.getTripId() != null) {
                                changesByTrip.computeIfAbsent(stopTimesFields.getTripId(), TripChange::new).addInsertedStop(stopTimesFields);
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
                                changesByTrip.computeIfAbsent(tripId, TripChange::new).addModifiedStop(fields);
                            }
                        }
                        break;
                    case DELETE:
                        for (EntityDescriptor descriptor : serviceChange.getAffectedEntity()) {
                            String tripId = descriptor.getTripId();
                            String stopId = descriptor.getStopId();
                            if (tripId != null && stopId != null) {
                                changesByTrip.computeIfAbsent(tripId, TripChange::new).addDeletedStop(descriptor);
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
                if (stopsFields.getStopLat() != null || stopsFields.getStopLon() != null) {
                    for (DateDescriptor date : serviceChange.getAffectedDates()) {
                        for (AgencyAndId tripId : getTripsForStopAndDateRange(stopId, date)) {
                            changesByTrip.computeIfAbsent(tripId.getId(), TripChange::new);
                        }
                    }
                }
            }
        }
        List<TripChange> tripChanges = new ArrayList<>(changesByTrip.values());
        return new TripChangeSet(tripChanges);
    }

    @Override
    public int handleTripChanges(TripChangeSet changeset) {
        int nSuccess = 0;
        for (TripChange change : changeset.getTripChanges()) {
            _log.info("Handling changes for trip {}", change.getTripId());
            if (handleTripChanges(change)) {
                nSuccess++;
            } else {
                _log.info("Unable to apply changes for trip {}", change.getTripId());
            }
        }
        return nSuccess;
    }

    private boolean handleTripChanges(TripChange change) {
        String trip = change.getTripId();
        AgencyAndId tripId = _entityIdService.getTripId(trip);
        TripEntryImpl tripEntry;
        List<StopTimeEntry> stopTimes;
        if (change.isAdded()) {
            tripEntry = convertTripFieldsToTripEntry(change.getAddedTripsFields());
            stopTimes = new ArrayList<>();
        } else if (change.isDelete()) {
            return _dao.deleteTripEntryForId(tripId);
        } else {
            tripEntry = (TripEntryImpl) _dao.getTripEntryForId(tripId);
            if (tripEntry == null) {
                return false;
            }
            stopTimes = new ArrayList<>(tripEntry.getStopTimes());
        }

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

        if (change.isAdded()) {
            tripEntry.setStopTimes(stopTimes);
            TripNarrative narrative = convertTripFieldsToTripNarrative(change.getAddedTripsFields());
            return _dao.addTripEntry(tripEntry, narrative);
        }

        // call internal method
        return _dao.updateStopTimesForTrip(tripEntry, stopTimes, shapeId);
    }

    List<StopTimeEntry> computeNewStopTimes(TripChange change, TripEntryImpl tripEntry) {
        return computeNewStopTimes(change, tripEntry, new ArrayList<>(tripEntry.getStopTimes()));
    }

    List<StopTimeEntry> computeNewStopTimes(TripChange change, TripEntryImpl tripEntry, List<StopTimeEntry> stopTimes) {

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
}
