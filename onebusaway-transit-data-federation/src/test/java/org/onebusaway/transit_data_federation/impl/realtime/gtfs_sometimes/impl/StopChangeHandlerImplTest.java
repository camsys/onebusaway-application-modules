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
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.StopChange;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.*;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.dateDescriptors;

public class StopChangeHandlerImplTest {

    StopChangeHandlerImpl handler;

    @Before
    public void setup() {
        handler = new StopChangeHandlerImpl();
    }

    // Stop Change

    @Test
    public void stopChangeNameTest() {
        ServiceChange change = serviceChange(Table.STOPS,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopEntity("stopA")),
                stopsFieldsList("stopA name", null, null),
                dateDescriptors(LocalDate.of(2018, 8, 10)));
        List<StopChange> stopChanges = handler.getAllStopChanges(Arrays.asList(change)).getStopChanges();
        assertEquals(1, stopChanges.size());
        StopChange stopChange = stopChanges.get(0);
        assertEquals("stopA", stopChange.getStopId());
        assertEquals("stopA name", stopChange.getStopName());
    }

    @Test
    public void stopChangeLocationTest1() {
        ServiceChange change = serviceChange(Table.STOPS,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopEntity("stopA")),
                stopsFieldsList(null, 10d, 20d),
                dateDescriptors(LocalDate.of(2018, 8, 10)));
        List<StopChange> stopChanges = handler.getAllStopChanges(Arrays.asList(change)).getStopChanges();
        assertEquals(1, stopChanges.size());
        StopChange stopChange = stopChanges.get(0);
        assertEquals("stopA", stopChange.getStopId());
        assertEquals(10d, stopChange.getStopLat(), 0.0001);
        assertEquals(20d, stopChange.getStopLon(), 0.0001);
    }

}
