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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl;

import org.junit.Before;
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
import org.onebusaway.transit_data_federation.bundle.tasks.transit_graph.StopTimeEntriesFactory;
import org.onebusaway.transit_data_federation.impl.transit_graph.AgencyEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopTimeEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphDaoImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public abstract class AbstractGtfsSometimesClientTest {

    @Autowired
    TransitGraphDao _graph;

    @Autowired
    GtfsSometimesHandlerImpl _handler;

    @Autowired
    StopTimeEntriesFactory _stopTimesEntriesFactory;

    @Autowired
    TimeServiceImpl _timeService;

    @Before
    public void loadGtfsData() throws IOException {

        GtfsReader reader = new GtfsReader();
        reader.setDefaultAgencyId("MTA");
        reader.setInputLocation(new File(getPath()));
        GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
        reader.setEntityStore(dao);
        reader.run();

        for (Agency agency : dao.getAllAgencies()) {
            AgencyEntryImpl aei = new AgencyEntryImpl();
            aei.setId(agency.getId());
            assertTrue(_graph.addAgencyEntry(aei));
        }

        AgencyEntryImpl defaultAgency = new AgencyEntryImpl();
        defaultAgency.setId("MTA");
        assertTrue(_graph.addAgencyEntry(defaultAgency));

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
            assertTrue(_graph.addTripEntry(tei, tripNarrative(trip)));
        }
        _handler.forceFlush();

        // set handler time
        _timeService.setTimeZone(ZoneId.of("America/New_York"));
        _timeService.setTime(getTime());
    }

    abstract String getPath();

    abstract String getTime();

    private TripNarrative tripNarrative(Trip trip) {
        return TripNarrative.builder()
                .setRouteShortName(trip.getRouteShortName())
                .setTripHeadsign(trip.getTripHeadsign())
                .setTripShortName(trip.getTripShortName()).create();
    }
}
