/**
 * Copyright (C) 2018 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphDaoImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.aid;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stop;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.stopTime;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.trip;

public class TransitGraphDaoImplTest {

    private TripEntryImpl tripA, tripB, tripC, tripD, tripE, tripF, tripG, tripH, tripI, tripJ;
    private StopEntryImpl stopA, stopB, stopC;
    private StopTimeEntryImpl stopAA, stopBA, stopCA, stopCB, stopBB, stopAB, stopAC, stopBC, stopCC, stopAD, stopBD, stopCD;
    private TransitGraphDaoImpl dao = new TransitGraphDaoImpl();
    private TransitGraphImpl graph = new TransitGraphImpl();

    @Before
    public void setup() {
        dao.setTripPlannerGraph(graph);

        stopA = stop("a", 47.5, -122.5);
        stopB = stop("b", 47.6, -122.4);
        stopC = stop("c", 47.5, -122.3);

        tripA = trip("tripA", "serviceId");
        tripB = trip("tripB", "serviceId");
        tripC = trip("tripC", "serviceId");
        tripD = trip("tripD", "serviceId");
        tripE = trip("tripE", "serviceId");
        tripF = trip("tripF", "serviceId");
        tripG = trip("tripG", "serviceId");
        tripH = trip("tripH", "serviceId");
        tripI = trip("tripI", "serviceId");
        tripJ = trip("tripJ", "serviceId");

        stopAA = stopTime(0, stopA, tripA, 30, 90, 0);
        stopBA = stopTime(1, stopB, tripA, 120, 120, 100);
        stopCA = stopTime(2, stopC, tripA, 180, 210, 200);

        stopCB = stopTime(3, stopC, tripB, 240, 240, 300);
        stopBB = stopTime(4, stopB, tripB, 270, 270, 400);
        stopAB = stopTime(5, stopA, tripB, 300, 300, 500);

        stopAC = stopTime(6, stopA, tripC, 360, 360, 600);
        stopBC = stopTime(7, stopB, tripC, 390, 390, 700);
        stopCC = stopTime(8, stopC, tripC, 420, 420, 800);

        // trip C and D are the same but on different runs
        stopAD = stopTime(6, stopA, tripD, 360, 360, 600);
        stopBD = stopTime(7, stopB, tripD, 390, 390, 700);
        stopCD = stopTime(8, stopC, tripD, 420, 420, 800);

        graph.putTripEntry(tripA);
        // we need to manually build index!
        graph.refreshTripMapping();

    }
    @Test
    public void testDeleteTrip() {

        assertNotNull(dao.getAllTrips());
        assertEquals(1, dao.getAllTrips().size());
        assertEquals("tripA", dao.getAllTrips().get(0).getId().getId());

        assertNotNull(dao.getTripEntryForId(aid("tripA")));
        assertEquals("tripA", dao.getTripEntryForId(aid("tripA")).getId().getId());

        // now delete!
        assertTrue(dao.deleteTripEntryForId(aid("tripA")));

        assertNotNull(dao.getAllTrips());
        assertEquals(0, dao.getAllTrips().size());
    }

    @Test
    public void testDeleteStopTime() {
        assertNotNull(dao.getAllTrips());
        assertEquals(1, dao.getAllTrips().size());
        TripEntry tripEntry = dao.getAllTrips().get(0);
        assertEquals("tripA", tripEntry.getId().getId());

        assertNotNull(tripEntry.getStopTimes());
        assertEquals(3, tripEntry.getStopTimes().size());

        assertEquals(stopCA.getStop().getId().getId(), tripEntry.getStopTimes().get(2).getStop().getId().getId());


        assertEquals(3, dao.getTripEntryForId(aid("tripA")).getStopTimes().size());
        assertEquals(stopCA.getStop().getId().getId(), dao.getTripEntryForId(aid("tripA")).getStopTimes().get(2).getStop().getId().getId());
        //remove stopCA
        dao.deleteStopTime(aid("tripA"), stopCA.getStop().getId());

        assertEquals(2, tripEntry.getStopTimes().size());
        // note: stopTime are shared across index!
        assertEquals(2, dao.getTripEntryForId(aid("tripA")).getStopTimes().size());
        StopEntry stopEntry = dao.getStopEntryForId(stopA.getId());


    }
}
