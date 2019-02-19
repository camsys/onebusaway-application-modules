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
package org.onebusaway.transit_data_federation.services.realtime;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;

public class VehicleStatus {

  private VehicleLocationRecord record;

  private List<VehicleLocationRecord> allRecords;

  public VehicleLocationRecord getRecord() {
    return record;
  }

  public void setRecord(VehicleLocationRecord record) {
    this.record = record;
  }

  public AgencyAndId getVehicleId() {
    return record.getVehicleId();
  }

  public List<VehicleLocationRecord> getAllRecords() {
    return allRecords;
  }

  public void setAllRecords(List<VehicleLocationRecord> allRecords) {
    this.allRecords = allRecords;
  }

  private VehicleOccupancyRecord occupancyRecord;

  public VehicleOccupancyRecord getOccupancyRecord() { return occupancyRecord;}

  public void setOccupancyRecord(VehicleOccupancyRecord vehicleOccupancyRecord) { this.occupancyRecord = vehicleOccupancyRecord; }

}
