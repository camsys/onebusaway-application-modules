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

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;

public class AddedStop {
    private final StopEntryImpl stopEntry;
    private final GtfsRealtime.Stop stop;
    private final AgencyAndId stopId;


    public AddedStop(StopEntryImpl stopEntry,
                     GtfsRealtime.Stop stop) {
        this.stopEntry = stopEntry;
        this.stop = stop;
        this.stopId = stopEntry.getId();
    }

    public StopEntryImpl getStopEntry() {
        return stopEntry;
    }

    public GtfsRealtime.Stop getStop() {
        return stop;
    }

    public AgencyAndId getStopId() {
        return stopId;
    }
}
