package org.onebusaway.transit_data.model.trip_mods;

import java.io.Serializable;
import java.util.List;

public class ShapeModificationDiff implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ShapePointSnapshot> originalShape;
    private List<ShapePointSnapshot> modifiedShape;
    private List<ShapePointSnapshot> originalSegment;
    private List<ShapePointSnapshot> replacementSegment;

    // AgencyAndId -> String
    private String startStopId;
    private String endStopId;

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