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
import com.camsys.transit.servicechange.field_descriptors.StopTimesFields;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.*;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.aid;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.time;

public class GtfsSometimesHandlerImplTest {

    private GtfsSometimesHandlerImpl handler;

    private TimeServiceImpl timeService;

    @Before
    public void before() {
        handler = new GtfsSometimesHandlerImpl();
        timeService = new TimeServiceImpl();
        timeService.setTimeZone(ZoneId.of("America/New_York"));
        timeService.setTime("2018-11-12 09:00");
        handler.setTimeService(timeService);
    }

    // Validation tests

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

    // Test whether changes should be applied

    @Test
    public void shouldApplyChangesTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                stopTimesFieldsList("tripA", time(9, 0, 0), time(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));

        // apply changes if timestamp = -1
        assertTrue(handler.shouldApplyChanges(-1, Arrays.asList(change)));

        // apply changes if regular timestamp, no state
        assertTrue(handler.shouldApplyChanges(1542033551, Arrays.asList(change)));

        // do not apply change if no changes, no state
        assertFalse(handler.shouldApplyChanges(1542033551, Collections.emptyList()));

        // do not apply change if time is equal to last updated time (already did)
        handler.setLastUpdatedTimestamp(1542033551);
        handler.setReapplyTime(LocalDateTime.of(2018, 11, 12, 13, 0, 0)); // 11/12 1:00pm
        assertFalse(handler.shouldApplyChanges(1542033551, Arrays.asList(change)));

        // do not apply change if time is before last updated time (error)
        assertFalse(handler.shouldApplyChanges(1542033550, Arrays.asList(change)));

        // apply change if time is after last updated time
        assertTrue(handler.shouldApplyChanges(1542033552, Arrays.asList(change)));

        // apply change if time is after last updated time, even if no changes
        assertTrue(handler.shouldApplyChanges(1542033552, Collections.emptyList()));

        // apply change if time is equal to last updated time but current time is after reapply time.
        timeService.setTime("2018-11-12 13:01");
        assertTrue(handler.shouldApplyChanges(1542033551, Arrays.asList(change)));
    }

    @Test
    public void calculateReapplyTime() {
        // current time is 2018-11-12 09:00

        // If all trip changes are for the current date, reapply time is midnight
        // If there is a trip for a previous service date that ends in the future, reapply time is that end time.

        TripChange endsToday = TestTripChangeImpl.create(aid("tripA"), LocalDate.of(2018, 11, 12),
                LocalDateTime.of(2018, 11, 12, 13, 0, 0));
        TripChange endsTomorrow = TestTripChangeImpl.create(aid("tripB"), LocalDate.of(2018, 11, 12),
                LocalDateTime.of(2018, 11, 13, 1, 0, 0));
        TripChange endsTodayFromYesterday1 = TestTripChangeImpl.create(aid("tripC"), LocalDate.of(2018, 11, 11),
                LocalDateTime.of(2018, 11, 12, 13, 0, 0));
        TripChange endsTodayFromYesterday2 = TestTripChangeImpl.create(aid("tripC"), LocalDate.of(2018, 11, 11),
                LocalDateTime.of(2018, 11, 12, 14, 0, 0));

        assertEquals(LocalDateTime.of(2018, 11, 13, 0, 0, 0), handler.getReapplyTime(Arrays.asList(endsToday, endsTomorrow)));
        assertEquals(LocalDateTime.of(2018, 11, 12, 13, 0, 0), handler.getReapplyTime(Arrays.asList(endsToday, endsTomorrow,
                endsTodayFromYesterday1, endsTodayFromYesterday2)));
    }

    private static class TestTripChangeImpl implements TripChange {
        private AgencyAndId tripId;

        private LocalDate serviceDate;

        private LocalDateTime endTime;

        @Override
        public AgencyAndId getTripId() {
            return tripId;
        }

        @Override
        public LocalDate getServiceDate() {
            return serviceDate;
        }

        @Override
        public LocalDateTime getEndTime() {
            return endTime;
        }

        private static TestTripChangeImpl create(AgencyAndId tripId, LocalDate serviceDate, LocalDateTime endTime) {
            TestTripChangeImpl change = new TestTripChangeImpl();
            change.tripId = tripId;
            change.serviceDate = serviceDate;
            change.endTime = endTime;
            return change;
        }
    }
}
