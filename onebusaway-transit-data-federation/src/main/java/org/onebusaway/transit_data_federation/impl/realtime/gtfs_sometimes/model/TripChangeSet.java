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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TripChangeSet {

    private List<DeleteTrip> deletedTrips = new ArrayList<>();

    private List<ModifyTrip> modifiedTrips = new ArrayList<>();

    private List<AddTrip> addedTrips = new ArrayList<>();

    public void addDeletedTrip(DeleteTrip trip) {
        deletedTrips.add(trip);
    }

    public void addModifiedTrip(ModifyTrip trip) {
        modifiedTrips.add(trip);
    }

    public void addAddedTrip(AddTrip addTrip) {
        addedTrips.add(addTrip);
    }

    public List<DeleteTrip> getDeletedTrips() {
        return deletedTrips;
    }

    public List<ModifyTrip> getModifiedTrips() {
        return modifiedTrips;
    }

    public List<AddTrip> getAddedTrips() {
        return addedTrips;
    }

    public int size() {
        return deletedTrips.size() + modifiedTrips.size() + addedTrips.size();
    }

    public List<TripChange> getAllChanges() {
        List<TripChange> changes = new ArrayList<>();
        changes.addAll(deletedTrips);
        changes.addAll(modifiedTrips);
        changes.addAll(addedTrips);
        return changes;
    }
}
