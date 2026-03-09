package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.model.ShapePoints;

public class ShapeModificationDiff {

    // Full shapes for the whole trip
    private ShapePoints originalShape;
    private ShapePoints modifiedShape;

    // modified segment
    private ShapePoints originalSegment;
    private ShapePoints replacementSegment;

    // The stop IDs mark the changed segment
    private AgencyAndId startStopId;
    private AgencyAndId endStopId;

    public ShapePoints getOriginalShape() {
        return originalShape;
    }

    public void setOriginalShape(ShapePoints originalShape) {
        this.originalShape = originalShape;
    }

    public ShapePoints getModifiedShape() {
        return modifiedShape;
    }

    public void setModifiedShape(ShapePoints modifiedShape) {
        this.modifiedShape = modifiedShape;
    }

    public ShapePoints getOriginalSegment() {
        return originalSegment;
    }

    public void setOriginalSegment(ShapePoints originalSegment) {
        this.originalSegment = originalSegment;
    }

    public ShapePoints getReplacementSegment() {
        return replacementSegment;
    }

    public void setReplacementSegment(ShapePoints replacementSegment) {
        this.replacementSegment = replacementSegment;
    }

    public AgencyAndId getStartStopId() {
        return startStopId;
    }

    public void setStartStopId(AgencyAndId startStopId) {
        this.startStopId = startStopId;
    }

    public AgencyAndId getEndStopId() {
        return endStopId;
    }

    public void setEndStopId(AgencyAndId endStopId) {
        this.endStopId = endStopId;
    }
}
