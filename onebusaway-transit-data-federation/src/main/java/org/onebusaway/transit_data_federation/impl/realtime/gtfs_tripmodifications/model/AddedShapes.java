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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddedShapes {

    private final List<String> failedAddedShapeIds = new ArrayList<>();
    private final List<String> successfullyAddedShapeIds = new ArrayList<>();
    private final List<AddedShape> addedShapes = new ArrayList<>();

    public int getFailedAddedShapeCount() {
        return failedAddedShapeIds.size();
    }

    public int getSuccessfullyAddedCount() {
        return successfullyAddedShapeIds.size();
    }

    public List<String> getFailedAddedShapeIds() {
        return Collections.unmodifiableList(failedAddedShapeIds);
    }

    public List<String> getSuccessfullyAddedShapeIds() {
        return Collections.unmodifiableList(successfullyAddedShapeIds);
    }

    public void addSuccessfullyAddedShapeId(String shapeId) {
        successfullyAddedShapeIds.add(shapeId);
    }

    public void addFailedAddedShapeId(String shapeId) {
        if (shapeId == null || shapeId.isBlank()) {
            this.failedAddedShapeIds.add("missing_shapeId");
        } else {
            this.failedAddedShapeIds.add(shapeId);

        }
    }

    public List<AddedShape> getAddedShapes() {
        return Collections.unmodifiableList(addedShapes);
    }

    public void addShape(AddedShape addedShape) {
        addedShapes.add(addedShape);
    }

}