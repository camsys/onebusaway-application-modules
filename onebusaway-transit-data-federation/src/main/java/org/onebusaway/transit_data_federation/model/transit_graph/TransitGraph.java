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
package org.onebusaway.transit_data_federation.model.transit_graph;

import java.util.List;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.transit_graph.AgencyEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.services.transit_graph.AgencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

public interface TransitGraph {
  
  public List<AgencyEntry> getAllAgencies();

  public AgencyEntry getAgencyForId(String id);

  boolean addAgencyEntry(AgencyEntryImpl agency);

  public List<StopEntry> getAllStops();

  boolean addStopEntry(StopEntryImpl stop);

  boolean removeStopEntry(AgencyAndId stopId);

  public List<TripEntry> getAllTrips();

  public List<BlockEntry> getAllBlocks();

  public List<StopEntry> getStopsByLocation(CoordinateBounds bounds);

  boolean addBlock(BlockEntryImpl block);

  boolean addTripEntry(TripEntryImpl trip);

  boolean removeTripEntryForId(AgencyAndId id);

  boolean updateBlockIndices(TripEntryImpl tripEntry);

  public BlockEntry getBlockEntryForId(AgencyAndId blockId);

  public TripEntry getTripEntryForId(AgencyAndId id);

  public StopEntry getStopEntryForId(AgencyAndId id);
  
  public List<RouteCollectionEntry> getAllRouteCollections();

  public RouteCollectionEntry getRouteCollectionForId(AgencyAndId id);
  
  public List<RouteEntry> getAllRoutes();

  public RouteEntry getRouteForId(AgencyAndId id);
}