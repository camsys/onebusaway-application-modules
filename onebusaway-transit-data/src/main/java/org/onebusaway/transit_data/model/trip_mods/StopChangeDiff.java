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