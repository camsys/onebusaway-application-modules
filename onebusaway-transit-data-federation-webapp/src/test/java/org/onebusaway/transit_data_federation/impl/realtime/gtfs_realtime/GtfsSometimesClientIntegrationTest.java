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

import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.calendar.CalendarServiceDataFactory;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.GtfsSometimesHandler;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.GtfsSometimesHandlerImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.AgencyEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockConfigurationEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteCollectionEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.testing.UnitTestingSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.*;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.dateAsLong;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.time;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration(locations = "classpath:org/onebusaway/transit_data_federation/tds-test-with-gtfs-sometimes-client.xml")
@TestPropertySource(properties = { "bundlePath = /tmp/foo"})
public class GtfsSometimesClientIntegrationTest {

    @Autowired
    private TransitDataService _tds;

    @Autowired
    private TransitGraphDao _graph;

    @Autowired
    private GtfsSometimesHandler _handler;

    private static final Logger _log = LoggerFactory.getLogger(GtfsSometimesClientIntegrationTest.class);

    @Before
    public void loadGtfsData() throws IOException {
        GtfsReader reader = new GtfsReader();
        String path = getClass().getResource("/gtfs_sometimes/gtfs_staten_island_S86.zip").getPath();
        reader.setDefaultAgencyId("MTA");
        reader.setInputLocation(new File(path));
        GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
        reader.setEntityStore(dao);
        reader.run();

        for (Agency agency : dao.getAllAgencies()) {
            AgencyEntryImpl aei = new AgencyEntryImpl();
            aei.setId(agency.getId());
            assertTrue(_graph.addAgencyEntry(aei));
        }

        Map<String, StopEntryImpl> stopById = new HashMap<>();

        for (Stop stop : dao.getAllStops()) {
            StopEntryImpl sei = new StopEntryImpl(stop.getId(), stop.getLat(), stop.getLon());
            assertTrue(_graph.addStopEntry(sei));
            stopById.put(stop.getId().getId(), sei);
        }

        CalendarServiceDataFactory csdf = new CalendarServiceDataFactoryImpl(dao);
        CalendarServiceData csd = csdf.createData();
        _graph.updateCalendarServiceData(csd);

        for (Trip trip : dao.getAllTrips()) {
            LocalizedServiceId lsi = new LocalizedServiceId(trip.getServiceId(), csd.getTimeZoneForAgencyId(trip.getId().getAgencyId()));
            TripEntryImpl tei = new TripEntryImpl();
            tei.setId(trip.getId());
            tei.setServiceId(lsi);
            BlockEntryImpl bei = new BlockEntryImpl();
            bei.setId(trip.getId());
            bei.setConfigurations(new ArrayList<>());
            BlockConfigurationEntryImpl.Builder builder = BlockConfigurationEntryImpl.builder();

            tei.setBlock(bei);
            RouteEntryImpl rei = new RouteEntryImpl();
            rei.setId(trip.getRoute().getId());
            tei.setRoute(rei);
            List<StopTimeEntry> stopTimes = new ArrayList<>();
            for (StopTime stopTime : dao.getStopTimesForTrip(trip)) {
                String stopId = stopTime.getStop().getId().getId();
                stopTimes.add(UnitTestingSupport.stopTime(stopTime.getId(), stopById.get(stopId), tei,
                        stopTime.getArrivalTime(), stopTime.getDepartureTime(), (int) Math.round(stopTime.getShapeDistTraveled()),
                        -1, stopTime.getStopSequence()));
            }
            tei.setStopTimes(stopTimes);
            builder.setBlock(bei);
            builder.setTrips(Collections.singletonList(tei));
            builder.setTripGapDistances(new double[] { 0 });
            builder.setServiceIds(new ServiceIdActivation(lsi));
            bei.getConfigurations().add(builder.create());
            assertTrue(_graph.addTripEntry(tei));
        }

        // set handler time
        ((GtfsSometimesHandlerImpl) _handler).setTime(dateAsLong("2018-08-10 12:00"));
    }

    @Test
    @DirtiesContext
    public void testLoadSuccess() {
        TripDetailsBean tripDetails = getTripDetails("CA_G8-Weekday-096000_MISC_545");
        assertEquals(71, tripDetails.getSchedule().getStopTimes().size());
        List<TripStopTimeBean> stopTimes = tripDetails.getSchedule().getStopTimes();
        TripStopTimeBean prev = stopTimes.get(14);
        assertEquals("MTA_201644", prev.getStop().getId());
        assertEquals(15, prev.getGtfsSequence());
    }

    @Test
    @DirtiesContext
    public void testRemoveStopTime() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Collections.singletonList(stopTimeEntity("CA_G8-Weekday-096000_MISC_545", "201645")),
                null,
                dateDescriptors(LocalDate.of(2018, 8, 10)));
        assertTrue(_handler.handleServiceChange(change));
        TripDetailsBean tripDetails = getTripDetails("CA_G8-Weekday-096000_MISC_545");
        assertEquals(70, tripDetails.getSchedule().getStopTimes().size());
        // 201645 is the 16th stop
        List<TripStopTimeBean> stopTimes = tripDetails.getSchedule().getStopTimes();
        TripStopTimeBean prev = stopTimes.get(14);
        assertEquals("MTA_201644", prev.getStop().getId());
        assertEquals(15, prev.getGtfsSequence());
        assertEquals(time(16, 9, 14), prev.getArrivalTime());
        assertEquals(time(16, 9, 14), prev.getDepartureTime());
        TripStopTimeBean next = stopTimes.get(15);
        assertEquals("MTA_201646", next.getStop().getId());
        assertEquals(17, next.getGtfsSequence());
        assertEquals(time(16, 10, 20), next.getArrivalTime());
        assertEquals(time(16, 10, 20), next.getDepartureTime());
    }

    private TripDetailsBean getTripDetails(String tripId) {
        TripDetailsQueryBean query = new TripDetailsQueryBean();
        query.setTripId("MTA NYCT_" + tripId);
        //query.setServiceDate(UnitTestingSupport.dateAsLong("2018-08-14 00:00")/1000);
        ListBean<TripDetailsBean> tripDetailsList = _tds.getTripDetails(query);
        assertEquals(1, tripDetailsList.getList().size());
        return tripDetailsList.getList().get(0);
    }
}
