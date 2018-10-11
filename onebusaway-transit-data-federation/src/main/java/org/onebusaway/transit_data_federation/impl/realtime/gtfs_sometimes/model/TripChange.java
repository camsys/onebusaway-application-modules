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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model;

import com.camsys.transit.servicechange.EntityDescriptor;
import com.camsys.transit.servicechange.field_descriptors.StopTimesFields;
import com.camsys.transit.servicechange.field_descriptors.TripsFields;
import org.onebusaway.gtfs.model.AgencyAndId;

import java.util.ArrayList;
import java.util.List;

public class TripChange {
    public enum TripChangeType { MODIFY, ADD, DELETE };

    public TripChange(String tripId) {
        this.tripId = tripId;
    }

    private String tripId;

    private TripChangeType type = TripChangeType.MODIFY;

    private TripsFields addedTripsFields;

    private List<StopTimesFields> modifiedStops = new ArrayList<>();

    private List<StopTimesFields> insertedStops = new ArrayList<>();

    private List<EntityDescriptor> deletedStops = new ArrayList<>();

    private AgencyAndId newShapeId;

    public String getTripId() {
        return tripId;
    }

    public List<StopTimesFields> getModifiedStops() {
        return modifiedStops;
    }

    public void addModifiedStop(StopTimesFields stop) {
        modifiedStops.add(stop);
    }

    public List<StopTimesFields> getInsertedStops() {
        return insertedStops;
    }

    public void addInsertedStop(StopTimesFields stop) {
        insertedStops.add(stop);
    }

    public List<EntityDescriptor> getDeletedStops() {
        return deletedStops;
    }

    public void addDeletedStop(EntityDescriptor stop) {
        deletedStops.add(stop);
    }

    public AgencyAndId getNewShapeId() {
        return newShapeId;
    }

    public void setNewShapeId(AgencyAndId newShapeId) {
        this.newShapeId = newShapeId;
    }

    public TripsFields getAddedTripsFields() {
        return addedTripsFields;
    }

    public void setAddedTripsFields(TripsFields addedTripsFields) {
        this.addedTripsFields = addedTripsFields;
        this.type = TripChangeType.ADD;
    }

    public void setDelete() {
        this.type = TripChangeType.DELETE;
    }

    public boolean isAdded() {
        return TripChangeType.ADD.equals(type);
    }

    public boolean isDelete() {
        return TripChangeType.DELETE.equals(type);
    }
}