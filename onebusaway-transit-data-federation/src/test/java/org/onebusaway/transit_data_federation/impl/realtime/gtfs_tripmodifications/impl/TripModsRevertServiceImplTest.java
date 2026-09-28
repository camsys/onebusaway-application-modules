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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShapesResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTripsResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsShapeUpdateService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsTripModificationUpdateService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TripModsRevertServiceImplTest {

    private static final String FEED_A = "feed-a";
    private static final String FEED_B = "feed-b";

    @Mock
    private TripModsTripModificationUpdateService _tripModificationUpdateService;

    @Mock
    private TripModsShapeUpdateService _tripModsShapeUpdateService;

    private TripModsRevertServiceImpl _revertService;

    @Before
    public void setUp() {
        _revertService = new TripModsRevertServiceImpl(_tripModificationUpdateService, _tripModsShapeUpdateService);
        when(_tripModificationUpdateService.updateTrips(anyList())).thenReturn(new ModifiedTripsResult());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRevertPreviousChanges_onlyRevertsThatFeedsTrips() {
        ModifiedTrip feedATrip = mock(ModifiedTrip.class);
        ModifiedTripsResult feedAResult = new ModifiedTripsResult();
        feedAResult.addOriginalTrip(feedATrip);

        ModifiedTrip feedBTrip = mock(ModifiedTrip.class);
        ModifiedTripsResult feedBResult = new ModifiedTripsResult();
        feedBResult.addOriginalTrip(feedBTrip);

        _revertService.setLastKnownTripModificationResults(FEED_A, feedAResult);
        _revertService.setLastKnownTripModificationResults(FEED_B, feedBResult);

        _revertService.revertPreviousChanges(FEED_A);

        ArgumentCaptor<List<ModifiedTrip>> captor = ArgumentCaptor.forClass(List.class);
        verify(_tripModificationUpdateService).updateTrips(captor.capture());
        assertEquals("Only FEED_A's original trips should be passed to updateTrips when reverting FEED_A",
                List.of(feedATrip), captor.getValue());

        assertEquals("FEED_B's last-known trip results must survive FEED_A's revert",
                feedBResult, _revertService.getLastKnownTripModificationResults(FEED_B));
    }

    @Test
    public void testRevertPreviousChanges_onlyRemovesThatFeedsShapes() {
        AgencyAndId feedAShape = new AgencyAndId("agency", "feed-a-shape");
        AddedShapesResult feedAShapes = new AddedShapesResult();
        feedAShapes.addSuccessfullyUpdatedShapeId(feedAShape);

        AgencyAndId feedBShape = new AgencyAndId("agency", "feed-b-shape");
        AddedShapesResult feedBShapes = new AddedShapesResult();
        feedBShapes.addSuccessfullyUpdatedShapeId(feedBShape);

        _revertService.setLastKnownShapeResults(FEED_A, feedAShapes);
        _revertService.setLastKnownShapeResults(FEED_B, feedBShapes);

        _revertService.revertPreviousChanges(FEED_A);

        verify(_tripModsShapeUpdateService).removeShapes(List.of(feedAShape));
        assertEquals("FEED_B's last-known shape results must survive FEED_A's revert",
                feedBShapes, _revertService.getLastKnownShapeResults(FEED_B));
    }

    @Test
    public void testRevertPreviousChanges_noPriorState_isNoOp() {
        _revertService.revertPreviousChanges(FEED_A);

        verify(_tripModificationUpdateService, never()).updateTrips(anyList());
        verify(_tripModsShapeUpdateService, never()).removeShapes(anyList());
    }

    @Test
    public void testClearAll_removesEveryFeedsState() {
        _revertService.setLastKnownShapeResults(FEED_A, new AddedShapesResult());
        _revertService.setLastKnownTripModificationResults(FEED_A, new ModifiedTripsResult());

        _revertService.clearAll();

        assertEquals(null, _revertService.getLastKnownShapeResults(FEED_A));
        assertEquals(null, _revertService.getLastKnownTripModificationResults(FEED_A));
    }
}
