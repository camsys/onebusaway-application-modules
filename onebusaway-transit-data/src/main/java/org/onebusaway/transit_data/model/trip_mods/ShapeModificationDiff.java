package org.onebusaway.transit_data.model.trip_mods;

import java.io.Serializable;
import java.util.List;

public class ShapeModificationDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ShapePointSnapshot> originalShape;
    private List<ShapePointSnapshot> modifiedShape;
    private List<ShapePointSnapshot> originalSegment;
    private List<ShapePointSnapshot> replacementSegment;

    private List<ShapePointSnapshot> prefixSegment; // segment from start of shape to start stop

    private List<ShapePointSnapshot> suffixSegment; // segment from end stop to end of shape

    private String startStopId;
    private String endStopId;

    public List<ShapePointSnapshot> getPrefixSegment() {
        return prefixSegment;
    }

    public void setPrefixSegment(List<ShapePointSnapshot> prefixSegment) {
        this.prefixSegment = prefixSegment;
    }

    public List<ShapePointSnapshot> getSuffixSegment() {
        return suffixSegment;
    }

    public void setSuffixSegment(List<ShapePointSnapshot> suffixSegment) {
        this.suffixSegment = suffixSegment;
    }

    public List<ShapePointSnapshot> getOriginalShape() { return originalShape; }
    public void setOriginalShape(List<ShapePointSnapshot> originalShape) { this.originalShape = originalShape; }

    public List<ShapePointSnapshot> getModifiedShape() { return modifiedShape; }
    public void setModifiedShape(List<ShapePointSnapshot> modifiedShape) { this.modifiedShape = modifiedShape; }

    public List<ShapePointSnapshot> getOriginalSegment() { return originalSegment; }
    public void setOriginalSegment(List<ShapePointSnapshot> originalSegment) { this.originalSegment = originalSegment; }

    public List<ShapePointSnapshot> getReplacementSegment() { return replacementSegment; }
    public void setReplacementSegment(List<ShapePointSnapshot> replacementSegment) { this.replacementSegment = replacementSegment; }

    public String getStartStopId() { return startStopId; }
    public void setStartStopId(String startStopId) { this.startStopId = startStopId; }

    public String getEndStopId() { return endStopId; }
    public void setEndStopId(String endStopId) { this.endStopId = endStopId; }
}