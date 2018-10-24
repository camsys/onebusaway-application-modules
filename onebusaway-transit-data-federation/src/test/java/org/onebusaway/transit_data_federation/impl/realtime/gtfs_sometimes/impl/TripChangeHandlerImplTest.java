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

import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.MockEntityIdServiceImpl;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.AddTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChange;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ModifyTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChangeSet;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockTripEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;
import org.onebusaway.transit_data_federation.services.StopTimeService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeInstance;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.*;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.dateDescriptors;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.tripEntity;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.*;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.aid;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stopTime;

public class TripChangeHandlerImplTest {

    TripChangeHandlerImpl handler;

    private TripEntryImpl tripA, tripB, tripC, tripD, tripE, tripF, tripG, tripH, tripI, tripJ;
    private StopEntryImpl stopA, stopB, stopC;
    private StopTimeEntryImpl stopAA, stopBA, stopCA, stopCB, stopBB, stopAB, stopAC, stopBC, stopCC, stopAD, stopBD, stopCD;

    @Before
    public void setup() {
        handler = new TripChangeHandlerImpl();

        handler.setEntityIdService(new MockEntityIdServiceImpl());

        TimeServiceImpl timeService = new TimeServiceImpl();
        Calendar cal = Calendar.getInstance();
        cal.set(2018, Calendar.JULY, 1, 13, 0, 0);
        timeService.setTime(cal.getTimeInMillis());
        timeService.setTimeZone(ZoneId.of("America/New_York"));
        handler.setTimeService(timeService);

        StopTimeService stopTimeService = mock(StopTimeService.class);
        Date from = Date.from(LocalDate.of(2018, 7, 1).atStartOfDay(ZoneId.of("America/New_York")).toInstant());
        Date to = Date.from(LocalDate.of(2018, 7, 2).atStartOfDay(ZoneId.of("America/New_York")).toInstant());
        StopTimeInstance stopTime = mock(StopTimeInstance.class);
        BlockTripEntryImpl blockTrip = new BlockTripEntryImpl();
        TripEntryImpl tripEntry = new TripEntryImpl();
        tripEntry.setId(new AgencyAndId("1", "tripA"));
        blockTrip.setTrip(tripEntry);
        when(stopTime.getTrip()).thenReturn(blockTrip);
        when(stopTimeService.getStopTimeInstancesInTimeRange(new AgencyAndId("1", "stopA"), from, to))
                .thenReturn(Collections.singletonList(stopTime));
        handler.setStopTimeService(stopTimeService);

        // mocks for inserted stops
        TransitGraphDao dao = mock(TransitGraphDao.class);
        when(dao.getStopEntryForId(new AgencyAndId("1", "stopA"))).thenReturn(stop("1_stopA"));
        when(dao.getStopEntryForId(new AgencyAndId("1", "stopB"))).thenReturn(stop("1_stopB"));
        when(dao.getStopEntryForId(new AgencyAndId("1", "stopC"))).thenReturn(stop("1_stopC"));
        handler.setTransitGraphDao(dao);

        // Setup for stop-time modification tests:
        stopA = stop("a", 47.5, -122.5);
        stopB = stop("b", 47.6, -122.4);
        stopC = stop("c", 47.5, -122.3);

        tripA = trip("tripA", "serviceId");
        tripB = trip("tripB", "serviceId");
        tripC = trip("tripC", "serviceId");
        tripD = trip("tripD", "serviceId");
        tripE = trip("tripE", "serviceId");
        tripF = trip("tripF", "serviceId");
        tripG = trip("tripG", "serviceId");
        tripH = trip("tripH", "serviceId");
        tripI = trip("tripI", "serviceId");
        tripJ = trip("tripJ", "serviceId");

        stopAA = stopTime(0, stopA, tripA, 30, 90, 25);
        stopBA = stopTime(1, stopB, tripA, 120, 120, 100);
        stopCA = stopTime(2, stopC, tripA, 180, 210, 200);

        stopCB = stopTime(3, stopC, tripB, 240, 240, 300);
        stopBB = stopTime(4, stopB, tripB, 270, 270, 400);
        stopAB = stopTime(5, stopA, tripB, 300, 300, 500);

        stopAC = stopTime(6, stopA, tripC, 360, 360, 600);
        stopBC = stopTime(7, stopB, tripC, 390, 390, 700);
        stopCC = stopTime(8, stopC, tripC, 420, 420, 800);

        // trip C and D are the same but on different runs
        stopAD = stopTime(6, stopA, tripD, 360, 360, 600);
        stopBD = stopTime(7, stopB, tripD, 390, 390, 700);
        stopCD = stopTime(8, stopC, tripD, 420, 420, 800);

        when(dao.getStopEntryForId(aid("a"))).thenReturn(stopA);
        when(dao.getStopEntryForId(aid("b"))).thenReturn(stopB);
        when(dao.getStopEntryForId(aid("c"))).thenReturn(stopC);
        when(dao.getTripEntryForId(aid("tripA"))).thenReturn(tripA);

        NarrativeService narrativeService = mock(NarrativeService.class);
        when(narrativeService.getTripForId(aid("tripA"))).thenReturn(TripNarrative.builder().create());
        handler.setNarrativeService(narrativeService);

        // for creating the revert set, just pretend dao succeeded
        when(dao.deleteTripEntryForId(any())).thenReturn(true);
        when(dao.updateStopTimesForTrip(any(), anyList(), any())).thenReturn(true);
        when(dao.addTripEntry(any(), any())).thenReturn(true);
    }

    // TripChange creation

    @Test
    public void deleteStopTimeTripChange() {
        ServiceChange serviceChange = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null, // no affected field for delete
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        TripChange tripChange = getSingleTripChange(serviceChange);
        assertEquals("tripA", tripChange.getTripId());
        assertTrue(tripChange.getInsertedStops().isEmpty());
        assertTrue(tripChange.getModifiedStops().isEmpty());
        assertEquals(1, tripChange.getDeletedStops().size());
        assertEquals("stopA", tripChange.getDeletedStops().get(0).getStopId());
    }

    @Test
    public void addStopTimeTripChangeTest() {
        ServiceChange serviceChange = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("tripA", time(9, 0, 0), time(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        TripChange tripChange = getSingleTripChange(serviceChange);
        assertEquals("tripA", tripChange.getTripId());
        assertTrue(tripChange.getDeletedStops().isEmpty());
        assertTrue(tripChange.getModifiedStops().isEmpty());
        assertEquals(1, tripChange.getInsertedStops().size());
        assertEquals("stopA", tripChange.getInsertedStops().get(0).getStopId());
    }

    @Test
    public void alterStopTimeTripChangeTest() {
        ServiceChange serviceChange = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                stopTimesFieldsList("tripA", time(9, 0, 0), time(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        TripChange tripChange = getSingleTripChange(serviceChange);
        assertEquals("tripA", tripChange.getTripId());
        assertTrue(tripChange.getDeletedStops().isEmpty());
        assertTrue(tripChange.getInsertedStops().isEmpty());
        assertEquals(1, tripChange.getModifiedStops().size());
        assertEquals("stopA", tripChange.getModifiedStops().get(0).getStopId());
    }

    @Test
    public void multipleTripsTripChangeTest() {
        ServiceChange change1 = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null, // no affected field for delete
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        ServiceChange change2 = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopB")),
                null, // no affected field for delete
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        ServiceChange change3 = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripB", "stopA")),
                null, // no affected field for delete
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        List<TripChange> tripChanges = handler.getTripChanges(Arrays.asList(change1, change2, change3));
        assertEquals(2, tripChanges.size());
        tripChanges.sort(Comparator.comparing(TripChange::getTripId));
        TripChange changeA = tripChanges.get(0);
        assertEquals(2, changeA.getDeletedStops().size());
        TripChange changeB = tripChanges.get(1);
        assertEquals(1, changeB.getDeletedStops().size());
    }

    @Test
    public void addTripTripChangeTest() {
        ServiceChange addTrip = serviceChange(Table.TRIPS,
                ServiceChangeType.ADD,
                null,
                tripsFieldsList("tripA", "routeA", "serviceA", "shapeA", null),
                dateDescriptors(LocalDate.of(2018, 7, 1)));

        ServiceChange stop0 = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("tripA",
                        time(16, 5, 0), time(16, 5, 0),
                        "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 7, 1)));

        ServiceChange stop1 = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("tripA",
                        time(16, 5, 42), time(16, 5, 42),
                        "stopB", 1),
                dateDescriptors(LocalDate.of(2018, 7, 1)));

        ServiceChange stop2 = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("tripA",
                        time(16, 6, 38), time(16, 6, 38),
                        "stopC", 0),
                dateDescriptors(LocalDate.of(2018, 7, 1)));

        TripChange tripChange = getSingleTripChange(addTrip, stop1, stop0, stop2);
        assertEquals("tripA", tripChange.getTripId());
        assertTrue(tripChange.getDeletedStops().isEmpty());
        assertTrue(tripChange.getModifiedStops().isEmpty());
        assertEquals(3, tripChange.getInsertedStops().size());
        assertTrue(tripChange.isAdded());

        List<StopTimeEntry> newStops = new ArrayList<>();
        handler.computeNewStopTimes(tripChange, new TripEntryImpl(), newStops);
        assertEquals(3, newStops.size());
        assertEquals("1_stopA", newStops.get(0).getStop().getId().getId());
        assertEquals("1_stopB", newStops.get(1).getStop().getId().getId());
        assertEquals("1_stopC", newStops.get(2).getStop().getId().getId());
    }

    @Test
    public void deleteTripTripChange() {
        ServiceChange serviceChange = serviceChange(Table.TRIPS,
                ServiceChangeType.DELETE,
                Collections.singletonList(tripEntity("tripA")),
                null, // no affected field for delete
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        TripChange tripChange = getSingleTripChange(serviceChange);
        assertEquals("tripA", tripChange.getTripId());
        assertTrue(tripChange.getInsertedStops().isEmpty());
        assertTrue(tripChange.getModifiedStops().isEmpty());
        assertTrue(tripChange.getDeletedStops().isEmpty());
        assertTrue(tripChange.isDelete());
    }

    private TripChange getSingleTripChange(ServiceChange... changes) {
        List<TripChange> tripChanges = handler.getTripChanges(Arrays.asList(changes));
        assertEquals(1, tripChanges.size());
        return tripChanges.get(0);
    }

    // test creation with stop change
    @Test
    public void stopChangeLocationTest2() {
        ServiceChange change = serviceChange(Table.STOPS,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopEntity("stopA")),
                stopsFieldsList(null, 10d, 20d),
                dateDescriptors(LocalDate.of(2018, 7, 01)));
        List<TripChange> tripChanges = handler.getTripChanges(Arrays.asList(change));
        assertEquals(1, tripChanges.size());
        assertEquals("tripA", tripChanges.get(0).getTripId());
    }


    // Test stoptime logic. Moved from TransitGraphDaoImplTest

    @Test
    public void testDeleteStopTime() {
        assertEquals(3, tripA.getStopTimes().size());
        assertEquals(stopCA.getStop().getId().getId(), tripA.getStopTimes().get(2).getStop().getId().getId());
        TripChange change = deletedStopTripChange(tripA, stopCA);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(2, stopTimes.size());
        for (StopTimeEntry stop : stopTimes) {
            assertNotEquals(stopCA.getStop().getId().getId(), stop.getStop().getId().getId());
        }
    }

    @Test
    public void testUpdateStopTime() {

        assertEquals(3, tripA.getStopTimes().size());

        StopTimeEntry stopTimeEntry = tripA.getStopTimes().get(2);
        assertEquals(stopCA.getStop().getId().getId(), stopTimeEntry.getStop().getId().getId());
        assertEquals(180, stopTimeEntry.getArrivalTime());

        TripChange change = modifiedStopTripChange(tripA, stopCA, 185, 186);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);

        assertEquals(185, stopTimes.get(2).getArrivalTime());
        assertEquals(186, stopTimes.get(2).getDepartureTime());
    }

    @Test
    public void testAddStopTime0Dist() {
        // insert at 0 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, 15, 60, 10);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(0).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime1Dist() {
        // insert at 1 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, 45, 100, 75);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(1).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime2Dist() {
        // insert at 2 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, 150, 180, 150);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(2).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime3Dist() {
        // insert at 3 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, 220, 250, 300);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(3).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime0Arr() {
        // insert at 0 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, 15, 60, -1);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(0).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime1Arr() {
        // insert at 1 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, 45, 100, -1);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(1).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime2Arr() {
        // insert at 2 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, 150, 180, -1);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(2).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime3Arr() {
        // insert at 3 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, 220, 250, -1);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(3).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime0Dept() {
        // insert at 0 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, -1, 60, -1);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(0).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime1Dept() {
        // insert at 1 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, -1, 100, -1);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(1).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime2Dept() {
        // insert at 2 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, -1, 180, -1);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(2).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime3Dept() {
        // insert at 3 position
        assertEquals(3, tripA.getStopTimes().size());
        TripChange change = insertStopTripChange(tripA, stopAD, -1, 250, -1);
        List<StopTimeEntry> stopTimes = handler.computeNewStopTimes(change, tripA);
        assertEquals(4, stopTimes.size());
        assertEquals(stopTimes.get(3).getStop().getId(), stopAD.getStop().getId());
    }

    private static TripChange modifiedStopTripChange(TripEntryImpl trip, StopTimeEntry stop, int arrivalTime, int departureTime) {
        TripChange change = new TripChange(trip.getId().getId());
        change.addModifiedStop(stopTimesFieldDescriptor(trip.getId().getId(), arrivalTime, departureTime, stop.getStop().getId().getId(), 0));
        return change;
    }

    private static TripChange deletedStopTripChange(TripEntryImpl trip, StopTimeEntry stop) {
        TripChange change = new TripChange(trip.getId().getId());
        change.addDeletedStop(stopTimeEntity(trip.getId().getId(), stop.getStop().getId().getId()));
        return change;
    }

    private static TripChange insertStopTripChange(TripEntryImpl trip, StopTimeEntry stop, int arrivalTime, int departureTime, double shapeDistTravelled) {
        TripChange change = new TripChange(trip.getId().getId());
        change.addInsertedStop(stopTimesFieldDescriptor(trip.getId().getId(), arrivalTime, departureTime, stop.getStop().getId().getId(),
                0, null, null, shapeDistTravelled, null));
        return change;
    }

    // Test IntermediateTripChange -> TripChangeSet

    @Test
    public void testModifiedTripChange() {
        TripChange change = insertStopTripChange(tripA, stopAD, -1, 250, -1);
        TripChangeSet changeset = handler.getChangeset(Collections.singletonList(change));
        ModifyTrip trip = getSingleModifyTrip(changeset);
        assertEquals(tripA, trip.getTripEntry());
        assertEquals(4, trip.getStopTimes().size());
        assertEquals(tripA.getId(), trip.getTripId());

        // Revert
        TripChangeSet revertSet = handler.handleTripChanges(changeset);
        ModifyTrip revertTrip = getSingleModifyTrip(revertSet);
        assertEquals(3, revertTrip.getStopTimes().size());
        assertEquals(tripA.getId(), revertTrip.getTripId());
    }

    @Test
    public void testDeletedTripChange() {
        TripChange change = new TripChange("tripX");
        change.setDelete();
        TripChangeSet changeset = handler.getChangeset(Collections.singletonList(change));
        AgencyAndId tripId = getSingleDeleteTrip(changeset);
        assertEquals("1_tripX", tripId.toString());

        // Revert
        TripChangeSet revertSet = handler.handleTripChanges(changeset);
        AddTrip addTrip = getSingleAddTrip(revertSet);
        assertEquals("1_tripX", addTrip.getTripId().toString());
    }

    @Test
    public void testAddedTripChange() {
        TripChange change = new TripChange("tripX");
        change.setAddedTripsFields(tripsFields("tripX", null, "serviceA", "shapeA", "headsign"));
        TripChangeSet changeset = handler.getChangeset(Collections.singletonList(change));
        AddTrip addTrip = getSingleAddTrip(changeset);
        assertEquals("1_tripX", addTrip.getTripId().toString());

        // Revert
        TripChangeSet revertSet = handler.handleTripChanges(changeset);
        AgencyAndId deleteTrip = getSingleDeleteTrip(revertSet);
        assertEquals("1_tripX", deleteTrip.toString());
    }

    private ModifyTrip getSingleModifyTrip(TripChangeSet changeset) {
        assertTrue(changeset.getAddedTrips().isEmpty());
        assertTrue(changeset.getDeletedTrips().isEmpty());
        assertEquals(1, changeset.getModifiedTrips().size());
        return changeset.getModifiedTrips().get(0);
    }

    private AddTrip getSingleAddTrip(TripChangeSet changeset) {
        assertTrue(changeset.getModifiedTrips().isEmpty());
        assertTrue(changeset.getDeletedTrips().isEmpty());
        assertEquals(1, changeset.getAddedTrips().size());
        return changeset.getAddedTrips().get(0);
    }

    private AgencyAndId getSingleDeleteTrip(TripChangeSet changeset) {
        assertTrue(changeset.getAddedTrips().isEmpty());
        assertTrue(changeset.getModifiedTrips().isEmpty());
        assertEquals(1, changeset.getDeletedTrips().size());
        return changeset.getDeletedTrips().get(0);
    }
}
