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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes;

import com.camsys.transit.servicechange.DateDescriptor;
import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import com.camsys.transit.servicechange.field_descriptors.AbstractFieldDescriptor;
import com.camsys.transit.servicechange.field_descriptors.ShapesFields;
import com.camsys.transit.servicechange.field_descriptors.StopTimesFields;
import com.camsys.transit.servicechange.field_descriptors.StopsFields;
import com.camsys.transit.servicechange.field_descriptors.TripsFields;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.GtfsRealtimeEntitySource;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.StopChange;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChange;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphDaoImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.services.AgencyService;
import org.onebusaway.transit_data_federation.services.StopTimeService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import java.util.stream.Collectors;

public class GtfsSometimesHandlerImpl implements GtfsSometimesHandler {

    private TransitGraphDao _dao;

    private List<String> _agencyIds = new ArrayList<>();

    private AgencyService _agencyService;

    private GtfsRealtimeEntitySource _entitySource;

    private ShapePointService _shapePointsService;

    private NarrativeService _narrativeService;

    private RefreshService _refreshService;

    private StopTimeService _stopTimeService;

    private CalendarService _calendarService;

    // for debugging
    private long _time = -1;

    private ZoneId _timeZone = null;

    private static final Logger _log = LoggerFactory.getLogger(GtfsSometimesHandlerImpl.class);

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Autowired
    public void setAgencyService(AgencyService agencyService) {
        _agencyService = agencyService;
    }

    @Autowired
    public void setShapePointsService(ShapePointService shapePointsService) {
        _shapePointsService = shapePointsService;
    }

    @Autowired
    public void setNarrativeService(NarrativeService narrativeService) {
        _narrativeService = narrativeService;
    }

    @Autowired
    public void setRefreshService(RefreshService refreshService) {
        _refreshService = refreshService;
    }

    @Autowired
    public void setStopTimeService(StopTimeService stopTimeService) {
        _stopTimeService = stopTimeService;
    }

    @Autowired
    public void setCalendarService(CalendarService calendarService) {
        _calendarService = calendarService;
    }

    public void setAgencyId(String agencyId) {
        _agencyIds = Collections.singletonList(agencyId);
    }

    public void setAgencyIds(List<String> agencyIds) {
        _agencyIds = agencyIds;
    }

    public void setEntitySource(GtfsRealtimeEntitySource entitySource) {
        _entitySource = entitySource;
    }

    public void setTime(long time) {
        _time = time;
    }

    public void setTimeZone(ZoneId timeZone) {
        _timeZone = timeZone;
    }

    @PostConstruct
    public void init() {
        // Borrowed from GtfsRealtimeSource. TODO - make this behavior reusbale.
        if (_agencyIds.isEmpty()) {
            _log.info("no agency ids specified for GtfsSometimesHandlerImpl, so defaulting to full agency id set");
            List<String> agencyIds = _agencyService.getAllAgencyIds();
            _agencyIds.addAll(agencyIds);
            if (_agencyIds.size() > 3) {
                _log.warn("The default agency id set is quite large (n="
                        + _agencyIds.size()
                        + ").  You might consider specifying the applicable agencies for your GtfsSometimesHandlerImpl.");
            }
        }
        _entitySource = new GtfsRealtimeEntitySource();
        _entitySource.setAgencyIds(_agencyIds);
        _entitySource.setTransitGraphDao(_dao);
        _entitySource.setCalendarService(_calendarService);
    }

    @Override
    public int handleServiceChanges(Collection<ServiceChange> serviceChanges) {
        /*
        Currently handled:
         * adding shapes (do this first so trips can use the new shapes)
         * trip modifications:
         *   inserted stop times
         *   deleted stop times
         *   altered stop times
         *   shape ID change
         */
        int nSuccess = 0;
        List<ServiceChange> activeChanges = filterServiceChanges(serviceChanges);

        // The ordering is tricky. Because we need to use StopEntryImpl to look up stops for trip,
        // we cannot apply StopChanges until we create TripChanges.
        // TripChanges also need to be applied AFTER shape and stop changes so that distances along
        // trip are calculated correctly.

        List<ShapePoints> shapesChanges = getShapePointsToAdd(activeChanges);

        List<StopChange> stopChanges = getAllStopChanges(activeChanges);

        List<TripChange> tripChanges = getAllTripChanges(activeChanges);

        for (ShapePoints shapePoints : shapesChanges) {
            if (_shapePointsService.addShape(shapePoints)) {
                nSuccess++;
            }
        }

        for (StopChange change : stopChanges) {
            if (handleStopChange(change)) {
                nSuccess++;
            }
        }

        for (TripChange change : tripChanges) {
            _log.info("Handling changes for trip {}", change.getTripId());
            if (handleTripChanges(change)) {
                nSuccess++;
            } else {
                _log.info("Unable to apply changes for trip {}", change.getTripId());
            }
         }

        if (nSuccess > 0) {
            forceFlush();
        }
        return nSuccess;
    }


    @Override
    public boolean handleServiceChange(ServiceChange change) {
        return handleServiceChanges(Collections.singleton(change)) > 0;
    }

    List<ServiceChange> filterServiceChanges(Collection<ServiceChange> changes) {
       return changes.stream().filter(this::isServiceChangeOk).collect(Collectors.toList());
    }

    boolean isServiceChangeOk(ServiceChange change) {
        if (!validateServiceChange(change)) {
            _log.debug("service change is invalid");
            return false;
        }
        if (!dateIsApplicable(change)) {
            _log.debug("Service change is not applicable to date.");
            return false;
        }
        return true;
    }

    List<ShapePoints> getShapePointsToAdd(Collection<ServiceChange> changes) {
        List<ShapePoints> shapePointsList = new ArrayList<>();
        for (ServiceChange change : changes) {
            if (Table.SHAPES.equals(change.getTable())
                    && ServiceChangeType.ADD.equals(change.getServiceChangeType())) {
                Collection<List<ShapesFields>> shapesListCollection = change.getAffectedField().stream()
                        .filter(ShapesFields.class::isInstance)
                        .map(ShapesFields.class::cast)
                        .collect(Collectors.groupingBy(ShapesFields::getShapeId))
                        .values();

                for (List<ShapesFields> shapeList : shapesListCollection) {
                    int nPoints = shapeList.size();
                    double[] lat = new double[nPoints];
                    double[] lon = new double[nPoints];
                    double[] distTraveled = new double[nPoints];
                    shapeList.sort(Comparator.comparingInt(ShapesFields::getShapePtSequence));
                    int i = 0;
                    for (ShapesFields fields : shapeList) {
                        lat[i] = fields.getShapePtLat();
                        lon[i] = fields.getShapePtLon();
                        if (fields.getShapeDistTraveled() != null && fields.getShapeDistTraveled() != ShapePoint.MISSING_VALUE) {
                            distTraveled[i] = fields.getShapeDistTraveled();
                        }
                        i++;
                    }
                    ShapePoints shapePoints = new ShapePoints();
                    shapePoints.setLats(lat);
                    shapePoints.setLons(lon);
                    shapePoints.setDistTraveled(distTraveled);
                    AgencyAndId shapeId = new AgencyAndId(_agencyIds.iterator().next(), shapeList.get(0).getShapeId());
                    shapePoints.setShapeId(shapeId);
                    shapePoints.ensureDistTraveled();
                    shapePointsList.add(shapePoints);
                }
            }
        }
        return shapePointsList;
    }

    List<StopChange> getAllStopChanges(Collection<ServiceChange> changes) {
        List<StopChange> stopChanges = new ArrayList<>();
        for (ServiceChange change : changes) {
            if (Table.STOPS.equals(change.getTable())) {
                if (ServiceChangeType.ALTER.equals(change.getServiceChangeType())) {
                    for (EntityDescriptor entity : change.getAffectedEntity()) {
                        StopChange stopChange = new StopChange(entity.getStopId());
                        StopsFields stopsFields = (StopsFields) change.getAffectedField().get(0);
                        if (stopsFields.getStopName() != null) {
                            stopChange.setStopName(stopsFields.getStopName());
                        }
                        if (stopsFields.getStopLat() != null) {
                            stopChange.setStopLat(stopsFields.getStopLat());
                        }
                        if (stopsFields.getStopLon() != null) {
                            stopChange.setStopLon(stopsFields.getStopLon());
                        }
                        stopChanges.add(stopChange);
                    }
                } else {
                    _log.info("Type {} not handled for table {}", change.getServiceChangeType(), change.getTable());
                }
            }
        }
        return stopChanges;
    }

    List<TripChange> getAllTripChanges(Collection<ServiceChange> changes) {
        Map<String, TripChange> changesByTrip = new HashMap<>();
        for (ServiceChange serviceChange : changes) {
            if (Table.TRIPS.equals(serviceChange.getTable())) {
                if (ServiceChangeType.ALTER.equals(serviceChange.getServiceChangeType())) {
                    String shapeId = ((TripsFields) serviceChange.getAffectedField().get(0)).getShapeId();
                    if (shapeId != null) {
                        for (EntityDescriptor desc : serviceChange.getAffectedEntity()) {
                            String tripId = desc.getTripId();
                            if (tripId != null) {
                                AgencyAndId shape = _entitySource.getObaShapeId(shapeId);
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
                AgencyAndId stopId = _entitySource.getObaStopId(bareStopId);
                if (stopsFields.getStopLat() != null || stopsFields.getStopLon() != null) {
                    for (DateDescriptor date : serviceChange.getAffectedDates()) {
                        for (AgencyAndId tripId : getTripsForStopAndDateRange(stopId, date)) {
                            changesByTrip.computeIfAbsent(tripId.getId(), TripChange::new);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(changesByTrip.values());
    }

    private void forceFlush() {
        // TODO - this ultimately should be part of the dao methods, and will be specific to a trip.
        if (_dao instanceof TransitGraphDaoImpl) {
            ((TransitGraphDaoImpl) _dao).updateBlockIndices(null);
            ((TransitGraphDaoImpl) _dao).flushCache();
        }
        _refreshService.refresh(RefreshableResources.BLOCK_SHAPE_DATA);
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

    private boolean dateIsApplicable(ServiceChange change) {
        LocalDate date = getCurrentDate();
        for (DateDescriptor dateDescriptor : change.getAffectedDates()) {
            if (dateDescriptor.getDate() != null && dateDescriptor.getDate().isEqual(date)) {
                return true;
            }
            if (dateDescriptor.getFrom() != null) {
                if (dateDescriptor.getTo() != null) {
                    LocalDate from = dateDescriptor.getFrom();
                    LocalDate to = dateDescriptor.getTo();
                    if ((date.isEqual(from) || date.isAfter(from)) && (date.isEqual(to) || date.isBefore(to))) {
                        return true;
                    }
                } else {
                    LocalDate from = dateDescriptor.getFrom();
                    if ((date.isEqual(from) || date.isAfter(from))) {
                        return true;
                    }
                }
            } else if (dateDescriptor.getTo() != null) {
                _log.error("Not supported: to-date with no from-date specified.");
            }
        }
        return false;
    }

    private boolean handleTripChanges(TripChange change) {
        String trip = change.getTripId();
        AgencyAndId tripId = _entitySource.getObaTripId(trip);
        TripEntryImpl tripEntry;
        List<StopTimeEntry> stopTimes;
        if (change.isAdded()) {
            tripEntry = convertTripFieldsToTripEntry(change.getAddedTripsFields());
            stopTimes = new ArrayList<>();
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
            return _dao.addTripEntry(tripEntry);
        }

        // call internal method
        return _dao.updateStopTimesForTrip(tripEntry, stopTimes, shapeId);
    }

    List<StopTimeEntry> computeNewStopTimes(TripChange change, TripEntryImpl tripEntry, List<StopTimeEntry> stopTimes) {

        // Removed stops

        Set<AgencyAndId> stopsToRemove = new HashSet<>();
        for (EntityDescriptor descriptor : change.getDeletedStops()) {
            AgencyAndId stopId = _entitySource.getObaStopId(descriptor.getStopId());
            stopsToRemove.add(stopId);
        }
        if (!stopTimes.removeIf(ste -> stopsToRemove.contains(ste.getStop().getId()))) {
            _log.error("unable to remove stops for trip {}", tripEntry.getId());
        }

        // Alter - only support changing arrival time/departure time

        for (StopTimesFields stopTimesFields : change.getModifiedStops()) {
            AgencyAndId stopId = _entitySource.getObaStopId(stopTimesFields.getStopId());
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
            AgencyAndId stopId = _entitySource.getObaStopId(stopTimesFields.getStopId());
            StopEntryImpl stopEntry = (StopEntryImpl) _dao.getStopEntryForId(stopId);
            Double shapeDistanceTravelled = stopTimesFields.getShapeDistTraveled();
            int arrivalTime = stopTimesFields.getArrivalTime();
            int departureTime = stopTimesFields.getDepartureTime();
            if (stopEntry != null) {
                StopTimeEntry newEntry = createStopTimeEntry(tripEntry, stopEntry, arrivalTime, departureTime, shapeDistanceTravelled == null ? -999 : shapeDistanceTravelled, -999);
                int insertPosition = 0;
                for (int i = stopTimes.size() - 1; i >= 0; i--) {
                    StopTimeEntry ste = stopTimes.get(i);
                    if (shapeDistanceTravelled != null) {
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

    private boolean handleStopChange(StopChange change) {
        AgencyAndId stopId = _entitySource.getObaStopId(change.getStopId());
        StopEntry oldStopEntry = _dao.getStopEntryForId(stopId);
        StopNarrative narrative = _narrativeService.removeStop(stopId);
        String stopName = change.hasStopName() ? change.getStopName() : narrative.getName();
        int index = oldStopEntry.getIndex();
        double lat = change.hasStopLat() ? change.getStopLat() : oldStopEntry.getStopLat();
        double lon = change.hasStopLon() ? change.getStopLon() : oldStopEntry.getStopLon();
        StopEntryImpl stopEntry = new StopEntryImpl(stopId, lat, lon);
        stopEntry.setIndex(index);
        stopEntry.setWheelchairBoarding(oldStopEntry.getWheelchairBoarding());
        _narrativeService.addStop(stopEntry, stopName);
        if (change.hasStopLat() || change.hasStopLon()) {
            _dao.removeStopEntry(stopId);
            _dao.addStopEntry(stopEntry);
        }
        return true;
    }

    private List<AgencyAndId> getTripsForStopAndDateRange(AgencyAndId stopId, DateDescriptor range) {
        Date from, to;
        LocalDate fromDate = range.getFrom() != null ? range.getFrom() : range.getDate();
        from = Date.from(fromDate.atStartOfDay(getTimeZone()).toInstant());
        if (range.getTo() != null) {
            to = Date.from(range.getTo().atStartOfDay(getTimeZone()).toInstant());
        } else {
            // If there is no "to", end tonight at midnight.
            to = Date.from(getCurrentDate().plusDays(1).atStartOfDay(getTimeZone()).toInstant());
        }
        List<AgencyAndId> tripIds = new ArrayList<>();
        List<StopTimeInstance> instances = _stopTimeService.getStopTimeInstancesInTimeRange(stopId, from, to);
        for (StopTimeInstance instance : instances) {
            tripIds.add(instance.getTrip().getTrip().getId());
        }
        return tripIds;
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

    private TripEntryImpl convertTripFieldsToTripEntry(TripsFields fields) {
        TripEntryImpl trip = new TripEntryImpl();
        String agencyId = _entitySource.getDefaultAgencyId();
        if (fields.getRouteId() != null) {
            AgencyAndId routeId = _entitySource.getObaRouteId(fields.getRouteId());
            RouteEntry routeEntry = _dao.getRouteForId(routeId);
            trip.setRoute((RouteEntryImpl) routeEntry);
            agencyId = routeId.getAgencyId();
        }

        AgencyAndId tripId = new AgencyAndId(agencyId, fields.getTripId());
        trip.setId(tripId);

        if (fields.getShapeId() != null) {
            AgencyAndId shapeId = _entitySource.getObaShapeId(fields.getShapeId());
            trip.setShapeId(shapeId);
        }
        if (fields.getServiceId() != null) {
            AgencyAndId serviceId = _entitySource.getObaServiceId(fields.getServiceId());
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

    private long getCurrentTime() {
        if (_time != -1)
            return _time;
        return new Date().getTime();
    }

    private LocalDate getCurrentDate() {
        return Instant.ofEpochMilli(getCurrentTime())
                .atZone(getTimeZone()).toLocalDate();
    }

    private ZoneId getTimeZone() {
        if (_timeZone != null) {
            return _timeZone;
        } else {
            return ZoneId.systemDefault();
        }
    }
}

