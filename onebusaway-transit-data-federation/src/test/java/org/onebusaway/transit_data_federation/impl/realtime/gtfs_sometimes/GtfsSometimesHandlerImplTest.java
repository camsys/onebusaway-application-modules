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
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.GtfsRealtimeEntitySource;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.*;

public class GtfsSometimesHandlerImplTest {

    private TransitGraphDao dao;

    private GtfsSometimesHandlerImpl handler;

    @Before
    public void setup() {
        dao = mock(TransitGraphDao.class);
        when(dao.deleteStopTime(any(), any())).thenReturn(true);
        handler = new GtfsSometimesHandlerImpl();
        handler.setTransitGraphDao(dao);
        handler.setAgencyId("1");
        Calendar cal = Calendar.getInstance();
        cal.set(2018, Calendar.JULY, 1, 13, 0, 0);
        handler.setTime(cal.getTimeInMillis());
        GtfsRealtimeEntitySource source = new MockEntitySource() { };
        handler.setEntitySource(source);
    }

    @Test
    public void deleteStopTimesTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null, // no affected field for delete
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        assertTrue(handler.handleServiceChange(change));
        verify(dao, times(1)).deleteStopTime(
                AgencyAndId.convertFromString("1_tripA"),
                AgencyAndId.convertFromString("1_stopA"));
    }

    @Test
    public void deleteStopTimesWrongDateTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                dateDescriptors(LocalDate.of(2018, 7, 2)));
        assertFalse(handler.handleServiceChange(change));
        verify(dao, never()).deleteStopTime(any(), any());
    }

    @Test
    public void deleteStopTimesDateRangeTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                dateDescriptorsRange(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 8, 1)));
        assertTrue(handler.handleServiceChange(change));
        verify(dao, times(1)).deleteStopTime(
                AgencyAndId.convertFromString("1_tripA"),
                AgencyAndId.convertFromString("1_stopA"));
    }

    @Test
    public void deleteStopTimesWrongDateRangeTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                dateDescriptorsRange(LocalDate.of(2018, 7, 2), LocalDate.of(2018, 8, 1)));
        assertFalse(handler.handleServiceChange(change));
        verify(dao, never()).deleteStopTime(any(), any());
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
        assertTrue(handler.handleServiceChange(change));
        verify(dao, times(1)).deleteStopTime(
                AgencyAndId.convertFromString("1_tripA"),
                AgencyAndId.convertFromString("1_stopA"));
        verify(dao, times(1)).deleteStopTime(
                AgencyAndId.convertFromString("1_tripB"),
                AgencyAndId.convertFromString("1_stopB"));
    }

    @Test
    public void addStopTimesTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null, // TODO
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        assertTrue(handler.handleServiceChange(change));
        verify(dao, times(1)).deleteStopTime(
                AgencyAndId.convertFromString("1_tripA"),
                AgencyAndId.convertFromString("1_stopA"));
    }

    private abstract class MockEntitySource extends GtfsRealtimeEntitySource {
        @Override
        public AgencyAndId getObaTripId(String tripId) {
            return new AgencyAndId("1", tripId);
        }

        @Override
        public AgencyAndId getObaStopId(String stopId) {
            return new AgencyAndId("1", stopId);
        }
    }
}
