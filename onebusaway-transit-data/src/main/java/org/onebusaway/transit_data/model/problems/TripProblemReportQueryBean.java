/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2012 Google, Inc.
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
package org.onebusaway.transit_data.model.problems;

import java.io.Serializable;

import org.onebusaway.transit_data.model.QueryBean;
import org.onebusaway.transit_data.services.TransitDataService;

/**
 * Allows for querying of {@link TripProblemReportBean} instances based on a
 * number of different parameters.
 * 
 * @author bdferris
 * @see TransitDataService#getTripProblemReports(TripProblemReportQueryBean)
 * @see TransitDataService#getTripProblemReportSummariesByGrouping(TripProblemReportQueryBean,
 *      ETripProblemGroupBy)
 */
@QueryBean
public class TripProblemReportQueryBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private String agencyId;

  private String tripId;

  private EProblemReportStatus status;

  private String label;

  private long timeFrom;

  private long timeTo;

  public String getAgencyId() {
    return agencyId;
  }

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public String getTripId() {
    return tripId;
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
  }

  public EProblemReportStatus getStatus() {
    return status;
  }

  public void setStatus(EProblemReportStatus status) {
    this.status = status;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public long getTimeFrom() {
    return timeFrom;
  }

  public void setTimeFrom(long timeFrom) {
    this.timeFrom = timeFrom;
  }

  public long getTimeTo() {
    return timeTo;
  }

  public void setTimeTo(long timeTo) {
    this.timeTo = timeTo;
  }
}
