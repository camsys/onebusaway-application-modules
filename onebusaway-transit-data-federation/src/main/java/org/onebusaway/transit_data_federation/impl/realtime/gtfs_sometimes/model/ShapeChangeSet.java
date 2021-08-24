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

import org.onebusaway.gtfs.model.AgencyAndId;

import java.util.ArrayList;
import java.util.List;

public class ShapeChangeSet {
    private List<AddShape> addedShapes = new ArrayList<>();

    private List<AgencyAndId> deletedShapes = new ArrayList<>();

    public List<AddShape> getAddedShapes() {
        return addedShapes;
    }

    public void addAddedShape(AddShape addedShape) {
        addedShapes.add(addedShape);
    }

    public List<AgencyAndId> getDeletedShapes() {
        return deletedShapes;
    }

    public void addDeletedShape(AgencyAndId id) {
        deletedShapes.add(id);
    }

    public int size() {
        return addedShapes.size() + deletedShapes.size();
    }
}
