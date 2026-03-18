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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.StopEntryData;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TripModsStopTimeFetcherImplTest {

    @Mock private TransitGraphDao dao;
    @Mock private NarrativeService narrativeService;
    @Mock private EntityIdService entityIdService;
    @Mock private StopEntry stopEntry;

    private TripModsStopTimeFetcherImpl fetcher;

    private static final String RAW_STOP_ID = "1_stopA";
    private static final AgencyAndId STOP_ID = new AgencyAndId("1", "stopA");
    private static final double STOP_LAT = 47.6062;
    private static final double STOP_LON = -122.3321;
    private static final String STOP_NAME = "Main St & 1st Ave";

    // Built with the real builder — no mock needed
    private static final StopNarrative STOP_NARRATIVE = StopNarrative.builder()
            .setName(STOP_NAME)
            .create();

    @Before
    public void setUp() {
        fetcher = new TripModsStopTimeFetcherImpl(dao, narrativeService, entityIdService);
    }

    @Test
    public void getStopEntry_validStop_returnsPopulatedStopEntryData() {
        when(entityIdService.getStopId(RAW_STOP_ID)).thenReturn(STOP_ID);
        when(dao.getStopEntryForId(STOP_ID)).thenReturn(stopEntry);
        when(stopEntry.getStopLat()).thenReturn(STOP_LAT);
        when(stopEntry.getStopLon()).thenReturn(STOP_LON);
        when(narrativeService.getStopForId(STOP_ID)).thenReturn(STOP_NARRATIVE);

        StopEntryData result = fetcher.getStopEntry(RAW_STOP_ID);

        assertNotNull(result);
        assertEquals(STOP_ID, result.getStopId());
        assertEquals(STOP_LAT, result.getLat(), 0.0001);
        assertEquals(STOP_LON, result.getLon(), 0.0001);
        assertEquals(STOP_NAME, result.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getStopEntry_stopEntryNotFound_throwsIllegalArgumentException() {
        when(entityIdService.getStopId(RAW_STOP_ID)).thenReturn(STOP_ID);
        when(dao.getStopEntryForId(STOP_ID)).thenReturn(null);

        fetcher.getStopEntry(RAW_STOP_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getStopEntry_narrativeNotFound_throwsIllegalArgumentException() {
        when(entityIdService.getStopId(RAW_STOP_ID)).thenReturn(STOP_ID);
        when(dao.getStopEntryForId(STOP_ID)).thenReturn(stopEntry);
        when(narrativeService.getStopForId(STOP_ID)).thenReturn(null);

        fetcher.getStopEntry(RAW_STOP_ID);
    }

    @Test
    public void getStopEntry_narrativeNotFound_exceptionMessageContainsStopId() {
        when(entityIdService.getStopId(RAW_STOP_ID)).thenReturn(STOP_ID);
        when(dao.getStopEntryForId(STOP_ID)).thenReturn(stopEntry);
        when(narrativeService.getStopForId(STOP_ID)).thenReturn(null);

        try {
            fetcher.getStopEntry(RAW_STOP_ID);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(STOP_ID.toString()));
        }
    }

    @Test
    public void getStopEntry_narrativeNotFound_stopEntryCoordinatesNeverRead() {
        when(entityIdService.getStopId(RAW_STOP_ID)).thenReturn(STOP_ID);
        when(dao.getStopEntryForId(STOP_ID)).thenReturn(stopEntry);
        when(narrativeService.getStopForId(STOP_ID)).thenReturn(null);

        try {
            fetcher.getStopEntry(RAW_STOP_ID);
        } catch (IllegalArgumentException ignored) {}

        verify(stopEntry, never()).getStopLat();
        verify(stopEntry, never()).getStopLon();
    }

    @Test
    public void getStopEntry_delegatesToEntityIdServiceForResolution() {
        when(entityIdService.getStopId(RAW_STOP_ID)).thenReturn(STOP_ID);
        when(dao.getStopEntryForId(STOP_ID)).thenReturn(stopEntry);
        when(narrativeService.getStopForId(STOP_ID)).thenReturn(STOP_NARRATIVE);

        fetcher.getStopEntry(RAW_STOP_ID);

        verify(entityIdService).getStopId(RAW_STOP_ID);
        verify(dao).getStopEntryForId(STOP_ID);
        verify(narrativeService).getStopForId(STOP_ID);
    }
}
