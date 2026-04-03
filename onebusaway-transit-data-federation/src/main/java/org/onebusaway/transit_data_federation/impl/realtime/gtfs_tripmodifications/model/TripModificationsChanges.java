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
import java.util.List;

public class TripModificationsChanges {

    private final List<GtfsRealtime.TripModifications> tripModificationsList = new ArrayList<>();
    private final List<GtfsRealtime.Shape> shapesList = new ArrayList<>();
    private final List<GtfsRealtime.Stop> stopsList = new ArrayList<>();
    private long feedTimestamp;
    private byte[] hash;

    public List<GtfsRealtime.TripModifications> getTripModifications() {
        return tripModificationsList;
    }

    public void addTripModification(GtfsRealtime.TripModifications tripModification) {
        this.tripModificationsList.add(tripModification);
    }

    public List<GtfsRealtime.Shape> getShapes() {
        return shapesList;
    }

    public void addShape(GtfsRealtime.Shape shape) {
        this.shapesList.add(shape);
    }

    public List<GtfsRealtime.Stop> getStops() {
        return stopsList;
    }

    public void addStop(GtfsRealtime.Stop stop) {
        this.stopsList.add(stop);
    }

    public void setFeedTimestamp(long timestamp) {
        if(timestamp <= 0) {
            this.feedTimestamp = System.currentTimeMillis();
        }
        else {
            this.feedTimestamp = timestamp;
        }
    }

    public void setLastModifiedTimestamp(Long timestamp) {
        if (timestamp == null) {
            setFeedTimestamp(0);
        } else {
            setFeedTimestamp(timestamp);
        }
    }

    public long getFeedTimestamp() {
        return this.feedTimestamp;
    }

    public boolean hasChanges() {
        return !tripModificationsList.isEmpty() || !shapesList.isEmpty() || !stopsList.isEmpty();
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }
}
