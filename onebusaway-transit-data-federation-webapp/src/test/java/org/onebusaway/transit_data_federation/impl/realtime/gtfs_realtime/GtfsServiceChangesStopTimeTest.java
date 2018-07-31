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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.beans.ArrivalsAndDeparturesBeanService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.transit_graph.AgencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration(locations = "classpath:org/onebusaway/transit_data_federation/tds-test.xml")
@TestPropertySource(properties = { "bundlePath = /tmp/foo"})
public class GtfsServiceChangesStopTimeTest {

    @Autowired
    private TransitDataService _tds;

    @Autowired
    private TransitGraphDao _dao;

    @Autowired
    private ArrivalsAndDeparturesBeanService _arrivalsAndDeparturesBeanService;

    @Autowired
    private BlockIndexService _blockIndexService;

    private FederatedTransitDataBundle _bundle;


    private static org.slf4j.Logger _log = LoggerFactory.getLogger(GtfsServiceChangesStopTimeTest.class);

    @BeforeClass
    public static void setupTest() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        _log.info("logger enabled!");

    }

    public void setupRun() {
    }

    @Test
    @Transactional
    @DirtiesContext
    public void testDeleteStopTimes() {

        addSeedData();
        // remove to catch empty collection bugs
        assertTrue(_dao.deleteStopTime(aid("tripA"), aid("a")));
        assertTrue(_dao.deleteStopTime(aid("tripA"), aid("b")));
        assertTrue(_dao.deleteStopTime(aid("tripA"), aid("c")));
        assertNull(_dao.getTripEntryForId(aid("tripA")).getStopTimes());
        for (TripEntry aTrip : _dao.getAllTrips()) {
            if (aTrip.getId().equals(aid("a"))) {
                assertNull(aTrip.getStopTimes());
            }
        }

        // put it back
        _dao.insertStopTime(aid("tripA"), aid("a"), 30, 90, 25);
        assertNotNull(_dao.getAllTrips().get(0).getStopTimes());
        assertEquals(1, _dao.getAllTrips().get(0).getStopTimes().size());

        _dao.insertStopTime(aid("tripA"), aid("b"), 120, 150, 100);
        _dao.insertStopTime(aid("tripA"), aid("c"), 180, 210, 200);

        assertEquals(3, _dao.getAllTrips().get(0).getStopTimes().size());

        assertNotNull(_tds.getTrip("1_tripA"));


    }

    @Test
    @Transactional
    @DirtiesContext
    public void testGetArrivalsAndDepartures() {
        addSeedData();


        // now search for that trip
        ArrivalsAndDeparturesQueryBean query = new ArrivalsAndDeparturesQueryBean();
        query.setMinutesAfter(1440);
        query.setMinutesAfter(1440);
        query.setTime(System.currentTimeMillis());

        List<ArrivalAndDepartureBean> arrivalsAndDeparturesByStopId = _arrivalsAndDeparturesBeanService.getArrivalsAndDeparturesByStopId(aid("a"), query);
        assertNotNull(arrivalsAndDeparturesByStopId);
        assertEquals(1, arrivalsAndDeparturesByStopId.size());
        ArrivalAndDepartureBean ad = arrivalsAndDeparturesByStopId.get(0);
        assertEquals(aid("tripA").toString(), ad.getTrip().getId());

    }

    @Test
    public void testBlockIndexService() {
        addSeedData();

        StopEntry stopEntry = _dao.getStopEntryForId(aid("a"));
        assertNotNull(stopEntry);

        StopEntryImpl stopEntryImpl = (StopEntryImpl) stopEntry;
        assertNotNull(stopEntryImpl.getStopTimeIndices());
        assertEquals(1, stopEntryImpl.getStopTimeIndices().size());

        List<BlockStopTimeIndex> stopTimeIndicesForStop = _blockIndexService.getStopTimeIndicesForStop(stopEntry);
        assertNotNull(stopTimeIndicesForStop);
        assertEquals(1, stopTimeIndicesForStop.size());

        assertEquals(1, _blockIndexService.getBlockTripIndices().size());
        assertEquals(0, _blockIndexService.getFrequencyBlockTripIndices().size());
        // we don't have a layover between two trips
        assertEquals(0, _blockIndexService.getBlockLayoverIndicesForAgencyId("1").size());
        assertEquals(1, _blockIndexService.getBlockTripIndicesForAgencyId("1").size());
        assertEquals(1, _blockIndexService.getBlockTripIndicesForRouteCollectionId(aid("route1")).size());
        assertEquals(1, _blockIndexService.getAllBlockSequenceIndices().size());

    }

    private void addSeedData() {
        assertNotNull(_tds);
        assertNotNull(_dao);
        // confirm empty graph
        assertEquals(0, _dao.getAllStops().size());
        assertEquals(0, _dao.getAllTrips().size());

        // trips need a block, even if its empty
        // empty blocks have a block_id == trip_id!!!
        BlockEntryImpl block1 = block("tripA");

        // add a trip
        TripEntryImpl tripA = trip("tripA", "sA");
        // trips need a route
        RouteEntryImpl route1 = route("route1");
        tripA.setRoute(route1);


        assertFalse(_dao.addTripEntry(tripA));

        // add a stop
        StopEntryImpl stopA = stop("a", 47.5, -122.5);
        assertTrue(_dao.addStopEntry(stopA));
        StopEntryImpl stopB = stop("b", 47.6, -122.4);
        assertTrue(_dao.addStopEntry(stopB));

        stopTime(0, stopA, tripA, time(0, 15), time(0, 17), 35);
        stopTime(1, stopB, tripA, time(0, 30), time(0, 31), 95);

        //unfortunately its illegal to create a blockConfiguration without stop times
        BlockConfigurationEntry blockConfigA = blockConfiguration(block1, serviceIds(lsids("sA"), lsids()), tripA);

        assertNotNull(blockConfigA.getStopTimes());
        assertEquals(2, blockConfigA.getStopTimes().size());
        assertNotNull(blockConfigA.getServiceIds());
        assertFalse(blockConfigA.getServiceIds().getActiveServiceIds().isEmpty());

        assertTrue(_dao.addTripEntry(tripA));

        BlockEntry blockEntry = _dao.getBlockEntryForId(aid("tripA"));
        assertNotNull(blockEntry);
        assertNotNull(blockConfigA.getStopTimes());
        assertEquals(2, blockConfigA.getStopTimes().size());
        assertEquals(1, blockEntry.getConfigurations().size());
        assertEquals(2, blockEntry.getConfigurations().get(0).getStopTimes().size());


        assertEquals(1, _dao.getAllTrips().size());
        assertNotNull(_dao.getTripEntryForId(aid("tripA")));
        assertNotNull(_dao.getAllTrips().get(0));

        assertEquals(aid("tripA"), _dao.getTripEntryForId(aid("tripA")).getId());

        StopEntryImpl blockCheck = (StopEntryImpl)_dao.getStopEntryForId(aid("a"));
        assertEquals(1, blockCheck.getStopTimeIndices().size());

        // add a stop
        StopEntryImpl stopC = stop("c", 47.6, -122.8);
        assertTrue(_dao.addStopEntry(stopC));

        assertEquals(3, _dao.getAllStops().size());
        assertNotNull(_dao.getStopEntryForId(aid("c")));

        StopEntryImpl stopD = stop("d", 47.8, -122.9);
        assertTrue(_dao.addStopEntry(stopD));

        // update the calendar info
        String[] tripIds = {aid("tripA").toString()};
        addCalendarSeeData(Arrays.asList(tripIds));


        // we have two stop times
        assertEquals(2, _dao.getAllTrips().get(0).getStopTimes().size());
        assertEquals(2, blockEntry.getConfigurations().get(0).getStopTimes().size());

        // add a third stop time
        assertEquals(1, blockEntry.getConfigurations().size());
        _dao.insertStopTime(aid("tripA"), aid("c"), 120, 180, 100);
        assertNotNull(_dao.getAllTrips().get(0).getStopTimes());
        assertEquals(3, _dao.getAllTrips().get(0).getStopTimes().size());
        assertEquals(1, blockEntry.getConfigurations().size());
        assertEquals(3, blockEntry.getConfigurations().get(0).getStopTimes().size());
        // make sure block config reflects this change as well
        blockEntry = _dao.getBlockEntryForId(aid("tripA"));
        assertNotNull(blockEntry);
        assertEquals(1, blockEntry.getConfigurations().size());
        blockConfigA = blockEntry.getConfigurations().get(0);
        assertNotNull(blockConfigA.getStopTimes());
        // make sure block is updated with new stop times!!!
        assertEquals(3, blockConfigA.getStopTimes().size());


    }

    private void addCalendarSeeData(List<String> tripAgencyIds) {
        CalendarServiceData data;

        ServiceDate dStart = new ServiceDate(2010, 2, 10);

        ServiceDate dEnd = new ServiceDate(2030, 2, 24);

        ServiceCalendar c1 = calendar(aid("sA"), dStart, dEnd, "1111111");
        List<ServiceCalendar> calendars = new ArrayList<ServiceCalendar>();
        calendars.add(c1);
        List<ServiceCalendarDate> calendarDates = new ArrayList<ServiceCalendarDate>();

        Map<AgencyAndId, List<String>> tripAgencyIdsReferencingServiceId = new HashMap<AgencyAndId, List<String>>();
        tripAgencyIdsReferencingServiceId.put(aid("sA"), tripAgencyIds);
        Map<String, TimeZone> timeZoneMapByAgencyId = new HashMap<String, TimeZone>();
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        assertNotNull(tz);
        timeZoneMapByAgencyId.put(aid("tripA").getAgencyId(), tz);
        CalendarServiceDataFactoryImpl csdfi = new CalendarServiceDataFactoryImpl();
        data = csdfi.updateData(adapt(_dao.getAllAgencies()),
                calendars,
                calendarDates,
                tripAgencyIdsReferencingServiceId,
                timeZoneMapByAgencyId);
        _dao.updateCalendarServiceData(data);
    }

    private Collection<Agency> adapt(List<AgencyEntry> allAgencies) {
        Collection<Agency> agencies = new ArrayList<Agency>();
        for (AgencyEntry ae : allAgencies) {
            Agency a = new Agency();
            a.setId(ae.getId());
            TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
            assertNotNull(tz);
            a.setTimezone(tz.getDisplayName());
            agencies.add(a);
        }
        return agencies;
    }

}
