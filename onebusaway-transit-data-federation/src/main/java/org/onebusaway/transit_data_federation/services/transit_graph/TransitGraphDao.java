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
package org.onebusaway.transit_data_federation.services.transit_graph;

import java.util.List;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.transit_data_federation.impl.transit_graph.AgencyEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;

/**
 * Service interface that abstract operations on a transit graph, such as access
 * to stops and trips, away from the underlying graph implementation.
 * 
 * @author bdferris
 * @see StopEntry
 * @see TripEntry
 */
public interface TransitGraphDao {

  /**
   * @return the list of all agency entries in the transit graph
   */
  public List<AgencyEntry> getAllAgencies();

  /**
   * @param id a agency id to query
   * @return the agency entry with the specified id, or null if not found
   */
  public AgencyEntry getAgencyForId(String id);

  public boolean addAgencyEntry(AgencyEntryImpl aei);

  /**
   * @return the list of all stop entries in the transit graph
   */
  public List<StopEntry> getAllStops();

  /**
   * @param id a stop id to query
   * @return the stop entry with the specified id, or null if not found
   */
  public StopEntry getStopEntryForId(AgencyAndId id);

  public StopEntry getStopEntryForId(AgencyAndId id,
      boolean throwExceptionIfNotFound);

  /**
   * @param bounds coordinate bounds query
   * @return a list of stop entries located within in the specified bounds
   */
  public List<StopEntry> getStopsByLocation(CoordinateBounds bounds);


  public boolean addStopEntry(StopEntryImpl stop);

  public boolean removeStopEntry(AgencyAndId stopId);
  /**
   * @return the list of all block entries in the transit graph
   */
  public List<BlockEntry> getAllBlocks();

  /**
   * @param blockId a block id to query
   * @return the block entry with the specified id, or null if not found
   */
  public BlockEntry getBlockEntryForId(AgencyAndId blockId);

  /**
   * @return the list of all trip entries in the transit graph
   */
  public List<TripEntry> getAllTrips();

  /**
   * @param id a trip id to query
   * @return the trip entry with the specified id, or null if not found
   */
  public TripEntry getTripEntryForId(AgencyAndId id);

  /**
   * @return the list of all route collections in the transit graph
   */
  public List<RouteCollectionEntry> getAllRouteCollections();

  /**
   * @param id a route collection id to query
   * @return the route collection entry with the specified id, or null if not
   *         found
   */
  public RouteCollectionEntry getRouteCollectionForId(AgencyAndId id);

  /**
   * @return the list of all routes in the transit graph
   */
  public List<RouteEntry> getAllRoutes();

  /**
   * @param id a route id to query
   * @return the route entry with the specified id, or null if not found
   */

  public RouteEntry getRouteForId(AgencyAndId id);

  /**
   * delete the trip represented by the given id
   * @param tripId tripId to delete
   * @return true if deletion was successful
   */
  public boolean deleteTripEntryForId(AgencyAndId tripId);

  /**
   * delete stop time for the given trip and stop pair
   * @param tripId trip to delete from
   * @param stopId stop to delete
   * @return true if deltion was successful
   */
  public boolean deleteStopTime(AgencyAndId tripId, AgencyAndId stopId);

  public boolean addTripEntry(TripEntryImpl trip, TripNarrative narrative);

  public boolean addTripEntry(TripEntryImpl trip);

  public boolean updateTripEntry(TripEntryImpl trip);

  public boolean removeTripEntry(TripEntryImpl trip);

    /**
   * update arrival/departure time for a stop should it exist on the given trip
   * @param tripId
   * @param stopId
   * @param originalArrivalTime -1 for any match, or old value if multiple stops exist on trip for that id
   * @param originalDepartureTime -1 for any match, or old value if multiple stops exist on trip for that id
   * @param newArrivalTime
   * @param newDepartureTime
   * @return
   */
  public boolean updateStopTime(AgencyAndId tripId, AgencyAndId stopId, int originalArrivalTime, int originalDepartureTime,
                                int newArrivalTime, int newDepartureTime);

  /**
   * insert a stop time into the list of stop times in the appropriate position based on the shapeDistanceTravelled
   * or failing that based on arrival/departure time.
   * @param tripId
   * @param stopId
   * @param arrivalTime
   * @param departureTime
   * @param shapeDistanceTravelled
   * @return
   */
  public boolean insertStopTime(AgencyAndId tripId, AgencyAndId stopId, int arrivalTime, int departureTime, double shapeDistanceTravelled);


  public void updateCalendarServiceData(CalendarServiceData data);

  public boolean addShape(ShapePoints shape);

  public ShapePoints getShape(AgencyAndId shapeId);

  public List<AgencyAndId> getAllReferencedShapeIds();

  public boolean updateShapeForTrip(TripEntryImpl trip, AgencyAndId shapeId);
}
