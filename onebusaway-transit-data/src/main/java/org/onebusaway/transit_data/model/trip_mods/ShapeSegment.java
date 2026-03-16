package org.onebusaway.transit_data.model.trip_mods;

import java.io.Serializable;
import java.util.List;

public class ShapeSegment implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ShapePointSnapshot> points;
    private String encodedPolyline;

    public List<ShapePointSnapshot> getPoints() { return points; }
    public void setPoints(List<ShapePointSnapshot> points) { this.points = points; }

    public String getEncodedPolyline() { return encodedPolyline; }
    public void setEncodedPolyline(String encodedPolyline) { this.encodedPolyline = encodedPolyline; }
}