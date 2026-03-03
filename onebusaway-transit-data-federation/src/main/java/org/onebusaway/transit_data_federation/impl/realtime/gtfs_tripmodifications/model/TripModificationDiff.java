package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

import java.util.List;

public class TripModificationDiff {
    private String tripId;
    private String routeId;
    private long effectiveStartTime;
    private long effectiveEndTime;
    private long lastUpdated;

    private List<StopTimeSnapshot> originalStopTimes;
    private List<StopTimeSnapshot> modifiedStopTimes;

    private List<StopChangeDiff> changes;

    private ShapeModificationDiff shapeDiff;

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public long getEffectiveStartTime() {
        return effectiveStartTime;
    }

    public void setEffectiveStartTime(long effectiveStartTime) {
        this.effectiveStartTime = effectiveStartTime;
    }

    public long getEffectiveEndTime() {
        return effectiveEndTime;
    }

    public void setEffectiveEndTime(long effectiveEndTime) {
        this.effectiveEndTime = effectiveEndTime;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<StopTimeSnapshot> getOriginalStopTimes() {
        return originalStopTimes;
    }

    public void setOriginalStopTimes(List<StopTimeSnapshot> originalStopTimes) {
        this.originalStopTimes = originalStopTimes;
    }

    public List<StopTimeSnapshot> getModifiedStopTimes() {
        return modifiedStopTimes;
    }

    public void setModifiedStopTimes(List<StopTimeSnapshot> modifiedStopTimes) {
        this.modifiedStopTimes = modifiedStopTimes;
    }

    public List<StopChangeDiff> getChanges() {
        return changes;
    }

    public void setChanges(List<StopChangeDiff> changes) {
        this.changes = changes;
    }

    public ShapeModificationDiff getShapeDiff() {
        return shapeDiff;
    }

    public void setShapeDiff(ShapeModificationDiff shapeDiff) {
        this.shapeDiff = shapeDiff;
    }
}


