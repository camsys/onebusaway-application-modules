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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.services.StopTimeService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration(locations = "classpath:org/onebusaway/transit_data_federation/tds-test-with-gtfs-sometimes-client.xml")
@TestPropertySource(properties = { "bundlePath = /tmp/foo"})
public class GtfsSometimesBlocksTest extends AbstractGtfsSometimesClientTest {

    @Autowired
    private TransitDataService _tds;

    @Autowired
    private StopTimeService _stopTimeService;

    @Override
    String getPath() {
        return getClass().getResource("/gtfs_sometimes/gtfs_brooklyn_B44.zip").getPath();
    }

    @Override
    String getTime() {
        return "2018-12-10 12:00";
    }

    @Test
    @DirtiesContext
    public void testChangeStopName() {
        String newName = "new stop name";
        String oldName = _tds.getStop("MTA_303479").getName();

        long time = _timeService.getCurrentTimeAsEpochMs();
        Date serviceStart = new Date(time - 10 * 1000);
        Date serviceEnd = new Date(time + 3600 * 1000);

        StopBean stopBean = _tds.getStop("MTA_303479");
        assertEquals(oldName, stopBean.getName());
        StopEntry stopEntry = _graph.getStopEntryForId(AgencyAndId.convertFromString(stopBean.getId()));
        assertNotNull(stopEntry);
        List<StopTimeInstance> stis = _stopTimeService.getStopTimeInstancesInTimeRange(
                stopEntry, serviceStart, serviceEnd,
                StopTimeService.EFrequencyStopTimeBehavior.INCLUDE_UNSPECIFIED);
        assertFalse(stis.isEmpty());

        ServiceChange change = serviceChange(Table.STOPS,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopEntity("303479")),
                stopsFieldsList(newName, null, null),
                dateDescriptors(LocalDate.of(2018, 12, 10)));
        assertTrue(_handler.handleServiceChange(change));

        stopBean = _tds.getStop("MTA_303479");
        assertEquals(newName, stopBean.getName());
        stopEntry = _graph.getStopEntryForId(AgencyAndId.convertFromString(stopBean.getId()));
        assertNotNull(stopEntry);
        stis = _stopTimeService.getStopTimeInstancesInTimeRange(
                stopEntry, serviceStart, serviceEnd,
                StopTimeService.EFrequencyStopTimeBehavior.INCLUDE_UNSPECIFIED);

        assertFalse(stis.isEmpty());
    }
}
