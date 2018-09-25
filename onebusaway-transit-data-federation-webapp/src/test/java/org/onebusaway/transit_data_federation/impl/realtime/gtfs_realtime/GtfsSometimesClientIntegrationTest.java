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
import com.camsys.transit.servicechange.field_descriptors.AbstractFieldDescriptor;
import com.camsys.transit.servicechange.field_descriptors.TripsFields;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.calendar.CalendarServiceDataFactory;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.bundle.tasks.transit_graph.StopTimeEntriesFactory;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.GtfsSometimesHandler;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.GtfsSometimesHandlerImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.AgencyEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockConfigurationEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphDaoImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;
import org.onebusaway.transit_data_federation.services.beans.TripBeanService;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

    @Autowired
    private TripBeanService _tripBeanService;

    @Autowired
    private StopTimeEntriesFactory _stopTimesEntriesFactory;

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

        for (AgencyAndId shapeId : dao.getAllShapeIds()) {
            List<ShapePoint> points = new ArrayList<>(dao.getShapePointsForShapeId(shapeId));
            points.sort(Comparator.comparingInt(ShapePoint::getSequence));
            int size = points.size();
            double[] lat = new double[size];
            double[] lon = new double[size];
            double[] distance = new double[size];
            for (int i = 0; i < size; i++) {
                ShapePoint pt = points.get(i);
                lat[i] = pt.getLat();
                lon[i] = pt.getLon();
                distance[i] = pt.getDistTraveled();
            }
            ShapePoints shapePoints = new ShapePoints();
            shapePoints.setLats(lat);
            shapePoints.setLons(lon);
            shapePoints.setShapeId(shapeId);
            shapePoints.setDistTraveled(distance);
            shapePoints.ensureDistTraveled();
            assertTrue(_graph.addShape(shapePoints));
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
            tei.setDirectionId(trip.getDirectionId());
            tei.setServiceId(lsi);
            tei.setShapeId(trip.getShapeId());
            BlockEntryImpl bei = new BlockEntryImpl();
            bei.setId(trip.getId());
            bei.setConfigurations(new ArrayList<>());
            BlockConfigurationEntryImpl.Builder builder = BlockConfigurationEntryImpl.builder();

            tei.setBlock(bei);
            RouteEntryImpl rei = new RouteEntryImpl();
            rei.setTrips(new ArrayList<>());
            rei.getTrips().add(tei);
            rei.setId(trip.getRoute().getId());
            tei.setRoute(rei);
            ShapePoints shape = _graph.getShape(trip.getShapeId());
            List<StopTimeEntryImpl> stopTimes = _stopTimesEntriesFactory.processStopTimes(((TransitGraphDaoImpl) _graph).getGraph(),
                    dao.getStopTimesForTrip(trip), tei, shape);
            tei.setStopTimes(new ArrayList<>(stopTimes));
            builder.setBlock(bei);
            builder.setTrips(Collections.singletonList(tei));
            builder.setTripGapDistances(new double[] { 0 });
            builder.setServiceIds(new ServiceIdActivation(lsi));
            bei.getConfigurations().add(builder.create());
            assertTrue(_graph.addTripEntry(tei, tripNarrative(trip)));
        }

        // set handler time
        ((GtfsSometimesHandlerImpl) _handler).setTime(dateAsLong("2018-08-10 12:00"));
    }

    private TripNarrative tripNarrative(Trip trip) {
        return TripNarrative.builder()
                .setRouteShortName(trip.getRouteShortName())
                .setTripHeadsign(trip.getTripHeadsign())
                .setTripShortName(trip.getTripShortName()).create();
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

    @Test
    @DirtiesContext
    public void testRemoveMultipleStopTimesDifferentTrips() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Arrays.asList(stopTimeEntity("CA_G8-Weekday-096000_MISC_545", "201645"),
                        stopTimeEntity("CA_C8-Weekday-099000_S7686_307", "203190")),
                null,
                dateDescriptors(LocalDate.of(2018, 8, 10)));
        assertTrue(_handler.handleServiceChange(change));

        TripDetailsBean tripDetails1 = getTripDetails("CA_G8-Weekday-096000_MISC_545");
        assertEquals(70, tripDetails1.getSchedule().getStopTimes().size());
        List<TripStopTimeBean> stopTimes1 = tripDetails1.getSchedule().getStopTimes();
        TripStopTimeBean stop1 = stopTimes1.get(15);
        assertEquals("MTA_201646", stop1.getStop().getId());
        assertEquals(17, stop1.getGtfsSequence());
        assertEquals(time(16, 10, 20), stop1.getArrivalTime());
        assertEquals(time(16, 10, 20), stop1.getDepartureTime());

        TripDetailsBean tripDetails2 = getTripDetails("CA_C8-Weekday-099000_S7686_307");
        assertEquals(70, tripDetails2.getSchedule().getStopTimes().size());
        List<TripStopTimeBean> stopTimes2 = tripDetails2.getSchedule().getStopTimes();
        TripStopTimeBean stop2 = stopTimes2.get(4);
        assertEquals("MTA_200180", stop2.getStop().getId());
        assertEquals(6, stop2.getGtfsSequence());
        assertEquals(time(16, 33, 32), stop2.getArrivalTime());
        assertEquals(time(16, 33, 32), stop2.getDepartureTime());
    }

    @Test
    @DirtiesContext
    public void testRemoveMultipleStopTimesSameTrip() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.DELETE,
                Arrays.asList(stopTimeEntity("CA_G8-Weekday-096000_MISC_545", "201645"),
                        stopTimeEntity("CA_G8-Weekday-096000_MISC_545", "201646")),
                null,
                dateDescriptors(LocalDate.of(2018, 8, 10)));
        assertTrue(_handler.handleServiceChange(change));

        TripDetailsBean tripDetails = getTripDetails("CA_G8-Weekday-096000_MISC_545");
        assertEquals(69, tripDetails.getSchedule().getStopTimes().size());
        List<TripStopTimeBean> stopTimes1 = tripDetails.getSchedule().getStopTimes();
        TripStopTimeBean stop1 = stopTimes1.get(15);
        assertEquals("MTA_201647", stop1.getStop().getId());
        assertEquals(18, stop1.getGtfsSequence());
        assertEquals(time(16, 11, 0), stop1.getArrivalTime());
        assertEquals(time(16, 11, 0), stop1.getDepartureTime());
    }

    @Test
    @DirtiesContext
    public void testAddStopTime() {

        String tripId = "CA_G8-Weekday-096000_MISC_545";

        // Check that adding a stop time does not change the narrative values
        TripBean trip = _tripBeanService.getTripForId(new AgencyAndId("MTA NYCT", tripId));
        assertNotNull(trip);
        assertEquals("MTA NYCT_" + tripId, trip.getId());
        assertEquals("LTD OAKWOOD MILL RD", trip.getTripHeadsign());

        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                 stopTimesFieldsList(tripId,
                        time(16, 47, 20), time(16, 47, 22),
                         "200176", 68), // stop_sequence is ignored
                dateDescriptors(LocalDate.of(2018, 8, 10)));
        assertTrue(_handler.handleServiceChange(change));

        TripDetailsBean tripDetails = getTripDetails(tripId);
        assertEquals(72, tripDetails.getSchedule().getStopTimes().size());
        List<TripStopTimeBean> stopTimes = tripDetails.getSchedule().getStopTimes();

        trip = _tripBeanService.getTripForId(new AgencyAndId("MTA NYCT", tripId));
        assertNotNull(trip);
        assertEquals("MTA NYCT_" + tripId, trip.getId());
        assertEquals("LTD OAKWOOD MILL RD", trip.getTripHeadsign());

        TripStopTimeBean prev = stopTimes.get(66);
        assertEquals("MTA_203563", prev.getStop().getId());
        assertEquals(67, prev.getGtfsSequence());
        assertEquals(time(16, 47, 0), prev.getArrivalTime());
        assertEquals(time(16, 47, 0), prev.getDepartureTime());

        TripStopTimeBean added = stopTimes.get(67);
        assertEquals("MTA_200176", added.getStop().getId());
        assertEquals(time(16, 47, 20), added.getArrivalTime());
        assertEquals(time(16, 47, 22), added.getDepartureTime());

        TripStopTimeBean next = stopTimes.get(68);
        assertEquals("MTA_203564", next.getStop().getId());
        assertEquals(68, next.getGtfsSequence());
        assertEquals(time(16, 47, 38), next.getArrivalTime());
        assertEquals(time(16, 47, 38), next.getDepartureTime());
    }

    @Test
    @DirtiesContext
    public void testAddStopTimeDifferentRoute() {

        // Duplicate some logic in StopForIdAction to ensure we can find this trip...
        // Before we add it, this stop does not exist for the route.
        StopsForRouteBean stopsForRouteBean = _tds.getStopsForRoute("MTA NYCT_S86");
        boolean stopFound = false;
        for (StopBean stop : stopsForRouteBean.getStops()) {
            stopFound |= stop.getId().equals("MTA_200001");
        }
        assertFalse(stopFound);

        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ADD,
                null,
                stopTimesFieldsList("CA_G8-Weekday-096000_MISC_545",
                        time(16, 47, 20), time(16, 47, 22),
                        "200001", 68), // stop_sequence is ignored
                dateDescriptors(LocalDate.of(2018, 8, 10)));
        assertTrue(_handler.handleServiceChange(change));

        TripDetailsBean tripDetails = getTripDetails("CA_G8-Weekday-096000_MISC_545");
        assertEquals(72, tripDetails.getSchedule().getStopTimes().size());

        stopsForRouteBean = _tds.getStopsForRoute("MTA NYCT_S86");
        stopFound = false;
        for (StopBean stop : stopsForRouteBean.getStops()) {
            stopFound |= stop.getId().equals("MTA_200001");
        }
        assertTrue(stopFound);

        assertTrue(_tds.stopHasRevenueServiceOnRoute("MTA NYCT", "MTA_200001", "MTA NYCT_S86", "1"));
    }

    @Test
    @DirtiesContext
    public void testAlterStopTime() {
        ServiceChange change = serviceChange(Table.STOP_TIMES,
                ServiceChangeType.ALTER,
                Collections.singletonList(stopTimeEntity("CA_G8-Weekday-096000_MISC_545", "203564")),
                stopTimesFieldsList("CA_G8-Weekday-096000_MISC_545",
                        time(16, 47, 42), time(16, 47, 44), // delay by 4-6 seconds
                        "203564", 68),
                dateDescriptors(LocalDate.of(2018, 8, 10)));
        assertTrue(_handler.handleServiceChange(change));

        TripDetailsBean tripDetails = getTripDetails("CA_G8-Weekday-096000_MISC_545");
        assertEquals(71, tripDetails.getSchedule().getStopTimes().size());
        List<TripStopTimeBean> stopTimes = tripDetails.getSchedule().getStopTimes();

        TripStopTimeBean prev = stopTimes.get(66);
        assertEquals("MTA_203563", prev.getStop().getId());
        assertEquals(67, prev.getGtfsSequence());
        assertEquals(time(16, 47, 0), prev.getArrivalTime());
        assertEquals(time(16, 47, 0), prev.getDepartureTime());

        TripStopTimeBean altered = stopTimes.get(67);
        assertEquals("MTA_203564", altered.getStop().getId());
        assertEquals(68, altered.getGtfsSequence());
        assertEquals(time(16, 47, 42), altered.getArrivalTime());
        assertEquals(time(16, 47, 44), altered.getDepartureTime());

        TripStopTimeBean next = stopTimes.get(68);
        assertEquals("MTA_203565", next.getStop().getId());
        assertEquals(69, next.getGtfsSequence());
        assertEquals(time(16, 47, 58), next.getArrivalTime());
        assertEquals(time(16, 47, 58), next.getDepartureTime());
    }

    @Test
    @DirtiesContext
    public void testAlterTripChangeShape() {
        TripDetailsBean tripDetails = getTripDetails("CA_G8-Weekday-096000_MISC_545");

        //sneak in a detour between shape points 98 and 99 (stops 200184 and 200185)
        AgencyAndId oldShapeId = AgencyAndId.convertFromString(tripDetails.getTrip().getShapeId());
        ShapePoints oldShapePoints = _graph.getShape(oldShapeId);
        int j = 0;
        List<AbstractFieldDescriptor> newShapeFields = new ArrayList<>();
        for (int i = 0; i < oldShapePoints.getSize(); i++) {
            newShapeFields.add(shapeFields("newShape", oldShapePoints.getLatForIndex(i), oldShapePoints.getLonForIndex(i), j));
            j++;
            if (i == 98) {
                // Added points: 40.626795, -74.074144; 40.626111, -74.073972
                newShapeFields.add(shapeFields("newShape", 40.626795, -74.074144, j));
                j++;
                newShapeFields.add(shapeFields("newShape", 40.626111, -74.073972, j));
                j++;
            }
        }

        ServiceChange addShape = serviceChange(Table.SHAPES,
                ServiceChangeType.ADD,
               null,
                newShapeFields,
                dateDescriptors(LocalDate.of(2018, 8, 10)));

        TripsFields fields = new TripsFields();
        fields.setShapeId("newShape");
        ServiceChange alterTrip = serviceChange(Table.TRIPS,
                ServiceChangeType.ALTER,
                Collections.singletonList(tripEntity("CA_G8-Weekday-096000_MISC_545")),
                Collections.singletonList(fields),
                dateDescriptors(LocalDate.of(2018, 8, 10)));

        assertEquals(2, _handler.handleServiceChanges(Arrays.asList(addShape, alterTrip)));

        List<TripStopTimeBean> oldSchedule = tripDetails.getSchedule().getStopTimes();
        List<TripStopTimeBean> newSchedule = getTripDetails("CA_G8-Weekday-096000_MISC_545").getSchedule().getStopTimes();

        // skip first stop - ends up with unset shape dist.
        for (int i = 1; i < newSchedule.size(); i++) {
            TripStopTimeBean oldStopTime = oldSchedule.get(i);
            TripStopTimeBean newStopTime = newSchedule.get(i);

            // before stop 200184 (10th stop), distance should be the same. After, the detour has happened.
            assertEquals(oldStopTime.getStop().getId(), newStopTime.getStop().getId());
            if (i <= 9) {
                assertEquals(oldStopTime.getDistanceAlongTrip(), newStopTime.getDistanceAlongTrip(), 0.001);
            } else {
                assertTrue(oldStopTime.getDistanceAlongTrip() < newStopTime.getDistanceAlongTrip());
            }
        }
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
