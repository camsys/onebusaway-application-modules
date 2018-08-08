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

import com.camsys.transit.servicechange.DateDescriptor;
import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import com.camsys.transit.servicechange.field_descriptors.AbstractFieldDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GtfsSometimesHandlerImplTest {

    private TransitGraphDao dao;

    private GtfsSometimesHandlerImpl handler;

    @Before
    public void setup() {
        dao = mock(TransitGraphDao.class);
        handler = new GtfsSometimesHandlerImpl();
        handler.setTransitGraphDao(dao);
        handler.setAgencyId("1");
        Calendar cal = Calendar.getInstance();
        cal.set(2018, Calendar.JULY, 1, 13, 0, 0);
        handler.setTime(cal.getTimeInMillis());
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

    private static EntityDescriptor stopTimeEntity(String tripId, String stopId) {
        EntityDescriptor desc = new EntityDescriptor();
        desc.setTripId(tripId);
        desc.setStopId(stopId);
        return desc;
    }

    private static List<DateDescriptor> dateDescriptors(LocalDate... dates) {
        List<DateDescriptor> descriptors = new ArrayList<>();
        for (LocalDate ld : dates) {
            DateDescriptor d = new DateDescriptor();
            d.setDate(ld);
            descriptors.add(d);
        }
        return descriptors;
    }

    private static List<DateDescriptor> dateDescriptorsRange(LocalDate... dates) {
        List<DateDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < dates.length/2; i++) {
            LocalDate from = dates[i];
            LocalDate to = dates[i+1];
            DateDescriptor desc = new DateDescriptor();
            desc.setFrom(from);
            desc.setTo(to);
            descriptors.add(desc);
        }
        return descriptors;
    }

    private static ServiceChange serviceChange(Table table, ServiceChangeType type, List<EntityDescriptor> entities,
                                               List<AbstractFieldDescriptor> descriptors, List<DateDescriptor> dates) {
        ServiceChange change = new ServiceChange();
        change.setTable(table);
        change.setServiceChangeType(type);
        change.setAffectedEntity(entities);
        change.setAffectedField(descriptors);
        change.setAffectedDates(dates);
        return change;
    }

}
