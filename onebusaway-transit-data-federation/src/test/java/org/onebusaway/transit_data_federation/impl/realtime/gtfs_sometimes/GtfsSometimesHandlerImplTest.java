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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.GtfsRealtimeEntitySource;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
        when(dao.insertStopTime(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(dao.updateStopTime(any(), any(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(true);

        // for alter trip:
        TripEntryImpl tripA = new TripEntryImpl();
        tripA.setId(new AgencyAndId("1", "tripA"));
        StopTimeEntryImpl stopTimeA = new StopTimeEntryImpl();
        StopEntryImpl stopA = new StopEntryImpl(new AgencyAndId("1", "stopA"), 0d, 0d);
        stopTimeA.setStop(stopA);
        stopTimeA.setArrivalTime(LocalTime.of(8, 0, 0).toSecondOfDay());
        stopTimeA.setDepartureTime(LocalTime.of(8, 0, 0).toSecondOfDay());
        tripA.setStopTimes(Collections.singletonList(stopTimeA));
        when(dao.getTripEntryForId(new AgencyAndId("1", "tripA"))).thenReturn(tripA);

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
    public void deleteStopTimesAffectedFieldCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                Collections.singletonList(new StopTimesFields()),
                dateDescriptors(LocalDate.of(2018, 7, 1)));
        assertFalse(handler.handleServiceChange(change));
    }

    @Test
    public void deleteStopTimesDateCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                Collections.emptyList());
        assertFalse(handler.handleServiceChange(change));

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
    public void addStopTimesAffectedEntityCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                Arrays.asList(stopTimeEntity("tripA", "stopA")),
                stopTimesFieldsList("tripA", LocalTime.of(9, 0, 0), LocalTime.of(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertFalse(handler.handleServiceChange(change));
    }

    @Test
    public void addStopTimesTestAffectedDateCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("tripA", LocalTime.of(9, 0, 0), LocalTime.of(9, 0, 0), "stopA", 0),
                Collections.emptyList());
        assertFalse(handler.handleServiceChange(change));
    }

    @Test
    public void addStopTimesTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("tripA", LocalTime.of(9, 0, 0), LocalTime.of(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertTrue(handler.handleServiceChange(change));
        verify(dao, times(1)).insertStopTime(
                AgencyAndId.convertFromString("1_tripA"),
                AgencyAndId.convertFromString("1_stopA"),
                9 * 3600, 9 * 3600, -1);
    }

    @Test
    public void alterStopTimesTestAffectedEntityCardinalityTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                null,
                stopTimesFieldsList("tripA", LocalTime.of(9, 0, 0), LocalTime.of(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertFalse(handler.handleServiceChange(change));
    }

    @Test
    public void alterStopTimesTestAffectedFieldCardinalityTest0() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                null,
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertFalse(handler.handleServiceChange(change));
    }

    @Test
    public void alterStopTimesTestAffectedFieldCardinalityTest1() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                Arrays.asList(stopTimesFieldDescriptor(null, null, null, null, -1),
                        stopTimesFieldDescriptor(null, null, null, null, -1)),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertFalse(handler.handleServiceChange(change));
    }

    @Test
    public void alterStopTimesTest() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("tripA", "stopA")),
                stopTimesFieldsList("tripA", LocalTime.of(9, 0, 0), LocalTime.of(9, 0, 0), "stopA", 0),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        assertTrue(handler.handleServiceChange(change));
        verify(dao, times(1)).updateStopTime(
                AgencyAndId.convertFromString("1_tripA"),
                AgencyAndId.convertFromString("1_stopA"),
                8 * 3600, 8 * 3600,
                9 * 3600, 9 * 3600);
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
