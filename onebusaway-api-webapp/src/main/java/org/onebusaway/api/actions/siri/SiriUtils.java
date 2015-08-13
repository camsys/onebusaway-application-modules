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
package org.onebusaway.api.actions.siri;

import org.onebusaway.siri.model.DistanceExtensions;
import org.onebusaway.siri.model.Distances;
import org.onebusaway.siri.model.FramedVehicleJourneyRef;
import org.onebusaway.siri.model.MonitoredCall;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.OnwardCall;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SiriUtils {

  private static Logger _log = LoggerFactory.getLogger(SiriUtils.class);
	
  public static String getIdWithoutAgency(String id) {
    int startIndex = id.indexOf('_') + 1;
    id = id.substring(startIndex);
    return id;
  }

  /**
   * @param stopTimes The list of all stops this trip makes
   * @param serviceDate
   * @param distance How far in meters the bus is along the trip
   * @param currentStop The stop the bus is presently at
   * @return A list of Siri OnwardCall objects
   */

  public static List<OnwardCall> getOnwardCalls(
      List<TripStopTimeBean> stopTimes, long serviceDate, double distance,
      StopBean currentStop) {

    ArrayList<OnwardCall> onwardCalls = new ArrayList<OnwardCall>();

    HashMap<String, Integer> visitNumberForStop = new HashMap<String, Integer>();
    boolean afterStart = false;
    boolean afterStop = currentStop == null;
    int i = 0;
    for (TripStopTimeBean stopTime : stopTimes) {
      StopBean stop = stopTime.getStop();
      int visitNumber = getVisitNumber(visitNumberForStop, stop);
      if (stopTime.getDistanceAlongTrip() >= distance) {
        afterStart = true;
      }
      if (afterStart) {
        i += 1;
        if (afterStop) {
          OnwardCall onwardCall = new OnwardCall();
          onwardCall.StopPointRef = SiriUtils.getIdWithoutAgency(stop.getId());
          onwardCall.StopPointName = stop.getName();
          onwardCall.VisitNumber = visitNumber;
          onwardCall.Extensions = new DistanceExtensions();

          onwardCall.Extensions.Distances = new Distances();
          onwardCall.Extensions.Distances.DistanceFromCall = stopTime.getDistanceAlongTrip()
              - distance;
          onwardCall.Extensions.Distances.CallDistanceAlongRoute = stopTime.getDistanceAlongTrip();
          onwardCall.Extensions.Distances.StopsFromCall = i - 1;

          /*
           * This is not really that useful without being more certain about
           * what trip we're on, so it's commented out Calendar arrivalTime =
           * new GregorianCalendar(); long millis = serviceDate +
           * stopTime.getArrivalTime() * 1000;
           * arrivalTime.setTimeInMillis(millis); onwardCall.AimedArrivalTime =
           * arrivalTime;
           * 
           * Calendar departureTime = new GregorianCalendar(); millis =
           * serviceDate + stopTime.getDepartureTime() * 1000;
           * departureTime.setTimeInMillis(millis);
           * onwardCall.AimedDepartureTime = departureTime;
           */

          onwardCalls.add(onwardCall);
        }
        if (stop == currentStop) {
          afterStop = true;
        }
      }
    }
    if (onwardCalls.size() == 0) {
      return null;
    }
    return onwardCalls;
  }

  public static int getVisitNumber(HashMap<String, Integer> visitNumberForStop,
      StopBean stop) {
    int visitNumber;
    if (visitNumberForStop.containsKey(stop.getId())) {
      visitNumber = visitNumberForStop.get(stop.getId()) + 1;
    } else {
      visitNumber = 1;
    }
    visitNumberForStop.put(stop.getId(), visitNumber);
    return visitNumber;
  }

  public static MonitoredVehicleJourney getMonitoredVehicleJourney(
      TripDetailsBean trip, Date serviceDate, String vehicleId) {
    TripBean tripBean = trip.getTrip();

    MonitoredVehicleJourney monitoredVehicleJourney = new MonitoredVehicleJourney();

    monitoredVehicleJourney.CourseOfJourneyRef = getIdWithoutAgency(trip.getTripId());
    RouteBean route = tripBean.getRoute();
    monitoredVehicleJourney.LineRef = getIdWithoutAgency(route.getId());
    monitoredVehicleJourney.DirectionRef = tripBean.getDirectionId();
    monitoredVehicleJourney.PublishedLineName = tripBean.getTripHeadsign();
    if (monitoredVehicleJourney.PublishedLineName  == null) {
    	_log.warn("publishedLineName is null!");
    }

    monitoredVehicleJourney.FramedVehicleJourneyRef = new FramedVehicleJourneyRef();
    monitoredVehicleJourney.VehicleRef = vehicleId;

    monitoredVehicleJourney.FramedVehicleJourneyRef.DataFrameRef = String.format(
        "%1$tY-%1$tm-%1$td", serviceDate);
    monitoredVehicleJourney.FramedVehicleJourneyRef.DatedVehicleJourneyRef = trip.getTripId();

    List<TripStopTimeBean> stops = trip.getSchedule().getStopTimes();
    monitoredVehicleJourney.OriginRef = getIdWithoutAgency(stops.get(0).getStop().getId());
    StopBean lastStop = stops.get(stops.size() - 1).getStop();
    monitoredVehicleJourney.DestinationRef = getIdWithoutAgency(lastStop.getId());

    StopBean nextStop = trip.getStatus().getNextStop();
    if (nextStop != null) {
      monitoredVehicleJourney.MonitoredCall = new MonitoredCall();
      monitoredVehicleJourney.MonitoredCall.StopPointRef = nextStop.getId();
      monitoredVehicleJourney.MonitoredCall.StopPointName = nextStop.getName();
      monitoredVehicleJourney.MonitoredCall.VisitNumber = 1; // FIXME: this is
                                                             // theoretically
                                                             // wrong but
                                                             // practically
                                                             // rarely so
    }
    return monitoredVehicleJourney;
  }

  public static String getProgressRateForStatus(String status) {
    if (status == null) {
      return "unknown";
    }
    if (status.equals("default")) {
      return "unknown";
    }
    if (status.equals("IN_PROGRESS")) {
      return "normalProgress";
    }
    if (status.startsWith("LAYOVER")) {
      return "noProgress";
    }
    if (status.equals("STALLED")) {
      return "noProgress";
    }
    return null; /* something unknown */
  }
}
