package org.onebusaway.transit_data.model.trip_mods;

import java.io.Serializable;

public class ShapePointSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private double lat;
    private double lon;
    private int sequence;
    private double distTraveled;

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public double getDistTraveled() {
        return distTraveled;
    }

    public void setDistTraveled(double distTraveled) {
        this.distTraveled = distTraveled;
    }
}
