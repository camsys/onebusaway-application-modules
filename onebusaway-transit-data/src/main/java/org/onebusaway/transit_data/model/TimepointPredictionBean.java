/**
 * Copyright (C) 2013 Kurt Raschke <kurt@kurtraschke.com>
 * Copyright (C) 2015 University of South Florida
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
package org.onebusaway.transit_data.model.trips;

import java.io.Serializable;

public class TimepointPredictionBean implements Serializable {

    private static final long serialVersionUID = 2L;

    private String timepointId;

    private String tripId;

    private int stopSequence = -1;

    private long timepointScheduledTime;

    private long timepointPredictedArrivalTime = -1;

    private long timepointPredictedDepartureTime = -1;

    public TimepointPredictionBean() {

    }

    public String getTimepointId() {
        return timepointId;
    }

    public void setTimepointId(String timepointId) {
        this.timepointId = timepointId;
    }

    public long getTimepointScheduledTime() {
        return timepointScheduledTime;
    }

    public void setTimepointScheduledTime(long timepointScheduledTime) {
        this.timepointScheduledTime = timepointScheduledTime;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(int stopSequence) {
        this.stopSequence = stopSequence;
    }

    public long getTimepointPredictedArrivalTime() {
        return timepointPredictedArrivalTime;
    }

    public void setTimepointPredictedArrivalTime(long timepointPredictedArrivalTime) {
        this.timepointPredictedArrivalTime = timepointPredictedArrivalTime;
    }

    public long getTimepointPredictedDepartureTime() {
        return timepointPredictedDepartureTime;
    }

    public void setTimepointPredictedDepartureTime(
            long timepointPredictedDepartureTime) {
        this.timepointPredictedDepartureTime = timepointPredictedDepartureTime;
    }
}
