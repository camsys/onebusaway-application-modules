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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.*;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.time;

public class GtfsSometimesHandlerImplTest {

    private GtfsSometimesHandlerImpl handler = new GtfsSometimesHandlerImpl();

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

}
