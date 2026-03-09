package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

import org.onebusaway.gtfs.model.AgencyAndId;

import java.time.LocalDate;
import java.util.List;

public class TripModificationDiff {
    private AgencyAndId tripId;
    private String routeId;
    private List<LocalDate> effectiveServiceDates;
    private long lastUpdated;

    private List<StopTimeSnapshot> originalStopTimes;
    private List<StopTimeSnapshot> modifiedStopTimes;

    private List<StopChangeDiff> changes;

    private ShapeModificationDiff shapeDiff;

    public AgencyAndId getTripId() {
        return tripId;
    }

    public void setTripId(AgencyAndId tripId) {
        this.tripId = tripId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public List<LocalDate> getEffectiveServiceDates() {
        return effectiveServiceDates;
    }

    public void setEffectiveServiceDates(List<LocalDate> effectiveServiceDates) {
        this.effectiveServiceDates = effectiveServiceDates;
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


