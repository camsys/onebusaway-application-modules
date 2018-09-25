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

import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.Feed;
import com.camsys.transit.servicechange.FeedEntity;
import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

public class JsonFileGtfsSometimesSourceTest {

    @Test
    public void testReadJson() {
        URL url = getClass().getResource("/gtfs_sometimes/service_change.json");
        String path = url.getPath();
        JsonFileGtfsSometimesSource bean = new JsonFileGtfsSometimesSource();
        bean.setFilename(path);

        Feed feed = bean.getFeed();
        assertNotNull(feed);
        // sanity check that the deserializer worked
        assertEquals(423, feed.getFeedEntities().size());
        ServiceChange serviceChange = feed.getFeedEntities().get(0).getServiceChange();
        assertEquals(Table.STOP_TIMES, serviceChange.getTable());
        assertEquals(ServiceChangeType.ALTER, serviceChange.getServiceChangeType());
        assertEquals(1, serviceChange.getAffectedEntity().size());
        EntityDescriptor entity = serviceChange.getAffectedEntity().get(0);
        assertEquals("MTA NYCT_405075", entity.getStopId());
        assertEquals("MTA NYCT_OH_C8-Weekday-SDon-035100_M101_18", entity.getTripId());
    }

    @Test
    public void testReadJsonAfterMidnight() {
        URL url = getClass().getResource("/gtfs_sometimes/service_change_after_midnight.json");
        String path = url.getPath();
        JsonFileGtfsSometimesSource bean = new JsonFileGtfsSometimesSource();
        bean.setFilename(path);

        Feed feed = bean.getFeed();
        assertNotNull(feed);
    }
}
