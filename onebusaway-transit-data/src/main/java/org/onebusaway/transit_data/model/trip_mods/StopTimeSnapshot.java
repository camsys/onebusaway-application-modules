/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.transit_data.model.trip_mods;

import java.io.Serializable;

public class StopTimeSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String stopId;
    private String stopName;
    private double lat, lon;
    private int stopSequence;
    private int gtfsSequence;
    private int arrivalTime;
    private int departureTime;
    private Double shapeDistTraveled;
    private int index;

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

    public int getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(int arrivalTime) { this.arrivalTime = arrivalTime; }

    public int getDepartureTime() { return departureTime; }
    public void setDepartureTime(int departureTime) { this.departureTime = departureTime; }

    public Double getShapeDistTraveled() { return shapeDistTraveled; }
    public void setShapeDistTraveled(Double shapeDistTraveled) { this.shapeDistTraveled = shapeDistTraveled; }

    public int getGtfsSequence() {
        return gtfsSequence;
    }

    public void setGtfsSequence(int gtfsSequence) {
        this.gtfsSequence = gtfsSequence;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) {
        this.index = index;

    }
}