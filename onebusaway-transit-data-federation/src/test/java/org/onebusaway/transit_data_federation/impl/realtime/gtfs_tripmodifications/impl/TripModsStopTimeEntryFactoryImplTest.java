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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.StopEntryData;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class TripModsStopTimeEntryFactoryImplTest {

    @Mock
    private TripEntryImpl tripEntry;

    private TripModsStopTimeEntryFactoryImpl factory;

    @Before
    public void setUp() {
        factory = new TripModsStopTimeEntryFactoryImpl();
    }

    @Test
    public void createSetsArrivalAndDepartureToSameValue() {
        StopEntryData stopEntryData = buildStopEntryData("MTA", "stop1", 40.7128, -74.0060);

        StopTimeEntryImpl result = factory.create(stopEntryData, tripEntry, 3600, -999, -999.0);

        assertEquals(3600, result.getArrivalTime());
        assertEquals(3600, result.getDepartureTime());
    }

    @Test
    public void createAssignsCorrectStopIdLatLon() {
        AgencyAndId stopId = new AgencyAndId("MTA", "stop42");
        StopEntryData stopEntryData = new StopEntryData(stopId, 40.1234, -73.5678, null);

        StopTimeEntryImpl result = factory.create(stopEntryData, tripEntry, 1000, -999, -999.0);

        assertEquals(stopId, result.getStop().getId());
        assertEquals(40.1234, result.getStop().getStopLat(), 0.00001);
        assertEquals(-73.5678, result.getStop().getStopLon(), 0.00001);
    }

    @Test
    public void createAssignsTripEntry() {
        StopEntryData stopEntryData = buildStopEntryData("MTA", "stop1", 40.0, -73.0);

        StopTimeEntryImpl result = factory.create(stopEntryData, tripEntry, 1000, -999, -999.0);

        assertSame(tripEntry, result.getTrip());
    }

    @Test
    public void createAssignsGtfsStopSequence() {
        StopEntryData stopEntryData = buildStopEntryData("MTA", "stop1", 40.0, -73.0);

        StopTimeEntryImpl result = factory.create(stopEntryData, tripEntry, 1000, 7, -999.0);

        assertEquals(7, result.getGtfsSequence());
    }

    @Test
    public void createAssignsDefaultGtfsStopSequenceWhenSentinel() {
        StopEntryData stopEntryData = buildStopEntryData("MTA", "stop1", 40.0, -73.0);

        StopTimeEntryImpl result = factory.create(stopEntryData, tripEntry, 1000, -999, -999.0);

        assertEquals(-999, result.getGtfsSequence());
    }

    @Test
    public void createAssignsShapeDistTraveled() {
        StopEntryData stopEntryData = buildStopEntryData("MTA", "stop1", 40.0, -73.0);

        StopTimeEntryImpl result = factory.create(stopEntryData, tripEntry, 1000, -999, 123.45);

        assertEquals(123.45, result.getShapeDistTraveled(), 0.00001);
    }

    @Test
    public void createAssignsDefaultShapeDistTraveledWhenSentinel() {
        StopEntryData stopEntryData = buildStopEntryData("MTA", "stop1", 40.0, -73.0);

        StopTimeEntryImpl result = factory.create(stopEntryData, tripEntry, 1000, -999, -999.0);

        assertEquals(-999.0, result.getShapeDistTraveled(), 0.00001);
    }

    @Test
    public void createReturnsNewInstanceEachCall() {
        StopEntryData stopEntryData = buildStopEntryData("MTA", "stop1", 40.0, -73.0);

        StopTimeEntryImpl first  = factory.create(stopEntryData, tripEntry, 1000, -999, -999.0);
        StopTimeEntryImpl second = factory.create(stopEntryData, tripEntry, 1000, -999, -999.0);

        assertNotSame(first, second);
    }

    @Test
    public void createProducesIndependentStopEntriesForDifferentStops() {
        StopEntryData stopA = buildStopEntryData("MTA", "stopA", 40.0, -73.0);
        StopEntryData stopB = buildStopEntryData("MTA", "stopB", 41.0, -74.0);

        StopTimeEntryImpl resultA = factory.create(stopA, tripEntry, 1000, -999, -999.0);
        StopTimeEntryImpl resultB = factory.create(stopB, tripEntry, 2000, -999, -999.0);

        assertNotSame(resultA.getStop(), resultB.getStop());
        assertEquals(new AgencyAndId("MTA", "stopA"), resultA.getStop().getId());
        assertEquals(new AgencyAndId("MTA", "stopB"), resultB.getStop().getId());
        assertEquals(1000, resultA.getArrivalTime());
        assertEquals(2000, resultB.getArrivalTime());
    }


    private StopEntryData buildStopEntryData(String agency, String stopIdStr,
                                             double lat, double lon) {
        return new StopEntryData(new AgencyAndId(agency, stopIdStr), lat, lon, null);
    }
}