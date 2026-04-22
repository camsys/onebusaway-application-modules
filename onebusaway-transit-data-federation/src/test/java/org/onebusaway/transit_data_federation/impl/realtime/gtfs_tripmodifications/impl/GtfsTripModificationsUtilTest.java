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
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GtfsTripModificationsUtilTest {

    @Mock
    private TimeService timeService;

    @Mock
    private BlockCalendarService blockCalendarService;

    @Mock
    private TripEntry tripEntry;

    @Mock
    private BlockEntry blockEntry;

    @Mock
    private BlockInstance blockInstance1;

    @Mock
    private BlockInstance blockInstance2;

    @Mock
    private StopTimeEntry stopTimeEntry0;

    @Mock
    private StopTimeEntry stopTimeEntry1;

    @Mock
    private StopTimeEntry stopTimeEntry2;

    @Mock
    private StopEntry stopEntry;

    private GtfsTripModificationsUtil util;

    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");
    private static final long NOW_MS = 1_700_000_000_000L; // fixed epoch

    @Before
    public void setUp() {
        util = new GtfsTripModificationsUtil(timeService, blockCalendarService);
        when(timeService.getCurrentTimeAsEpochMs()).thenReturn(NOW_MS);
        when(timeService.getTimeZone()).thenReturn(TEST_ZONE);
        when(tripEntry.getBlock()).thenReturn(blockEntry);
        when(blockEntry.getId()).thenReturn(new AgencyAndId("MTA", "B1"));
    }

    // -------------------------------------------------------------------------
    // getActiveServiceDateForTrip
    // -------------------------------------------------------------------------

    @Test
    public void getActiveServiceDateForTrip_returnsNullWhenNoBlocksActive() {
        when(blockCalendarService.getActiveBlocks(any(), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        LocalDate result = util.getActiveServiceDateForTrip(tripEntry);

        assertNull(result);
    }

    @Test
    public void getActiveServiceDateForTrip_returnsSingleBlockServiceDate() {
        // service date = 2023-11-14 in America/New_York
        long serviceDate = LocalDate.of(2023, 11, 14)
                .atStartOfDay(TEST_ZONE).toInstant().toEpochMilli();
        when(blockInstance1.getServiceDate()).thenReturn(serviceDate);
        when(blockCalendarService.getActiveBlocks(any(), anyLong(), anyLong()))
                .thenReturn(Collections.singletonList(blockInstance1));

        LocalDate result = util.getActiveServiceDateForTrip(tripEntry);

        assertEquals(LocalDate.of(2023, 11, 14), result);
    }

    @Test
    public void getActiveServiceDateForTrip_returnsMinimumServiceDateWhenMultipleBlocks() {
        long earlierDate = LocalDate.of(2023, 11, 13)
                .atStartOfDay(TEST_ZONE).toInstant().toEpochMilli();
        long laterDate = LocalDate.of(2023, 11, 14)
                .atStartOfDay(TEST_ZONE).toInstant().toEpochMilli();

        when(blockInstance1.getServiceDate()).thenReturn(laterDate);
        when(blockInstance2.getServiceDate()).thenReturn(earlierDate);
        when(blockCalendarService.getActiveBlocks(any(), anyLong(), anyLong()))
                .thenReturn(Arrays.asList(blockInstance1, blockInstance2));

        LocalDate result = util.getActiveServiceDateForTrip(tripEntry);

        assertEquals(LocalDate.of(2023, 11, 13), result);
    }

    @Test
    // Verifies the correct block ID and time window [now, now+1day] are passed to
    // blockCalendarService, independent of the return value (empty list is just to avoid NPE).
    public void getActiveServiceDateForTrip_queriesNowToNowPlusOneDay() {
        when(blockCalendarService.getActiveBlocks(any(), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        util.getActiveServiceDateForTrip(tripEntry);

        long expectedEnd = NOW_MS + TimeUnit.DAYS.toMillis(1);
        verify(blockCalendarService).getActiveBlocks(
                eq(new AgencyAndId("MTA", "B1")),
                eq(NOW_MS),
                eq(expectedEnd)
        );
    }

    // -------------------------------------------------------------------------
    // parseServiceDates
    // -------------------------------------------------------------------------

    @Test
    public void parseServiceDates_parsesValidDateStrings() {
        List<String> input = Arrays.asList("20231114", "20231201", "20240101");

        Set<LocalDate> result = util.parseServiceDates(input);

        assertEquals(3, result.size());
        assertTrue(result.contains(LocalDate.of(2023, 11, 14)));
        assertTrue(result.contains(LocalDate.of(2023, 12, 1)));
        assertTrue(result.contains(LocalDate.of(2024, 1, 1)));
    }

    @Test
    public void parseServiceDates_returnsEmptySetForEmptyInput() {
        Set<LocalDate> result = util.parseServiceDates(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void parseServiceDates_deduplicatesDuplicateDates() {
        List<String> input = Arrays.asList("20231114", "20231114");

        Set<LocalDate> result = util.parseServiceDates(input);

        assertEquals(1, result.size());
    }

    @Test(expected = Exception.class)
    public void parseServiceDates_throwsOnInvalidFormat() {
        util.parseServiceDates(Collections.singletonList("2023-11-14"));
    }

    // -------------------------------------------------------------------------
    // getReferenceTime
    // -------------------------------------------------------------------------

    @Test
    public void getReferenceTime_returnsFirstStopArrivalWhenStartIndexIsZero() {
        when(stopTimeEntry0.getArrivalTime()).thenReturn(3600); // 01:00:00
        List<StopTimeEntry> stopTimes = Arrays.asList(stopTimeEntry0, stopTimeEntry1, stopTimeEntry2);

        int result = util.getReferenceTime(stopTimes, 0);

        assertEquals(3600, result);
    }

    @Test
    public void getReferenceTime_returnsFirstStopArrivalWhenStartIndexIsOne() {
        when(stopTimeEntry0.getArrivalTime()).thenReturn(3600);
        List<StopTimeEntry> stopTimes = Arrays.asList(stopTimeEntry0, stopTimeEntry1, stopTimeEntry2);

        // startIndex == 1 is NOT > 1, so falls through to index 0
        int result = util.getReferenceTime(stopTimes, 1);

        assertEquals(3600, result);
    }

    @Test
    public void getReferenceTime_returnsPrecedingStopArrivalWhenStartIndexGreaterThanOne() {
        when(stopTimeEntry1.getArrivalTime()).thenReturn(7200); // 02:00:00
        List<StopTimeEntry> stopTimes = Arrays.asList(stopTimeEntry0, stopTimeEntry1, stopTimeEntry2);

        // startIndex == 2 → returns stopTimes[2-1] = stopTimes[1]
        int result = util.getReferenceTime(stopTimes, 2);

        assertEquals(7200, result);
    }

    @Test
    public void getReferenceTime_returnsCorrectEntryForHigherStartIndex() {
        when(stopTimeEntry2.getArrivalTime()).thenReturn(10800); // 03:00:00
        List<StopTimeEntry> stopTimes = Arrays.asList(stopTimeEntry0, stopTimeEntry1, stopTimeEntry2);

        // startIndex == 3 → returns stopTimes[3-1] = stopTimes[2]
        int result = util.getReferenceTime(stopTimes, 3);

        assertEquals(10800, result);
    }

    // -------------------------------------------------------------------------
    // calculateReplacementStopArrivalTime
    // -------------------------------------------------------------------------

    @Test
    public void calculateReplacementStopArrivalTime_addsTravelTimeWhenPresent() {
        GtfsRealtime.ReplacementStop replacementStop = GtfsRealtime.ReplacementStop.newBuilder()
                .setTravelTimeToStop(300)
                .build();

        int result = util.calculateReplacementStopArrivalTime(replacementStop, 3600);

        assertEquals(3900, result);
    }

    @Test
    public void calculateReplacementStopArrivalTime_returnsReferenceTimeWhenNoTravelTime() {
        GtfsRealtime.ReplacementStop replacementStop = GtfsRealtime.ReplacementStop.newBuilder()
                .build();

        int result = util.calculateReplacementStopArrivalTime(replacementStop, 3600);

        assertEquals(3600, result);
    }

    @Test
    public void calculateReplacementStopArrivalTime_handlesZeroTravelTime() {
        GtfsRealtime.ReplacementStop replacementStop = GtfsRealtime.ReplacementStop.newBuilder()
                .setTravelTimeToStop(0)
                .build();

        int result = util.calculateReplacementStopArrivalTime(replacementStop, 3600);

        // hasTravelTimeToStop() is true when explicitly set to 0
        assertEquals(3600, result);
    }

    // -------------------------------------------------------------------------
    // areIdsEqual
    // -------------------------------------------------------------------------

    @Test
    public void areIdsEqual_trueWhenIdPartMatches() {
        AgencyAndId agencyAndId = new AgencyAndId("MTA", "STOP_1");

        assertTrue(util.areIdsEqual(agencyAndId, "STOP_1"));
    }

    @Test
    public void areIdsEqual_trueWhenFullAgencyAndIdStringMatches() {
        AgencyAndId agencyAndId = new AgencyAndId("MTA", "STOP_1");

        assertTrue(util.areIdsEqual(agencyAndId, "MTA_STOP_1"));
    }

    @Test
    public void areIdsEqual_falseWhenNeitherMatches() {
        AgencyAndId agencyAndId = new AgencyAndId("MTA", "STOP_1");

        assertFalse(util.areIdsEqual(agencyAndId, "STOP_999"));
    }

    @Test
    public void areIdsEqual_falseWhenOnlyAgencyMatches() {
        AgencyAndId agencyAndId = new AgencyAndId("MTA", "STOP_1");

        assertFalse(util.areIdsEqual(agencyAndId, "MTA"));
    }

    // -------------------------------------------------------------------------
    // findStopTimeIndexForSelector
    // -------------------------------------------------------------------------

    @Test
    public void findStopTimeIndexForSelector_findsByStopSequence() {
        when(stopTimeEntry0.getGtfsSequence()).thenReturn(10);
        when(stopTimeEntry1.getGtfsSequence()).thenReturn(20);
        List<StopTimeEntry> stopTimes = Arrays.asList(stopTimeEntry0, stopTimeEntry1, stopTimeEntry2);

        GtfsRealtime.StopSelector selector = GtfsRealtime.StopSelector.newBuilder()
                .setStopSequence(20)
                .build();

        int index = util.findStopTimeIndexForSelector(stopTimes, selector);

        assertEquals(1, index);
    }

    @Test
    public void findStopTimeIndexForSelector_findsByStopId() {
        AgencyAndId stopId = new AgencyAndId("MTA", "S99");
        when(stopEntry.getId()).thenReturn(stopId);
        when(stopTimeEntry0.getStop()).thenReturn(mock(StopEntry.class));
        when(stopTimeEntry0.getStop().getId()).thenReturn(new AgencyAndId("MTA", "S10"));
        when(stopTimeEntry1.getStop()).thenReturn(stopEntry);
        List<StopTimeEntry> stopTimes = Arrays.asList(stopTimeEntry0, stopTimeEntry1);

        GtfsRealtime.StopSelector selector = GtfsRealtime.StopSelector.newBuilder()
                .setStopId("S99")
                .build();

        int index = util.findStopTimeIndexForSelector(stopTimes, selector);

        assertEquals(1, index);
    }

    @Test
    public void findStopTimeIndexForSelector_findsByStopIdFullAgencyAndIdString() {
        AgencyAndId stopId = new AgencyAndId("MTA", "S99");
        when(stopEntry.getId()).thenReturn(stopId);
        when(stopTimeEntry0.getStop()).thenReturn(stopEntry);
        List<StopTimeEntry> stopTimes = Collections.singletonList(stopTimeEntry0);

        GtfsRealtime.StopSelector selector = GtfsRealtime.StopSelector.newBuilder()
                .setStopId("MTA_S99")
                .build();

        int index = util.findStopTimeIndexForSelector(stopTimes, selector);

        assertEquals(0, index);
    }

    @Test
    public void findStopTimeIndexForSelector_returnsNegativeOneWhenSequenceNotFound() {
        when(stopTimeEntry0.getGtfsSequence()).thenReturn(10);
        List<StopTimeEntry> stopTimes = Collections.singletonList(stopTimeEntry0);

        GtfsRealtime.StopSelector selector = GtfsRealtime.StopSelector.newBuilder()
                .setStopSequence(999)
                .build();

        int index = util.findStopTimeIndexForSelector(stopTimes, selector);
        assertEquals(-1, index);
    }

    @Test
    public void findStopTimeIndexForSelector_returnsNegativeOneWhenStopIdNotFound() {
        AgencyAndId stopId = new AgencyAndId("MTA", "S10");
        when(stopEntry.getId()).thenReturn(stopId);
        when(stopTimeEntry0.getStop()).thenReturn(stopEntry);
        List<StopTimeEntry> stopTimes = Collections.singletonList(stopTimeEntry0);

        GtfsRealtime.StopSelector selector = GtfsRealtime.StopSelector.newBuilder()
                .setStopId("S999")
                .build();

        int index = util.findStopTimeIndexForSelector(stopTimes, selector);
        assertEquals(-1, index);
    }

    @Test
    public void findStopTimeIndexForSelector_stopSequenceTakesPriorityOverStopId() {
        // Both sequence and ID are set — sequence match is attempted first
        when(stopTimeEntry0.getGtfsSequence()).thenReturn(10);
        List<StopTimeEntry> stopTimes = Arrays.asList(stopTimeEntry0, stopTimeEntry1);

        GtfsRealtime.StopSelector selector = GtfsRealtime.StopSelector.newBuilder()
                .setStopSequence(10)
                .setStopId("some-other-id")
                .build();

        int index = util.findStopTimeIndexForSelector(stopTimes, selector);

        assertEquals(0, index);
    }

    // -------------------------------------------------------------------------
    // toLocalDate
    // -------------------------------------------------------------------------

    @Test
    public void toLocalDate_convertsEpochMillisToCorrectDate() {
        // 2023-11-14T00:00:00 America/New_York
        long epochMs = LocalDate.of(2023, 11, 14)
                .atStartOfDay(TEST_ZONE).toInstant().toEpochMilli();

        LocalDate result = util.toLocalDate(epochMs, TEST_ZONE);

        assertEquals(LocalDate.of(2023, 11, 14), result);
    }

    @Test
    public void toLocalDate_handlesTimezoneCorrectly() {
        // Midnight UTC on 2023-11-14 is still 2023-11-13 in New York (UTC-5)
        long midnightUtc = LocalDate.of(2023, 11, 14)
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();

        LocalDate nyResult = util.toLocalDate(midnightUtc, TEST_ZONE);

        assertEquals(LocalDate.of(2023, 11, 13), nyResult);
    }

    // -------------------------------------------------------------------------
    // toServiceDate
    // -------------------------------------------------------------------------

    @Test
    public void toServiceDate_convertsLocalDateToServiceDate() {
        LocalDate localDate = LocalDate.of(2023, 11, 14);

        ServiceDate result = util.toServiceDate(localDate);

        assertEquals(2023, result.getYear());
        assertEquals(11, result.getMonth());
        assertEquals(14, result.getDay());
    }

    @Test
    public void toServiceDate_handlesFirstDayOfYear() {
        ServiceDate result = util.toServiceDate(LocalDate.of(2024, 1, 1));

        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonth());
        assertEquals(1, result.getDay());
    }

    @Test
    public void toServiceDate_handlesLastDayOfYear() {
        ServiceDate result = util.toServiceDate(LocalDate.of(2023, 12, 31));

        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonth());
        assertEquals(31, result.getDay());
    }
}