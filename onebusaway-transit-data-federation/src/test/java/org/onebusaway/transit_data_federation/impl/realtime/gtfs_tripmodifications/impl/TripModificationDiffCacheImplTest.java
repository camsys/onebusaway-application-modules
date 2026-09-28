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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TripModificationDiffCacheImplTest {

    private static final String FEED_A = "feed-a";
    private static final String FEED_B = "feed-b";

    private static final AgencyAndId TRIP_1 = new AgencyAndId("agency", "trip-1");
    private static final AgencyAndId TRIP_2 = new AgencyAndId("agency", "trip-2");

    private TripModificationDiffCacheImpl _cache;

    @Before
    public void setUp() {
        _cache = new TripModificationDiffCacheImpl();
    }

    @Test
    public void testReplaceAll_isScopedToItsOwnFeed() {
        TripModificationDiff feedADiff = mock(TripModificationDiff.class);
        TripModificationDiff feedBDiff = mock(TripModificationDiff.class);

        _cache.replaceAll(FEED_A, singleEntry(TRIP_1, feedADiff));
        _cache.replaceAll(FEED_B, singleEntry(TRIP_2, feedBDiff));

        assertEquals("Feed A's entry should be unaffected by Feed B's replaceAll",
                feedADiff, _cache.get(TRIP_1));
        assertEquals(feedBDiff, _cache.get(TRIP_2));
        assertEquals(2, _cache.getAllById().size());
    }

    @Test
    public void testReplaceAll_doesNotClobberAnotherFeedsEntryForSameTrip() {
        // Regression test: a naive global replaceAll would wipe out Feed A's entry for TRIP_1
        // when Feed B is (re)applied, even though Feed B never mentions TRIP_1 at all.
        TripModificationDiff feedADiff = mock(TripModificationDiff.class);
        _cache.replaceAll(FEED_A, singleEntry(TRIP_1, feedADiff));

        _cache.replaceAll(FEED_B, singleEntry(TRIP_2, mock(TripModificationDiff.class)));

        assertEquals("Feed A's entry for TRIP_1 must survive Feed B's independent replaceAll",
                feedADiff, _cache.get(TRIP_1));
    }

    @Test
    public void testNoConflict_returnsTheOnlyFeedsDiff() {
        TripModificationDiff diff = mock(TripModificationDiff.class);
        _cache.registerFeedPriority(FEED_A, 0);
        _cache.replaceAll(FEED_A, singleEntry(TRIP_1, diff));

        assertEquals(diff, _cache.get(TRIP_1));
        assertTrue(_cache.getAll().contains(diff));
    }

    @Test
    public void testConflict_resolvedByPriority_lowerNumberWins() {
        TripModificationDiff highPriorityDiff = mock(TripModificationDiff.class);
        TripModificationDiff lowPriorityDiff = mock(TripModificationDiff.class);

        _cache.registerFeedPriority(FEED_A, 0); // higher priority (lower number wins)
        _cache.registerFeedPriority(FEED_B, 1);

        _cache.replaceAll(FEED_A, singleEntry(TRIP_1, highPriorityDiff));
        _cache.replaceAll(FEED_B, singleEntry(TRIP_1, lowPriorityDiff));

        assertEquals("The lower-priority-number feed (FEED_A) should win the conflict",
                highPriorityDiff, _cache.get(TRIP_1));
        assertEquals(highPriorityDiff, _cache.getAllById().get(TRIP_1));
        assertTrue(_cache.getAll().contains(highPriorityDiff));
        assertFalse(_cache.getAll().contains(lowPriorityDiff));
    }

    @Test
    public void testConflict_orderIndependent_priorityWinsRegardlessOfWhichFeedWroteFirst() {
        TripModificationDiff highPriorityDiff = mock(TripModificationDiff.class);
        TripModificationDiff lowPriorityDiff = mock(TripModificationDiff.class);

        _cache.registerFeedPriority(FEED_A, 5);
        _cache.registerFeedPriority(FEED_B, 1);

        // FEED_A (lower priority / higher number) writes second, but should still lose.
        _cache.replaceAll(FEED_B, singleEntry(TRIP_1, lowPriorityDiff));
        _cache.replaceAll(FEED_A, singleEntry(TRIP_1, highPriorityDiff));

        assertEquals(lowPriorityDiff, _cache.get(TRIP_1));
    }

    @Test
    public void testUnregisteredPriority_treatedAsLowestPriority() {
        TripModificationDiff registeredDiff = mock(TripModificationDiff.class);
        TripModificationDiff unregisteredDiff = mock(TripModificationDiff.class);

        _cache.registerFeedPriority(FEED_A, 0);
        // FEED_B never registered a priority

        _cache.replaceAll(FEED_A, singleEntry(TRIP_1, registeredDiff));
        _cache.replaceAll(FEED_B, singleEntry(TRIP_1, unregisteredDiff));

        assertEquals("A feed with an explicit priority should win over one with none registered",
                registeredDiff, _cache.get(TRIP_1));
    }

    @Test
    public void testClearSingleFeed_leavesOtherFeedsIntact() {
        _cache.replaceAll(FEED_A, singleEntry(TRIP_1, mock(TripModificationDiff.class)));
        TripModificationDiff feedBDiff = mock(TripModificationDiff.class);
        _cache.replaceAll(FEED_B, singleEntry(TRIP_2, feedBDiff));

        _cache.clear(FEED_A);

        assertNull(_cache.get(TRIP_1));
        assertEquals(feedBDiff, _cache.get(TRIP_2));
    }

    @Test
    public void testClearAll_removesEverything() {
        _cache.replaceAll(FEED_A, singleEntry(TRIP_1, mock(TripModificationDiff.class)));
        _cache.replaceAll(FEED_B, singleEntry(TRIP_2, mock(TripModificationDiff.class)));

        _cache.clear();

        assertTrue(_cache.getAll().isEmpty());
    }

    @Test
    public void testPutAndRemove_scopedToFeed() {
        TripModificationDiff diff = mock(TripModificationDiff.class);
        _cache.put(FEED_A, TRIP_1, diff);
        assertEquals(diff, _cache.get(TRIP_1));

        _cache.remove(FEED_A, TRIP_1);
        assertNull(_cache.get(TRIP_1));
    }

    private Map<AgencyAndId, TripModificationDiff> singleEntry(AgencyAndId tripId, TripModificationDiff diff) {
        Map<AgencyAndId, TripModificationDiff> map = new HashMap<>();
        map.put(tripId, diff);
        return map;
    }
}
