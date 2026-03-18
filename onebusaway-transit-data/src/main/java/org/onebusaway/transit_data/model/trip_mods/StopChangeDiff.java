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
package org.onebusaway.transit_data.model.trip_mods;

import java.io.Serializable;

public class StopChangeDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ChangeType {
        UNCHANGED,
        REMOVED,
        ADDED,
        TIME_CHANGED
    }

    private ChangeType changeType;
    private String stopId;
    private StopTimeSnapshot originalStopTime;
    private StopTimeSnapshot modifiedStopTime;
    private Integer originalIndex;
    private Integer modifiedIndex;

    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }

    public String getStopId() { return stopId; }
    public void setStopId(String stopId) { this.stopId = stopId; }

    public StopTimeSnapshot getOriginalStopTime() { return originalStopTime; }
    public void setOriginalStopTime(StopTimeSnapshot originalStopTime) { this.originalStopTime = originalStopTime; }

    public StopTimeSnapshot getModifiedStopTime() { return modifiedStopTime; }
    public void setModifiedStopTime(StopTimeSnapshot modifiedStopTime) { this.modifiedStopTime = modifiedStopTime; }

    public Integer getOriginalIndex() { return originalIndex; }
    public void setOriginalIndex(Integer originalIndex) { this.originalIndex = originalIndex; }

    public Integer getModifiedIndex() { return modifiedIndex; }
    public void setModifiedIndex(Integer modifiedIndex) { this.modifiedIndex = modifiedIndex; }
}