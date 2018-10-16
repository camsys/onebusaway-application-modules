/**
 * Copyright (C) 2011 Google, Inc.
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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteCollectionEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.RouteEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

public class EntityIdServiceImplTest {

  private EntityIdServiceImpl _service;

  private TransitGraphDao _dao;

  @Before
  public void before() {
    _service = new EntityIdServiceImpl();
    _service.setAgencyIds(Arrays.asList("1", "2"));

    _dao = Mockito.mock(TransitGraphDao.class);
    _service.setTransitGraphDao(_dao);
  }

  @Test
  public void testGetRouteId() {

    RouteCollectionEntryImpl routeCollection = new RouteCollectionEntryImpl();
    routeCollection.setId(new AgencyAndId("2", "R10C"));

    RouteEntryImpl route = new RouteEntryImpl();
    route.setId(new AgencyAndId("2", "R10"));
    route.setParent(routeCollection);

    Mockito.when(_dao.getRouteForId(route.getId())).thenReturn(route);
    AgencyAndId routeId = _service.getRouteId("R10");
    assertEquals("2", routeId.getAgencyId());
    assertEquals("R10C", routeId.getId());

    routeId = _service.getRouteId("R11");
    assertEquals("1", routeId.getAgencyId());
    assertEquals("R11", routeId.getId());
  }

  @Test
  public void testGetTripId() {

    TripEntryImpl trip = new TripEntryImpl();
    trip.setId(new AgencyAndId("2", "T10"));
    Mockito.when(_dao.getTripEntryForId(trip.getId())).thenReturn(trip);

    AgencyAndId tripId = _service.getTripId("T10");
    assertEquals("2", tripId.getAgencyId());
    assertEquals("T10", tripId.getId());

    tripId = _service.getTripId("T11");
    assertEquals("1", tripId.getAgencyId());
    assertEquals("T11", tripId.getId());
  }


  @Test
  public void testGetStopId() {

    StopEntryImpl stop = new StopEntryImpl(new AgencyAndId("2", "S10"), 0, 0);
    Mockito.when(_dao.getStopEntryForId(stop.getId())).thenReturn(stop);

    AgencyAndId stopId = _service.getStopId("S10");
    assertEquals("2", stopId.getAgencyId());
    assertEquals("S10", stopId.getId());

    stopId = _service.getStopId("S11");
    assertEquals("1", stopId.getAgencyId());
    assertEquals("S11", stopId.getId());
  }

}
