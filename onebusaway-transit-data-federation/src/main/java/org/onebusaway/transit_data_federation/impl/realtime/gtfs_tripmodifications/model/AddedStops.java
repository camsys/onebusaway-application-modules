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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddedStops {

    private final List<AddedStop> addedStops = new ArrayList<>();
    private final List<String> failedAddedStopIds = new ArrayList<>();
    private final List<String> successfullyAddedStopIds = new ArrayList<>();

    public void addStop(AddedStop addedStop) {
        addedStops.add(addedStop);
    }

    public List<AddedStop> getAddedStops() {
        return Collections.unmodifiableList(addedStops);
    }

    public int getSuccessfullyAddedCount() {
        return successfullyAddedStopIds.size();
    }

    public int getFailedAddedCount() {
        return failedAddedStopIds.size();
    }

    public List<String> getFailedAddedStopIds() {
        return Collections.unmodifiableList(failedAddedStopIds);
    }

    public List<String> getSuccessfullyAddedStopIds() {
        return Collections.unmodifiableList(successfullyAddedStopIds);
    }

    public void addSuccessfullyAddedStopId(String stopId) {
        this.successfullyAddedStopIds.add(stopId);
    }

    public void addFailedAddedStopId(String stopId) {
        if(stopId == null || stopId.isBlank()){
            this.failedAddedStopIds.add("missing_stopId");
        } else{
            this.failedAddedStopIds.add(stopId);

        }
    }
}
