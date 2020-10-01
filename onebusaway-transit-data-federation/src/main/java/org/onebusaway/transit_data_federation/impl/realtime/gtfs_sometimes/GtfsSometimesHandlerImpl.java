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
import com.camsys.transit.servicechange.field_descriptors.TripsFields;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.GtfsRealtimeEntitySource;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphDaoImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.AgencyService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GtfsSometimesHandlerImpl implements GtfsSometimesHandler {

    private TransitGraphDao _dao;

    private List<String> _agencyIds = new ArrayList<>();

    private AgencyService _agencyService;

    private GtfsRealtimeEntitySource _entitySource;

    private ShapePointService _shapePointsService;

    private RefreshService _refreshService;

    // for debugging
    private long _time = -1;

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
    public void setRefreshService(RefreshService refreshService) {
        _refreshService = refreshService;
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
        Collection<ServiceChange> addedShapes = new ArrayList<>();
        Map<String, TripChange> changesByTrip = new HashMap<>();

        for (ServiceChange serviceChange : serviceChanges) {
            if (!validateServiceChange(serviceChange)) {
                _log.debug("service change is invalid");
                continue;
            }
            if (!dateIsApplicable(serviceChange)) {
                _log.debug("Service change is not applicable to date.");
                continue;
            }
            if (Table.SHAPES.equals(serviceChange.getTable())
                    && ServiceChangeType.ADD.equals(serviceChange.getServiceChangeType())) {
                addedShapes.add(serviceChange);
            } else if (Table.TRIPS.equals(serviceChange.getTable())
                    && ServiceChangeType.ALTER.equals(serviceChange.getServiceChangeType())) {
                String shapeId = ((TripsFields) serviceChange.getAffectedField().get(0)).getShapeId();
                if (shapeId != null) {
                    for (EntityDescriptor desc : serviceChange.getAffectedEntity()) {
                        String tripId = desc.getTripId();
                        if (tripId != null) {
                            AgencyAndId shape = _entitySource.getObaShapeId(shapeId);
                            changesByTrip.computeIfAbsent(tripId, TripChange::new).newShapeId = shape;
                        }
                    }
                }
            } else if (Table.STOP_TIMES.equals(serviceChange.getTable())) {
                switch(serviceChange.getServiceChangeType()) {
                    case ADD:
                        for (AbstractFieldDescriptor desc : serviceChange.getAffectedField()) {
                            StopTimesFields stopTimesFields = (StopTimesFields) desc;
                            if (stopTimesFields.getTripId() != null) {
                                changesByTrip.computeIfAbsent(stopTimesFields.getTripId(), TripChange::new).insertedStops.add(stopTimesFields);
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
                                changesByTrip.computeIfAbsent(tripId, TripChange::new).modifiedStops.add(fields);
                            }
                        }
                        break;
                    case DELETE:
                        for (EntityDescriptor descriptor : serviceChange.getAffectedEntity()) {
                            String tripId = descriptor.getTripId();
                            String stopId = descriptor.getStopId();
                            if (tripId != null && stopId != null) {
                                changesByTrip.computeIfAbsent(tripId, TripChange::new).deletedStops.add(descriptor);
                            }
                        }
                        break;
                }
            } else {
                _log.info("not implemented: handling service change for table={}, type={}",
                        serviceChange.getTable(), serviceChange.getServiceChangeType());
            }

        }

        for (ServiceChange change : addedShapes) {
            if (handleAddShapesChange(change)) {
                _log.info("Added shape");
                nSuccess++;
            }
        }

        for (String tripId : changesByTrip.keySet()) {
            _log.info("Handling changes for trip {}", tripId);
            nSuccess += handleTripChanges(tripId, changesByTrip.get(tripId));
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

    private int handleTripChangesSeparately(String trip, TripChange change) {
        int nSuccess = 0;

        AgencyAndId tripId = _entitySource.getObaTripId(trip);
        TripEntryImpl tripEntry = (TripEntryImpl) _dao.getTripEntryForId(tripId);
        if (tripEntry == null) {
            return 0;
        }
        List<StopTimeEntry> stopTimes = new ArrayList<>(tripEntry.getStopTimes());
        AgencyAndId shapeId = change.newShapeId;

        // Removed stops
        for (EntityDescriptor descriptor : change.deletedStops) {
            AgencyAndId stopId = _entitySource.getObaStopId(descriptor.getStopId());
            if (_dao.deleteStopTime(tripId, stopId)) {
                nSuccess++;
            }
        }

        // Alter - only support changing arrival time/departure time

        for (StopTimesFields stopTimesFields : change.modifiedStops) {
            AgencyAndId stopId = _entitySource.getObaStopId(stopTimesFields.getStopId());
            int arrivalTime = stopTimesFields.getArrivalTime();
            int departureTime = stopTimesFields.getDepartureTime();
            Optional<StopTimeEntry> stopTimeSearch = stopTimes.stream().filter(ste -> stopId.equals(ste.getStop().getId())).findFirst();
            if (stopTimeSearch.isPresent()) {
                int originalArrivalTime = stopTimeSearch.get().getArrivalTime();
                int originalDepartureTime = stopTimeSearch.get().getDepartureTime();
                if (_dao.updateStopTime(tripId, stopId, originalArrivalTime, originalDepartureTime, arrivalTime, departureTime)) {
                    nSuccess++;
                }
            }

        }


        // Inserted stops
        for (StopTimesFields stopTimesFields : change.insertedStops) {
            AgencyAndId stopId = _entitySource.getObaStopId(stopTimesFields.getStopId());
            StopEntryImpl stopEntry = (StopEntryImpl) _dao.getStopEntryForId(stopId);
            int arrivalTime = stopTimesFields.getArrivalTime();
            int departureTime = stopTimesFields.getDepartureTime();
            Double shapeDistanceTravelled = stopTimesFields.getShapeDistTraveled();
            if (_dao.insertStopTime(tripId, stopId, arrivalTime, departureTime, shapeDistanceTravelled != null ? shapeDistanceTravelled : -999)) {
                nSuccess++;
            }
        }

        // Update shape
        if (shapeId != null) {
            if (_dao.updateShapeForTrip(tripEntry, shapeId)) {
                nSuccess++;
                _log.info("Success updated shape for trip {}", tripId);

            } else {
                _log.info("Error updating shape for trip {}", tripId);
            }
        }

        return nSuccess;
    }

    private int handleTripChanges(String trip, TripChange change) {
        int nSuccess = 0;

        AgencyAndId tripId = _entitySource.getObaTripId(trip);
        TripEntryImpl tripEntry = (TripEntryImpl) _dao.getTripEntryForId(tripId);
        if (tripEntry == null) {
            return 0;
        }
        List<StopTimeEntry> stopTimes = new ArrayList<>(tripEntry.getStopTimes());
        AgencyAndId shapeId = change.newShapeId;
        if (shapeId == null) {
            shapeId = tripEntry.getShapeId();
        } else {
            nSuccess++;
        }

        // Removed stops

        Set<AgencyAndId> stopsToRemove = new HashSet<>();
        for (EntityDescriptor descriptor : change.deletedStops) {
            AgencyAndId stopId = _entitySource.getObaStopId(descriptor.getStopId());
            stopsToRemove.add(stopId);
        }
        if (stopTimes.removeIf(ste -> stopsToRemove.contains(ste.getStop().getId()))) {
            nSuccess++;
        }

        // Alter - only support changing arrival time/departure time

        for (StopTimesFields stopTimesFields : change.modifiedStops) {
            AgencyAndId stopId = _entitySource.getObaStopId(stopTimesFields.getStopId());
            for (int i = 0; i < stopTimes.size(); i++) {
                StopTimeEntry stopTime = stopTimes.get(i);
                if (stopTime.getStop().getId().equals(stopId)) {
                    StopTimeEntryImpl newStopTime = new StopTimeEntryImpl(stopTime);
                    newStopTime.setArrivalTime(stopTimesFields.getArrivalTime());
                    newStopTime.setDepartureTime(stopTimesFields.getDepartureTime());
                    stopTimes.set(i, newStopTime);
                    nSuccess++;
                    break;
                }
            }
        }


        // Inserted stops

        for (StopTimesFields stopTimesFields : change.insertedStops) {
            AgencyAndId stopId = _entitySource.getObaStopId(stopTimesFields.getStopId());
            StopEntryImpl stopEntry = (StopEntryImpl) _dao.getStopEntryForId(stopId);
            Double shapeDistanceTravelled = stopTimesFields.getShapeDistTraveled();
            int arrivalTime = stopTimesFields.getArrivalTime();
            int departureTime = stopTimesFields.getDepartureTime();
            if (stopEntry != null) {
                StopTimeEntry newEntry = createStopTimeEntry(tripEntry, stopEntry, arrivalTime, departureTime, shapeDistanceTravelled == null ? -999 : shapeDistanceTravelled, -999);
                int insertPosition = -1;
                for (int i = stopTimes.size() - 1; i >= 0; i--) {
                    StopTimeEntry ste = tripEntry.getStopTimes().get(i);
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
                if (insertPosition >= 0) {
                    nSuccess++;
                    stopTimes.add(insertPosition, newEntry);
                }
            }
        }

        // call internal method
        boolean success = _dao.updateStopTimesForTrip(tripEntry, stopTimes, shapeId);
        if (!success) {
            _log.info("Error with trip {}", tripId);
        }

        return success ? nSuccess : 0;
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


    private boolean handleStopTimesChange(ServiceChange change) {
        switch(change.getServiceChangeType()) {
            case ADD:
                return handleAddStopTimes(change);
            case ALTER:
                return handleAlterStopTimes(change);
            case DELETE:
                return handleDeleteStopTimes(change);
            default:
                return false;
        }
    }

    private boolean handleAddStopTimes(ServiceChange change) {
        boolean success = true;
        for (AbstractFieldDescriptor abstractFieldDescriptor : change.getAffectedField()) {
            if (!(abstractFieldDescriptor instanceof StopTimesFields)) {
                return false;
            }
            StopTimesFields fields = (StopTimesFields) abstractFieldDescriptor;
            AgencyAndId tripId = _entitySource.getObaTripId(fields.getTripId());
            AgencyAndId stopId = _entitySource.getObaStopId(fields.getStopId());
            success &= _dao.insertStopTime(tripId, stopId, fields.getArrivalTime(), fields.getDepartureTime(), -1);
        }
        return success;
    }

    private boolean handleDeleteStopTimes(ServiceChange change) {
        boolean success = true;
        int nSuccess = 0, nTotal = 0;
        for (EntityDescriptor descriptor : change.getAffectedEntity()) {
            String bareTripId = descriptor.getTripId();
            String bareStopId = descriptor.getStopId();
            if (bareTripId == null || bareStopId == null) {
                _log.info("Service Change not fully applied; not enough info for stop_time");
                success = false;
                continue;
            }
            AgencyAndId tripId = _entitySource.getObaTripId(bareTripId);
            AgencyAndId stopId = _entitySource.getObaStopId(bareStopId);
            if (_dao.deleteStopTime(tripId, stopId)) {
                nSuccess++;
            } else {
                success = false;
            }
            nTotal++;
        }
        _log.info("Delete stop times: success in {} / {}", nSuccess, nTotal);
        return success;
    }

    private boolean handleAlterStopTimes(ServiceChange change) {
        boolean success = true;
        for (EntityDescriptor descriptor : change.getAffectedEntity()) {
            String bareTripId = descriptor.getTripId();
            String bareStopId = descriptor.getStopId();
            if (bareTripId == null || bareStopId == null) {
                _log.info("Service Change not fully applied; not enough info for stop_time");
                success = false;
                continue;
            }
            AgencyAndId tripId = _entitySource.getObaTripId(bareTripId);
            AgencyAndId stopId = _entitySource.getObaStopId(bareStopId);

            TripEntry trip = _dao.getTripEntryForId(tripId);
            success = false;
            if (trip != null) {
                // TODO: loop trips?
                for (StopTimeEntry stopTime : trip.getStopTimes()) {
                    if (stopTime.getStop().getId().equals(stopId)) {
                        StopTimesFields fields = (StopTimesFields) change.getAffectedField().iterator().next();
                        int arrivalTime = fields.getArrivalTime();
                        int departureTime = fields.getDepartureTime();
                        success = _dao.updateStopTime(tripId, stopId, stopTime.getArrivalTime(),
                                stopTime.getDepartureTime(), arrivalTime, departureTime);
                    }
                }
            }
            break;
        }
        return success;
    }

    private boolean handleAddShapesChange(ServiceChange change) {
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
            _shapePointsService.addShape(shapePoints);
        }

        return true;
    }

    private boolean handleTripsChange(ServiceChange change) {
        switch(change.getServiceChangeType()) {
            case ADD:
                _log.info("Add trip not supported");
            case ALTER:
                return handleAlterTrip(change);
            case DELETE:
                _log.info("Delete trip not supported");
            default:
                return false;
        }
    }

    private boolean handleAlterTrip(ServiceChange change) {
        TripsFields field = (TripsFields) change.getAffectedField().get(0);
        if (field.getShapeId() == null) {
            _log.info("Only alter trip shape supported");
            return false;
        }
        AgencyAndId shapeId = _entitySource.getObaShapeId(field.getShapeId());
        boolean success = true;
        for (EntityDescriptor entityDescriptor : change.getAffectedEntity()) {
            AgencyAndId tripId = _entitySource.getObaTripId(entityDescriptor.getTripId());
            TripEntry trip = _dao.getTripEntryForId(tripId);
            success &= _dao.updateShapeForTrip((TripEntryImpl) trip, shapeId);
        }
        return success;
    }

    private long getCurrentTime() {
        if (_time != -1)
            return _time;
        return new Date().getTime();
    }

    private LocalDate getCurrentDate() {
        return Instant.ofEpochMilli(getCurrentTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    class TripChange {
        TripChange(String tripId) {
            this.tripId = tripId;
        }

        String tripId;

        List<StopTimesFields> modifiedStops = new ArrayList<>();

        List<StopTimesFields> insertedStops = new ArrayList<>();

        List<EntityDescriptor> deletedStops = new ArrayList<>();

        AgencyAndId newShapeId;
    }
}

