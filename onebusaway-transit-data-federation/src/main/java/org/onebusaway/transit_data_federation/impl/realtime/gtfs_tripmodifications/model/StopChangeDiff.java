package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

import org.onebusaway.gtfs.model.AgencyAndId;

public class StopChangeDiff {

    public enum ChangeType {
        UNCHANGED,
        REMOVED,
        ADDED,
        TIME_CHANGED
    }
    private ChangeType changeType;
    private AgencyAndId stopId;
    private StopTimeSnapshot originalStopTime;
    private StopTimeSnapshot modifiedStopTime;

    private Integer originalIndex;
    private Integer modifiedIndex;

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public AgencyAndId getStopId() {
        return stopId;
    }

    public void setStopId(AgencyAndId stopId) {
        this.stopId = stopId;
    }

    public StopTimeSnapshot getOriginalStopTime() {
        return originalStopTime;
    }

    public void setOriginalStopTime(StopTimeSnapshot originalStopTime) {
        this.originalStopTime = originalStopTime;
    }

    public StopTimeSnapshot getModifiedStopTime() {
        return modifiedStopTime;
    }

    public void setModifiedStopTime(StopTimeSnapshot modifiedStopTime) {
        this.modifiedStopTime = modifiedStopTime;
    }

    public Integer getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(Integer originalIndex) {
        this.originalIndex = originalIndex;
    }

    public Integer getModifiedIndex() {
        return modifiedIndex;
    }

    public void setModifiedIndex(Integer modifiedIndex) {
        this.modifiedIndex = modifiedIndex;
    }
}
