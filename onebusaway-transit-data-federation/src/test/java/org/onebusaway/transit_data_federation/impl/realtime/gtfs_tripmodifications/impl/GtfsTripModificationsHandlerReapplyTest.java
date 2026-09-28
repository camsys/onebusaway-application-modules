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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl.TimeServiceImpl;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.TripModificationsChanges;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GtfsTripModificationsHandlerReapplyTest {

    @Mock
    private TimeService _timeService;

    private TripModificationsChanges _changes = new TripModificationsChanges();

    @InjectMocks
    private GtfsTripModificationsHandlerImpl _handler;

    private static final String FEED_A = "feed-a";
    private static final String FEED_B = "feed-b";
    private static final byte[] HASH = {1, 2, 3};
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);
    private static final LocalDateTime REAPPLY_TIME = TODAY.plusDays(1).atTime(3, 0);

    @Before
    public void setUp() throws Exception {
        _changes.setHash(HASH);
        //when(_timeService.getCurrentDate()).thenReturn(TODAY);

        // Simulate a prior successful feed application for FEED_A
        setMapField("_lastKnownHashByFeed", FEED_A, HASH);
        setMapField("_reapplyTimeByFeed", FEED_A, REAPPLY_TIME);
    }

    @Test
    public void testReapplyTime_null_doesNotEnterBranch() {
        clearMapEntry("_reapplyTimeByFeed", FEED_A);
        assertFalse("Should not apply when reapplyTime is null",
                _handler.shouldApplyChanges(FEED_A, _changes));
    }

    @Test
    public void testCurrentTime_beforeReapplyTime_doesNotEnterBranch() {
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME.minusMinutes(1));

        assertFalse("Should not apply when current time is before reapply time",
                _handler.shouldApplyChanges(FEED_A, _changes));
    }

    @Test
    public void testCurrentTime_exactlyAtReapplyTime_doesNotEnterBranch() {
        // isAfter is exclusive — exactly at reapply time should not enter the branch
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME);

        assertFalse("Should not apply when current time equals reapply time",
                _handler.shouldApplyChanges(FEED_A, _changes));
    }

    @Test
    public void testCurrentTime_oneSecondAfterReapplyTime_shouldApply() {
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME.plusSeconds(1));

        assertTrue("Should apply when current time is just after reapply time",
                _handler.shouldApplyChanges(FEED_A, _changes));
    }

    @Test
    public void testCurrentTime_wellAfterReapplyTime_shouldApply() {
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME.plusHours(5));

        assertTrue("Should apply when current time is well after reapply time",
                _handler.shouldApplyChanges(FEED_A, _changes));
    }

    @Test
    public void testUnknownFeed_firstUpdate_withActualChanges_shouldApply() {
        // FEED_B has no tracked state at all, unlike FEED_A set up in setUp(). Unlike the shared
        // _changes fixture (hash only, no content), this one has an actual stop change so
        // hasChanges() is true.
        TripModificationsChanges changesWithContent = new TripModificationsChanges();
        changesWithContent.setHash(HASH);
        changesWithContent.addStop("entity-1", com.google.transit.realtime.GtfsRealtime.Stop.newBuilder().build());

        assertTrue("An unrecognized feed with actual changes should be treated as a first update",
                _handler.shouldApplyChanges(FEED_B, changesWithContent));
    }

    @Test
    public void testUnknownFeed_firstUpdate_emptyFeed_shouldNotApply() {
        // The shared _changes fixture only carries a hash, no shapes/stops/tripMods, so
        // hasChanges() is false — an empty feed should be ignored even on first sight.
        assertFalse("An unrecognized feed with no actual changes should not be applied",
                _handler.shouldApplyChanges(FEED_B, _changes));
    }

    @Test
    public void testOneFeedsReapplyTime_doesNotForceAnotherFeedToReapply() {
        // FEED_B has the same hash as FEED_A but its own reapply time has not yet passed
        setMapField("_lastKnownHashByFeed", FEED_B, HASH);
        setMapField("_reapplyTimeByFeed", FEED_B, REAPPLY_TIME.plusHours(1));

        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME.plusMinutes(30));

        assertTrue("FEED_A's reapply time has passed and should reapply",
                _handler.shouldApplyChanges(FEED_A, _changes));
        assertFalse("FEED_B's own (later) reapply time has not passed and should not reapply",
                _handler.shouldApplyChanges(FEED_B, _changes));
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private <V> void setMapField(String fieldName, String feedId, V value) {
        Map<String, V> map = (Map<String, V>) getField(fieldName);
        map.put(feedId, value);
    }

    private void clearMapEntry(String fieldName, String feedId) {
        ((Map<?, ?>) getField(fieldName)).remove(feedId);
    }

    private Object getField(String fieldName) {
        try {
            java.lang.reflect.Field field =
                    GtfsTripModificationsHandlerImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(_handler);
            if (value == null) {
                value = new ConcurrentHashMap<>();
                field.set(_handler, value);
            }
            return value;
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field: " + fieldName, e);
        }
    }
}