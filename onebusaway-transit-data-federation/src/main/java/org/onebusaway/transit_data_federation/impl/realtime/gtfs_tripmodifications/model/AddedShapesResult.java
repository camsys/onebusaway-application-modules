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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AddedShapesResult {
    private final List<AgencyAndId> failedUpdatedShapeIds = new ArrayList<>();
    private final List<AgencyAndId> successfullyUpdatedShapeIds = new ArrayList<>();

    public int getFailedUpdatedShapeCount() {
        return failedUpdatedShapeIds.size();
    }

    public int getSuccessfullyUpdatedShapeCount() {
        return successfullyUpdatedShapeIds.size();
    }

    public List<AgencyAndId> getFailedUpdatedShapeIds() {
        return Collections.unmodifiableList(failedUpdatedShapeIds);
    }

    public List<AgencyAndId> getSuccessfullyUpdatedShapeIds() {
        return Collections.unmodifiableList(successfullyUpdatedShapeIds);
    }

    public void addSuccessfullyUpdatedShapeId(AgencyAndId shapeId) {
        successfullyUpdatedShapeIds.add(shapeId);
    }

    public void addFailedUpdatedShapeId(AgencyAndId shapeId) {
        if(shapeId != null){
            this.failedUpdatedShapeIds.add(shapeId);
        }
    }

    public String getSuccessfullyUpdatedShapeIdsAsString() {
        return successfullyUpdatedShapeIds.stream()
                .filter(Objects::nonNull)
                .map(AgencyAndId::convertToString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public String getFailedUpdatedShapeIdsAsString() {
        return failedUpdatedShapeIds.stream()
                .filter(Objects::nonNull)
                .map(AgencyAndId::convertToString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

}
