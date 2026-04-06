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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.StopEntryData;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsStopTimeEntryFactory;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.springframework.stereotype.Component;

@Component
public class TripModsStopTimeEntryFactoryImpl implements TripModsStopTimeEntryFactory {

    @Override
    public StopTimeEntryImpl create(StopEntryData stopEntryData,
                                    TripEntryImpl tripEntry,
                                    int arrivalTime,
                                    int gtfsStopSequence,
                                    double shapeDistanceTraveled) {

        StopEntryImpl stopEntry = new StopEntryImpl(stopEntryData.getStopId(), stopEntryData.getLat(), stopEntryData.getLon());

        StopTimeEntryImpl stopTimeEntry = new StopTimeEntryImpl();
        stopTimeEntry.setTrip(tripEntry);
        stopTimeEntry.setStop(stopEntry);
        stopTimeEntry.setArrivalTime(arrivalTime);
        stopTimeEntry.setDepartureTime(arrivalTime); // departure = arrival per spec
        stopTimeEntry.setShapeDistTraveled(shapeDistanceTraveled);
        stopTimeEntry.setGtfsSequence(gtfsStopSequence);

        // Might not want to set this here.
        // This might be cumulative (sequence of ALL stops)
        //stopTimeEntry.setSequence(stopSequence);

        return stopTimeEntry;
    }
}
