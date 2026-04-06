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

    private String originalShapePolyline;
    private String modifiedShapePolyline;
    private String originalSegmentPolyline;
    private String replacementSegmentPolyline;
    private String prefixSegmentPolyline;
    private String suffixSegmentPolyline;

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

    public String getOriginalShapePolyline() { return originalShapePolyline; }
    public void setOriginalShapePolyline(String originalShapePolyline) { this.originalShapePolyline = originalShapePolyline; }

    public String getModifiedShapePolyline() { return modifiedShapePolyline; }
    public void setModifiedShapePolyline(String modifiedShapePolyline) { this.modifiedShapePolyline = modifiedShapePolyline; }

    public String getOriginalSegmentPolyline() { return originalSegmentPolyline; }
    public void setOriginalSegmentPolyline(String originalSegmentPolyline) { this.originalSegmentPolyline = originalSegmentPolyline; }

    public String getReplacementSegmentPolyline() { return replacementSegmentPolyline; }
    public void setReplacementSegmentPolyline(String replacementSegmentPolyline) { this.replacementSegmentPolyline = replacementSegmentPolyline; }

    public String getPrefixSegmentPolyline() { return prefixSegmentPolyline; }
    public void setPrefixSegmentPolyline(String prefixSegmentPolyline) { this.prefixSegmentPolyline = prefixSegmentPolyline; }

    public String getSuffixSegmentPolyline() { return suffixSegmentPolyline; }
    public void setSuffixSegmentPolyline(String suffixSegmentPolyline) { this.suffixSegmentPolyline = suffixSegmentPolyline; }

    public String getStartStopId() { return startStopId; }
    public void setStartStopId(String startStopId) { this.startStopId = startStopId; }

    public String getEndStopId() { return endStopId; }
    public void setEndStopId(String endStopId) { this.endStopId = endStopId; }
}