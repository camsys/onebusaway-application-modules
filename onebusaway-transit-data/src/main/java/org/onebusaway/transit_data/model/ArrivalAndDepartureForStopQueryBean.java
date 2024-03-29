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
package org.onebusaway.transit_data.model;

import org.onebusaway.gtfs.model.calendar.AgencyServiceInterval;

import java.io.Serializable;
import java.util.HashSet;

@QueryBean
/**
 * Query for a specific arrival and departure.  As this is a specific request,
 * we do not apply the system FilterChain.
 */
public final class ArrivalAndDepartureForStopQueryBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private String tripId;

  private long serviceDate;

  private String vehicleId;

  private String stopId;

  private int stopSequence = -1;

  private long time;

  private HashSet<String> agenciesExcludingScheduled;

  private AgencyServiceInterval serviceInterval;

  public String getTripId() {
    return tripId;
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
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

  public String getStopId() {
    return stopId;
  }

  public void setStopId(String stopId) {
    this.stopId = stopId;
  }

  public int getStopSequence() {
    return stopSequence;
  }

  public void setStopSequence(int stopSequence) {
    this.stopSequence = stopSequence;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public void setAgenciesExcludingScheduled(HashSet<String> agenciesExcludingScheduled) {
    this.agenciesExcludingScheduled = agenciesExcludingScheduled;
  }

  public HashSet<String> getAgenciesExcludingScheduled(){
    return this.agenciesExcludingScheduled;
  }

  public AgencyServiceInterval getServiceInterval() {
    return serviceInterval;
  }

  public void setServiceInterval(AgencyServiceInterval serviceInterval) {
    this.serviceInterval = serviceInterval;
  }
}
