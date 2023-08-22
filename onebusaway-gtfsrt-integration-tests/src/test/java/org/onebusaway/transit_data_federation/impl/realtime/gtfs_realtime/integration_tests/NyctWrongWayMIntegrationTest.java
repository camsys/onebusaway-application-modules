/**
 * Copyright (C) 2023 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.integration_tests;

import org.junit.Test;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.AbstractGtfsRealtimeBeanVerificationTest;

public class NyctWrongWayMIntegrationTest extends AbstractGtfsRealtimeBeanVerificationTest {
    @Override
    protected String getIntegrationTestPath() {
        return "org/onebusaway/transit_data_federation/impl/realtime/gtfs_realtime/integration_tests/nyct_wrong_way";
    }

    @Override
    protected String[] getPaths() {
        String[] paths = {"test-data-sources.xml"};
        return paths;
    }

    @Override
    protected String getPbFilename() {
        return "org/onebusaway/transit_data_federation/impl/realtime/gtfs_realtime/integration_tests/nyct_wrong_way/m.pb";
    }

    @Override
    protected String[] getSouthStops() {
        String[] a = {"MTASBWY_M11N"}; // not found
        return a;
    }

    @Override
    protected String[] getNorthStops() {
        String[] a = {"MTASBWY_M11S"};
        return a;
    }
    @Test
    public void testWrongWayConcurrenciesOnJM() throws Exception {
        executeTest();
    }

}
