package org.onebusaway.transit_data.model.trip_mods;

import java.io.Serializable;

public class StopTimeSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String stopId;
    private String stopName;
    private double lat, lon;
    private int stopSequence;
    private int arrivalOffset;
    private int departureOffset;
    private Double shapeDistTraveled;

    public String getStopId() { return stopId; }
    public void setStopId(String stopId) { this.stopId = stopId; }

    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public int getStopSequence() { return stopSequence; }
    public void setStopSequence(int stopSequence) { this.stopSequence = stopSequence; }

    public int getArrivalOffset() { return arrivalOffset; }
    public void setArrivalOffset(int arrivalOffset) { this.arrivalOffset = arrivalOffset; }

    public int getDepartureOffset() { return departureOffset; }
    public void setDepartureOffset(int departureOffset) { this.departureOffset = departureOffset; }

    public Double getShapeDistTraveled() { return shapeDistTraveled; }
    public void setShapeDistTraveled(Double shapeDistTraveled) { this.shapeDistTraveled = shapeDistTraveled; }
}