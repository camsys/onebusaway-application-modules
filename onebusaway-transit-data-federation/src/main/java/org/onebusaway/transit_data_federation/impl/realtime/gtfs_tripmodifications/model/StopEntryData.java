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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

import org.onebusaway.gtfs.model.AgencyAndId;

public final class StopEntryData {
    private final AgencyAndId stopId;
    private final double lat;
    private final double lon;
    private final String name;

    public StopEntryData(AgencyAndId stopId, double lat, double lon, String name) {
        this.stopId = stopId;
        this.lat = lat;
        this.lon = lon;
        this.name = name;
    }

    public AgencyAndId getStopId() { return stopId; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getName() { return name; }
}
