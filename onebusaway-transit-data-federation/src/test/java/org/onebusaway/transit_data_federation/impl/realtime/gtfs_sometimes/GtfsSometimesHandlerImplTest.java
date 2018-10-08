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

import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import com.camsys.transit.servicechange.field_descriptors.StopTimesFields;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChange;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.*;

public class GtfsSometimesHandlerImplTest {

    private GtfsSometimesHandlerImpl handler;

    @Before
    public void setup() {
        handler = new GtfsSometimesHandlerImpl();
        //handler.setTransitGraphDao(dao);
        handler.setAgencyId("1");
        Calendar cal = Calendar.getInstance();
        cal.set(2018, Calendar.JULY, 1, 13, 0, 0);
        handler.setTime(cal.getTimeInMillis());
    }

    // Validation and date range tests

    @Test
    public void deleteStopTimesValidationTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null, // no affected field for delete
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        assertTrue(handler.isServiceChangeOk(change));
    }

    @Test
    public void deleteStopTimesAffectedFieldCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                Collections.singletonList(new StopTimesFields()),
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void deleteStopTimesDateCardinalityValidationTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                Collections.emptyList());
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void deleteStopTimesWrongDateTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                dateDescriptors(LocalDate.of(2018, 7, 2)));
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void deleteStopTimesDateRangeTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                dateDescriptorsRange(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 8, 1)));
        assertTrue(handler.isServiceChangeOk(change));
    }

    @Test
    public void deleteStopTimesWrongDateRangeTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                dateDescriptorsRange(LocalDate.of(2018, 7, 2), LocalDate.of(2018, 8, 1)));
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void deleteStopTimesReadsListTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Arrays.asList(
                        stopTimeEntity("tripA", "stopA"),
                        stopTimeEntity("tripB", "stopB")),
                null, // no affected field for delete
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertTrue(handler.isServiceChangeOk(change));
    }

    @Test
    public void addStopTimesAffectedEntityCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                Arrays.asList(stopTimeEntity("tripA", "stopA")),
                stopTimesFieldsList("tripA", time(9, 0, 0), time(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void addStopTimesTestAffectedDateCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("tripA", time(9, 0, 0), time(9, 0, 0), "stopA", 0),
                Collections.emptyList());
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void addStopTimesValidationTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("tripA", time(9, 0, 0), time(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertTrue(handler.isServiceChangeOk(change));
    }

    @Test
    public void alterStopTimesTestAffectedEntityCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                null,
                stopTimesFieldsList("tripA", time(9, 0, 0), time(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void alterStopTimesTestAffectedFieldCardinalityTest0() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void alterStopTimesTestAffectedFieldCardinalityTest1() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                Arrays.asList(stopTimesFieldDescriptor(null, null, null, null, -1),
                        stopTimesFieldDescriptor(null, null, null, null, -1)),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertFalse(handler.isServiceChangeOk(change));
    }

    @Test
    public void alterStopTimesTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                stopTimesFieldsList("tripA", time(9, 0, 0), time(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertTrue(handler.isServiceChangeOk(change));
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
        List<TripChange> tripChanges = handler.getAllTripChanges(Arrays.asList(change1, change2, change3));
        assertEquals(2, tripChanges.size());
        tripChanges.sort(Comparator.comparing(TripChange::getTripId));
        TripChange changeA = tripChanges.get(0);
        assertEquals(2, changeA.getDeletedStops().size());
        TripChange changeB = tripChanges.get(1);
        assertEquals(1, changeB.getDeletedStops().size());
    }

    private TripChange getSingleTripChange(ServiceChange... changes) {
        List<TripChange> tripChanges = handler.getAllTripChanges(Arrays.asList(changes));
        assertEquals(1, tripChanges.size());
        return tripChanges.get(0);
    }
}
