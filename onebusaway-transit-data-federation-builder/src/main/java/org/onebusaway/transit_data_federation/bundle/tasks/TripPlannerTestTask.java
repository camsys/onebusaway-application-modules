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
package org.onebusaway.transit_data_federation.bundle.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.onebusaway.collections.tuple.Pair;
import org.onebusaway.collections.tuple.Tuples;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.transit_data.model.tripplanning.ConstraintsBean;
import org.onebusaway.transit_data.model.tripplanning.ItinerariesBean;
import org.onebusaway.transit_data.model.tripplanning.TransitLocationBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

public class TripPlannerTestTask implements Runnable {

  private TransitDataService _transitDataService;

  private File _input;

  private Date _time;

  @Autowired
  public void setTransitDataService(TransitDataService transitDataService) {
    _transitDataService = transitDataService;
  }

  public void setInput(File input) {
    _input = input;
  }

  public void setTime(Date time) {
    _time = time;
  }

  public void run() {

    try {
      if (_input == null)
        _input = new File(
            "/Users/bdferris/oba/performance/trip-planner-points.txt");

      if (_time == null) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
            DateFormat.SHORT);
        _time = format.parse("4/2/11 01:30 PM");
      }

      List<Pair<CoordinatePoint>> points = new ArrayList<Pair<CoordinatePoint>>();

      BufferedReader reader = new BufferedReader(new FileReader(_input));
      String line = null;

      while ((line = reader.readLine()) != null) {
        String[] tokens = line.split(" ");
        double lat1 = Double.parseDouble(tokens[0]);
        double lon1 = Double.parseDouble(tokens[1]);
        double lat2 = Double.parseDouble(tokens[2]);
        double lon2 = Double.parseDouble(tokens[3]);
        CoordinatePoint p1 = new CoordinatePoint(lat1, lon1);
        CoordinatePoint p2 = new CoordinatePoint(lat2, lon2);
        Pair<CoordinatePoint> pair = Tuples.pair(p1, p2);
        points.add(pair);
      }

      long tTotal = 0;
      int index = 0;

      ConstraintsBean constraints = new ConstraintsBean();
      constraints.setMaxComputationTime(20000);
      constraints.setResultCount(3);
      constraints.setUseRealTime(true);
      
      for (Pair<CoordinatePoint> pair : points) {
        TransitLocationBean from = new TransitLocationBean(pair.getFirst());
        TransitLocationBean to = new TransitLocationBean(pair.getSecond());
        long tIn = System.currentTimeMillis();
        ItinerariesBean result = _transitDataService.getItinerariesBetween(
            from, to, _time.getTime(), constraints);
        long tOut = System.currentTimeMillis();
        long tDiff = tOut - tIn;
        tTotal += tDiff;
        index++;
        double mu = tTotal / (double) index;
        System.out.println(index + " " + tDiff + " "
            + result.isComputationTimeLimitReached() + " " + mu);
        
        if( index == 400)
          break;
      }
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
