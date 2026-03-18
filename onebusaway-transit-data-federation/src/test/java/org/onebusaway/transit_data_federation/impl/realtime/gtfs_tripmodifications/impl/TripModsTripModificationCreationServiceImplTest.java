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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrips;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.StopEntryData;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.*;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TripModsTripModificationCreationServiceImplTest {

    @Mock
    private TransitGraphDao dao;

    @Mock
    private EntityIdService entityIdService;

    @Mock
    private NarrativeService narrativeService;

    @Mock
    private TripModsStopTimeFetcher stopTimeFetcher;

    @Mock
    private TripModsStopTimeEntryFactory stopTimeEntryFactory;

    @Mock
    private GtfsTripModificationsUtil util;

    @Mock
    private TripModsTimeService timeService;

    @Mock
    private TripEntryImpl tripEntry;

    @Mock
    private TripModificationDiffComputer tripModificationDiffComputer;

    @Mock
    private TripModificationDiffCache tripModificationDiffCache;

    private TripModsTripModificationCreationServiceImpl service;

    @Before
    public void setUp() {
        service = new TripModsTripModificationCreationServiceImpl(
                dao,
                entityIdService,
                narrativeService,
                stopTimeFetcher,
                stopTimeEntryFactory,
                util,
                timeService,
                tripModificationDiffComputer,
                tripModificationDiffCache
        );
    }

    // Dates Tests

    @Test
    public void isDateApplicableReturnsFalseWhenServiceDatesEmpty() {
        GtfsRealtime.TripModifications tripModifications =
                GtfsRealtime.TripModifications.newBuilder().build();

        assertFalse(service.isDateApplicable(tripModifications));
    }

    @Test
    public void isDateApplicableReturnsTrueWhenServiceDatesPresent() {
        GtfsRealtime.TripModifications tripModifications =
                GtfsRealtime.TripModifications.newBuilder()
                        .addServiceDates("20260310")
                        .build();

        assertTrue(service.isDateApplicable(tripModifications));
    }

    @Test
    public void isValidTripModificationReturnsFalseWhenNoServiceDates() {
        GtfsRealtime.TripModifications tripModifications =
                GtfsRealtime.TripModifications.newBuilder().build();

        assertFalse(service.isValidTripModification(tripModifications));
    }

    @Test
    public void isValidTripModificationReturnsTrueWhenServiceDatesPresent() {
        GtfsRealtime.TripModifications tripModifications = getTripModificationsWithValidDates();

        assertTrue(service.isValidTripModification(tripModifications));
    }

    @Test
    public void filterTripModificationsFiltersInvalidEntries() {
        GtfsRealtime.TripModifications invalid =
                GtfsRealtime.TripModifications.newBuilder().build();

        GtfsRealtime.TripModifications valid = getTripModificationsWithValidDates();

        List<GtfsRealtime.TripModifications> result =
                service.filterTripModifications(Arrays.asList(invalid, valid));

        assertEquals(1, result.size());
        assertEquals(valid, result.get(0));
    }

    private GtfsRealtime.TripModifications getTripModificationsWithValidDates() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate yesterday = today.minusDays(1);

        return GtfsRealtime.TripModifications.newBuilder()
                .addServiceDates(today.toString())
                .addServiceDates(tomorrow.toString())
                .addServiceDates(yesterday.toString())
                .build();
    }

    // Shapes

    @Test
    public void getSelectedTripsShapeIdReturnsResolvedShapeId() {
        AgencyAndId expectedShapeId = new AgencyAndId("MTA", "shape_1");

        GtfsRealtime.TripModifications.SelectedTrips selectedTrips =
                GtfsRealtime.TripModifications.SelectedTrips.newBuilder()
                        .setShapeId("shape_1")
                        .build();

        when(entityIdService.getShapeId("shape_1")).thenReturn(expectedShapeId);

        AgencyAndId result = service.getSelectedTripsShapeId(selectedTrips);

        assertEquals(expectedShapeId, result);
        verify(entityIdService).getShapeId("shape_1");
    }

    @Test
    public void getSelectedTripsShapeIdReturnsNullWhenMissingShapeId() {
        GtfsRealtime.TripModifications.SelectedTrips selectedTrips =
                GtfsRealtime.TripModifications.SelectedTrips.newBuilder().build();

        AgencyAndId result = service.getSelectedTripsShapeId(selectedTrips);

        assertNull(result);
        verify(entityIdService, never()).getShapeId(anyString());
    }

    @Test
    public void getModifiedShapeIdReturnsProvidedShapeId() {
        AgencyAndId providedShapeId = new AgencyAndId("MTA", "providedShape");

        AgencyAndId result = service.getModifiedShapeId(tripEntry, providedShapeId);

        assertEquals(providedShapeId, result);
    }

    @Test
    public void getModifiedShapeIdFallsBackToTripShapeId() {
        AgencyAndId tripShapeId = new AgencyAndId("MTA", "tripShape");

        when(tripEntry.getShapeId()).thenReturn(tripShapeId);

        AgencyAndId result = service.getModifiedShapeId(tripEntry, null);

        assertEquals(tripShapeId, result);
    }

    // Trips

    @Test
    public void getTripEntryReturnsNullWhenDaoReturnsNull() {
        AgencyAndId tripId = new AgencyAndId("MTA", "trip1");

        when(dao.getTripEntryForId(tripId)).thenReturn(null);

        TripEntryImpl result = service.getTripEntry(tripId);

        assertNull(result);
    }

    @Test
    public void getTripEntryReturnsTripEntryImpl() {
        AgencyAndId tripId = new AgencyAndId("MTA", "trip1");

        when(dao.getTripEntryForId(tripId)).thenReturn(tripEntry);

        TripEntryImpl result = service.getTripEntry(tripId);

        assertSame(tripEntry, result);
    }

    // Stop Times

    @Test
    public void adjustStopTimeUpdatesTimesAndSequence() {
        StopEntryImpl stop = new StopEntryImpl(new AgencyAndId("MTA", "stop1"), 40.0, -73.0);

        StopTimeEntryImpl original = new StopTimeEntryImpl();
        original.setStop(stop);
        original.setArrivalTime(100);
        original.setDepartureTime(110);
        original.setSequence(5);

        StopTimeEntry adjusted = service.adjustStopTime(original, -999, 30);

        assertNotSame(original, adjusted);
        assertEquals(130, adjusted.getArrivalTime());
        assertEquals(140, adjusted.getDepartureTime());
        assertEquals(-999, adjusted.getSequence());
        assertEquals(stop, adjusted.getStop());
    }

    @Test
    public void getAllStopTimesBeforeSelectionReturnsPrefix() {
        StopTimeEntry st1 = mock(StopTimeEntry.class);
        StopTimeEntry st2 = mock(StopTimeEntry.class);
        StopTimeEntry st3 = mock(StopTimeEntry.class);

        List<StopTimeEntry> original = Arrays.asList(st1, st2, st3);

        List<StopTimeEntry> result = service.getAllStopTimesBeforeSelection(2, original);

        assertEquals(2, result.size());
        assertSame(st1, result.get(0));
        assertSame(st2, result.get(1));
    }

    @Test
    public void getAllStopTimesAfterSelectionAppliesDelayToRemainingStops() {
        StopEntryImpl stop = new StopEntryImpl(new AgencyAndId("MTA", "stop1"), 40.0, -73.0);

        StopTimeEntryImpl st1 = new StopTimeEntryImpl();
        st1.setStop(stop);
        st1.setArrivalTime(100);
        st1.setDepartureTime(110);

        StopTimeEntryImpl st2 = new StopTimeEntryImpl();
        st2.setStop(stop);
        st2.setArrivalTime(200);
        st2.setDepartureTime(210);

        StopTimeEntryImpl st3 = new StopTimeEntryImpl();
        st3.setStop(stop);
        st3.setArrivalTime(300);
        st3.setDepartureTime(310);

        List<StopTimeEntry> result =
                service.getAllStopTimesAfterSelection(1, Arrays.asList(st1, st2, st3), 15);

        assertEquals(2, result.size());

        assertEquals(215, result.get(0).getArrivalTime());
        assertEquals(225, result.get(0).getDepartureTime());
        assertEquals(-999, result.get(0).getSequence());

        assertEquals(315, result.get(1).getArrivalTime());
        assertEquals(325, result.get(1).getDepartureTime());
        assertEquals(-999, result.get(1).getSequence());
    }

    @Test
    public void createStopTimeEntryDelegatesToFetcherAndFactory() {
        GtfsRealtime.ReplacementStop replacementStop =
                GtfsRealtime.ReplacementStop.newBuilder()
                        .setStopId("905019")
                        .setTravelTimeToStop(61)
                        .build();

        StopEntryData stopEntryData = new StopEntryData(null, 0, 0, null);
        StopTimeEntryImpl expected = new StopTimeEntryImpl();

        when(stopTimeFetcher.getStopEntry("905019")).thenReturn(stopEntryData);
        when(stopTimeEntryFactory.create(stopEntryData, tripEntry, 1000, -999, -999.0))
                .thenReturn(expected);

        StopTimeEntry result = service.createStopTimeEntry(
                replacementStop,
                1000,
                -999,
                -999.0,
                tripEntry
        );

        assertSame(expected, result);
        verify(stopTimeFetcher).getStopEntry("905019");
        verify(stopTimeEntryFactory).create(stopEntryData, tripEntry, 1000, -999, -999.0);
    }

    @Test
    public void createModifiedTripForExistingTripReturnsModifiedTrip() {
        AgencyAndId tripId = new AgencyAndId("MTA", "trip1");
        AgencyAndId shapeId = new AgencyAndId("MTA", "shape1");
        LocalDate serviceDate = LocalDate.of(2026, 3, 10);

        StopTimeEntry stopTime = mock(StopTimeEntry.class);
        List<StopTimeEntry> stopTimes = Collections.singletonList(stopTime);

        when(dao.getTripEntryForId(tripId)).thenReturn(tripEntry);
        when(tripEntry.getShapeId()).thenReturn(shapeId);
        when(tripEntry.getStopTimes()).thenReturn(stopTimes);
        when(util.getActiveServiceDateForTrip(tripEntry)).thenReturn(serviceDate);

        ModifiedTrip result = service.createModifiedTripForExistingTrip(tripId);

        assertNotNull(result);
        assertEquals(tripId, result.getTripId());
        assertEquals(shapeId, result.getShapeId());
        assertEquals(serviceDate, result.getServiceDate());
        assertEquals(stopTimes, result.getStopTimes());
        assertSame(tripEntry, result.getTripEntry());
        assertTrue(result.getModifications().isEmpty());
    }

    @Test
    public void createModifiedTripForExistingTripReturnsNullWhenTripMissing() {
        AgencyAndId tripId = new AgencyAndId("MTA", "missingTrip");

        when(dao.getTripEntryForId(tripId)).thenReturn(null);

        ModifiedTrip result = service.createModifiedTripForExistingTrip(tripId);

        assertNull(result);
    }

    @Test
    public void createModifiedTripsReturnsFailedIdsWhenTripsCannotBeResolved() {
        GtfsRealtime.TripModifications.SelectedTrips selectedTrips =
                GtfsRealtime.TripModifications.SelectedTrips.newBuilder()
                        .addTripIds("trip_a")
                        .addTripIds("trip_b")
                        .build();

        GtfsRealtime.TripModifications tripMods =
                GtfsRealtime.TripModifications.newBuilder()
                        .addServiceDates("20260310")
                        .addSelectedTrips(selectedTrips)
                        .build();

        AgencyAndId tripAId = new AgencyAndId("MTA", "trip_a");
        AgencyAndId tripBId = new AgencyAndId("MTA", "trip_b");

        when(entityIdService.getTripId("trip_a")).thenReturn(tripAId);
        when(entityIdService.getTripId("trip_b")).thenReturn(tripBId);
        when(dao.getTripEntryForId(tripAId)).thenReturn(null);
        when(dao.getTripEntryForId(tripBId)).thenReturn(null);
        when(util.parseServiceDates(Collections.singletonList("20260310")))
                .thenReturn(Collections.singleton(LocalDate.of(2026, 3, 10)));

        ModifiedTrips result = service.createModifiedTrips(Collections.singletonList(tripMods));

        assertNotNull(result);
        assertTrue(result.getModifiedTrips().isEmpty());
        assertTrue(result.getFailedModifiedTripIds().contains("trip_a"));
        assertTrue(result.getFailedModifiedTripIds().contains("trip_b"));
        assertEquals(2, result.getFailedModifiedCount());
        assertEquals(0, result.getSuccessfullyModifiedCount());
    }
}