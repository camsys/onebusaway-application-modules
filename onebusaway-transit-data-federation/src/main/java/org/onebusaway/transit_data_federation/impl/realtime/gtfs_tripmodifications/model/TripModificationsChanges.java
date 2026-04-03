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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripModificationsChanges {

    private final Map<String, GtfsRealtime.TripModifications> tripModificationsList = new HashMap<>();
    private final Map<String, GtfsRealtime.Shape> shapesList = new HashMap<>();
    private final Map<String, GtfsRealtime.Stop> stopsList = new HashMap<>();
    private long feedTimestamp;
    private byte[] hash;

    public Map<String, GtfsRealtime.TripModifications> getTripModifications() {
        return tripModificationsList;
    }

    public void addTripModification(String entityId, GtfsRealtime.TripModifications tripModification) {
        this.tripModificationsList.put(entityId, tripModification);
    }

    public Map<String, GtfsRealtime.Shape> getShapes() {
        return shapesList;
    }

    public void addShape(String entityId, GtfsRealtime.Shape shape) {
        this.shapesList.put(entityId, shape);
    }

    public Map<String, GtfsRealtime.Stop> getStops() {
        return stopsList;
    }

    public void addStop(String entityId, GtfsRealtime.Stop stop) {
        this.stopsList.put(entityId, stop);
    }

    public void setFeedTimestamp(long timestamp) {
        if(timestamp <= 0) {
            this.feedTimestamp = System.currentTimeMillis();
        }
        else {
            this.feedTimestamp = timestamp;
        }
    }

    public long getFeedTimestamp() {
        return this.feedTimestamp;
    }

    public boolean hasChanges() {
        return !tripModificationsList.isEmpty() || !shapesList.isEmpty() || !stopsList.isEmpty();
    }
}
