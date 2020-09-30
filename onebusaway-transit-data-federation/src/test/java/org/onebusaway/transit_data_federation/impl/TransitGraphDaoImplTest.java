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
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
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

        stopAA = stopTime(0, stopA, tripA, 30, 90, 25);
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
        graph.putTripEntry(tripB);
        graph.putTripEntry(tripC);
        graph.putTripEntry(tripD);
        graph.putStopEntry(stopA);
        graph.putStopEntry(stopB);
        graph.putStopEntry(stopC);
        // we need to manually build trip index!
        graph.refreshTripMapping();
        // we need to manually build stop index!
        graph.refreshStopMapping();

    }
    @Test

    public void testDeleteTrip() {

        assertNotNull(dao.getAllTrips());
        assertEquals(4, dao.getAllTrips().size());
        assertEquals("tripA", dao.getAllTrips().get(0).getId().getId());

        assertNotNull(dao.getTripEntryForId(aid("tripA")));
        assertEquals("tripA", dao.getTripEntryForId(aid("tripA")).getId().getId());

        // now delete!
        assertTrue(dao.deleteTripEntryForId(aid("tripA")));

        assertNotNull(dao.getAllTrips());
        assertEquals(3, dao.getAllTrips().size());
    }

    @Test
    public void testDeleteStopTime() {
        assertNotNull(dao.getAllTrips());
        assertEquals(4, dao.getAllTrips().size());
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

    @Test
    public void testUpdateStopTime() {

        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        StopTimeEntry stopTimeEntry = tripA.getStopTimes().get(2);
        assertEquals(stopCA.getStop().getId().getId(), stopTimeEntry.getStop().getId().getId());
        assertEquals(180, stopTimeEntry.getArrivalTime());
        dao.updateStopTime(aid("tripA"), stopCA.getStop().getId(), -1,-1,185, 186);

        tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(185, tripA.getStopTimes().get(2).getArrivalTime());
        assertEquals(186, tripA.getStopTimes().get(2).getDepartureTime());
    }


    @Test
    public void testAddStopTime0Dist() {
        // insert at 0 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), 15, 60, 10);

        assertEquals(4, tripA.getStopTimes().size());
        tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(tripA.getStopTimes().get(0).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime1Dist() {
        // insert at 1 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), 45, 100, 75);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(1).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime2Dist() {
        // insert at 2 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), 150, 180, 150);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(2).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime3Dist() {
        // insert at 3 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), 220, 250, 300);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(3).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime0Arr() {
        // insert at 0 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), 15, 60, -1);

        assertEquals(4, tripA.getStopTimes().size());
        tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(tripA.getStopTimes().get(0).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime1Arr() {
        // insert at 1 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), 45, 100, -1);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(1).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime2Arr() {
        // insert at 2 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), 150, 180, -1);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(2).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime3Arr() {
        // insert at 3 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), 220, 250, -1);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(3).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime0Dept() {
        // insert at 0 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), -1, 60, -1);

        assertEquals(4, tripA.getStopTimes().size());
        tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(tripA.getStopTimes().get(0).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime1Dept() {
        // insert at 1 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), -1, 100, -1);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(1).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime2Dept() {
        // insert at 2 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), -1, 180, -1);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(2).getStop().getId(), stopAD.getStop().getId());
    }

    @Test
    public void testAddStopTime3Dept() {
        // insert at 3 position
        TripEntry tripA = dao.getTripEntryForId(aid("tripA"));
        assertEquals(3, tripA.getStopTimes().size());

        dao.insertStopTime(aid("tripA"), stopAD.getStop().getId(), -1, 250, -1);

        assertEquals(4, tripA.getStopTimes().size());
        assertEquals(tripA.getStopTimes().get(3).getStop().getId(), stopAD.getStop().getId());
    }

}
