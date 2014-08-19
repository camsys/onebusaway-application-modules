/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.transit_data.model.tripplanning;

import java.io.Serializable;
import java.util.List;

import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TimeIntervalBean;
import org.onebusaway.transit_data.model.schedule.FrequencyBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;

public class TransitLegBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private TripBean trip;

  private long serviceDate;

  private String vehicleId;

  private FrequencyBean frequency;

  private StopBean fromStop;

  private Integer fromStopSequence;

  private StopBean toStop;

  private Integer toStopSequence;

  private String routeShortName;

  private String routeLongName;

  private String tripHeadsign;

  private String path;

  private long scheduledDepartureTime;

  private TimeIntervalBean scheduledDepartureInterval;

  private long predictedDepartureTime;

  private TimeIntervalBean predictedDepartureInterval;

  private long scheduledArrivalTime;

  private TimeIntervalBean scheduledArrivalInterval;

  private long predictedArrivalTime;

  private TimeIntervalBean predictedArrivalInterval;
  private List<ServiceAlertBean> situations;

  public TripBean getTrip() {
    return trip;
  }

  public void setTrip(TripBean trip) {
    this.trip = trip;
  }

  public long getServiceDate() {
    return serviceDate;
  }

  public void setServiceDate(long serviceDate) {
    this.serviceDate = serviceDate;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public FrequencyBean getFrequency() {
    return frequency;
  }

  public void setFrequency(FrequencyBean frequency) {
    this.frequency = frequency;
  }

  public StopBean getFromStop() {
    return fromStop;
  }

  public void setFromStop(StopBean fromStop) {
    this.fromStop = fromStop;
  }

  public Integer getFromStopSequence() {
    return fromStopSequence;
  }

  public void setFromStopSequence(Integer fromStopSequence) {
    this.fromStopSequence = fromStopSequence;
  }

  public StopBean getToStop() {
    return toStop;
  }

  public void setToStop(StopBean toStop) {
    this.toStop = toStop;
  }

  public Integer getToStopSequence() {
    return toStopSequence;
  }

  public void setToStopSequence(Integer toStopSequence) {
    this.toStopSequence = toStopSequence;
  }

  public String getRouteShortName() {
    return routeShortName;
  }

  public void setRouteShortName(String routeShortName) {
    this.routeShortName = routeShortName;
  }

  public String getRouteLongName() {
    return routeLongName;
  }

  public void setRouteLongName(String routeLongName) {
    this.routeLongName = routeLongName;
  }

  public String getTripHeadsign() {
    return tripHeadsign;
  }

  public void setTripHeadsign(String tripHeadsign) {
    this.tripHeadsign = tripHeadsign;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public long getScheduledDepartureTime() {
    return scheduledDepartureTime;
  }

  public void setScheduledDepartureTime(long scheduledDepartureTime) {
    this.scheduledDepartureTime = scheduledDepartureTime;
  }

  public TimeIntervalBean getScheduledDepartureInterval() {
    return scheduledDepartureInterval;
  }

  public void setScheduledDepartureInterval(
      TimeIntervalBean scheduledDepartureInterval) {
    this.scheduledDepartureInterval = scheduledDepartureInterval;
  }

  public long getPredictedDepartureTime() {
    return predictedDepartureTime;
  }

  public void setPredictedDepartureTime(long predictedDepartureTime) {
    this.predictedDepartureTime = predictedDepartureTime;
  }

  public TimeIntervalBean getPredictedDepartureInterval() {
    return predictedDepartureInterval;
  }

  public void setPredictedDepartureInterval(
      TimeIntervalBean predictedDepartureInterval) {
    this.predictedDepartureInterval = predictedDepartureInterval;
  }

  public long getScheduledArrivalTime() {
    return scheduledArrivalTime;
  }

  public void setScheduledArrivalTime(long scheduledArrivalTime) {
    this.scheduledArrivalTime = scheduledArrivalTime;
  }

  public TimeIntervalBean getScheduledArrivalInterval() {
    return scheduledArrivalInterval;
  }

  public void setScheduledArrivalInterval(
      TimeIntervalBean scheduledArrivalInterval) {
    this.scheduledArrivalInterval = scheduledArrivalInterval;
  }

  public long getPredictedArrivalTime() {
    return predictedArrivalTime;
  }

  public void setPredictedArrivalTime(long predictedArrivalTime) {
    this.predictedArrivalTime = predictedArrivalTime;
  }

  public TimeIntervalBean getPredictedArrivalInterval() {
    return predictedArrivalInterval;
  }

  public void setPredictedArrivalInterval(
      TimeIntervalBean predictedArrivalInterval) {
    this.predictedArrivalInterval = predictedArrivalInterval;
  }

  public List<ServiceAlertBean> getSituations() {
    return situations;
  }

  public void setSituations(List<ServiceAlertBean> situations) {
    this.situations = situations;
  }
}
