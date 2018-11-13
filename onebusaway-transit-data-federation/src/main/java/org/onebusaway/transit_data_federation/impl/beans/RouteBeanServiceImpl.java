/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.transit_data_federation.impl.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.container.cache.Cacheable;
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.NameBeanTypes;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.TransitDataConstants;
import org.onebusaway.transit_data_federation.impl.DirectedGraph;
import org.onebusaway.transit_data_federation.impl.StopGraphComparator;
import org.onebusaway.transit_data_federation.model.StopSequence;
import org.onebusaway.transit_data_federation.model.StopSequenceCollection;
import org.onebusaway.transit_data_federation.model.narrative.RouteCollectionNarrative;
import org.onebusaway.transit_data_federation.services.RouteService;
import org.onebusaway.transit_data_federation.services.StopSequenceCollectionService;
import org.onebusaway.transit_data_federation.services.StopSequencesService;
import org.onebusaway.transit_data_federation.services.beans.AgencyBeanService;
import org.onebusaway.transit_data_federation.services.beans.RouteBeanService;
import org.onebusaway.transit_data_federation.services.beans.ShapeBeanService;
import org.onebusaway.transit_data_federation.services.beans.StopBeanService;
import org.onebusaway.transit_data_federation.services.blocks.AbstractBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class RouteBeanServiceImpl implements RouteBeanService {

  private TransitGraphDao _transitGraphDao;

  private NarrativeService _narrativeService;

  private AgencyBeanService _agencyBeanService;

  private StopBeanService _stopBeanService;

  private ShapeBeanService _shapeBeanService;

  private RouteService _routeService;

  private StopSequencesService _stopSequencesService;

  private StopSequenceCollectionService _stopSequenceBlocksService;

  private BlockIndexService _blockIndexService;

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setNarrativeService(NarrativeService narrativeService) {
    _narrativeService = narrativeService;
  }

  @Autowired
  public void setAgencyBeanService(AgencyBeanService agencyBeanService) {
    _agencyBeanService = agencyBeanService;
  }

  @Autowired
  public void setStopBeanService(StopBeanService stopBeanService) {
    _stopBeanService = stopBeanService;
  }

  @Autowired
  public void setShapeBeanService(ShapeBeanService shapeBeanService) {
    _shapeBeanService = shapeBeanService;
  }

  @Autowired
  public void setRouteService(RouteService routeService) {
    _routeService = routeService;
  }

  @Autowired
  public void setStopSequencesLibrary(StopSequencesService service) {
    _stopSequencesService = service;
  }

  @Autowired
  public void setStopSequencesBlocksService(
      StopSequenceCollectionService stopSequenceBlocksService) {
    _stopSequenceBlocksService = stopSequenceBlocksService;
  }

  @Autowired
  public void setBlockIndexService(BlockIndexService blockIndexService) {
    _blockIndexService = blockIndexService;
  }

  @Cacheable
  public RouteBean getRouteForId(AgencyAndId id) {
    RouteCollectionNarrative rc = _narrativeService.getRouteCollectionForId(id);
    if (rc == null)
      return null;
    return getRouteBeanForRouteCollection(id, rc);
  }

  @Cacheable
  public StopsForRouteBean getStopsForRoute(AgencyAndId routeId) {
    RouteCollectionEntry routeCollectionEntry = _transitGraphDao.getRouteCollectionForId(routeId);
    RouteCollectionNarrative narrative = _narrativeService.getRouteCollectionForId(routeId);
    if (routeCollectionEntry == null || narrative == null)
      return null;
    return getStopsForRouteCollectionAndNarrative(routeCollectionEntry,
        narrative);
  }

  /****
   * Private Methods
   ****/

  private RouteBean getRouteBeanForRouteCollection(AgencyAndId id,
      RouteCollectionNarrative rc) {

    RouteBean.Builder bean = RouteBean.builder();
    bean.setId(ApplicationBeanLibrary.getId(id));
    bean.setShortName(rc.getShortName());
    bean.setLongName(rc.getLongName());
    bean.setColor(rc.getColor());
    bean.setDescription(rc.getDescription());
    bean.setTextColor(rc.getTextColor());
    bean.setType(rc.getType());
    bean.setUrl(rc.getUrl());

    AgencyBean agency = _agencyBeanService.getAgencyForId(id.getAgencyId());
    bean.setAgency(agency);

    return bean.create();
  }

  private List<StopBean> getStopBeansForRoute(AgencyAndId routeId) {

    Collection<AgencyAndId> stopIds = _routeService.getStopsForRouteCollection(routeId);
    List<StopBean> stops = new ArrayList<StopBean>();

    for (AgencyAndId stopId : stopIds) {
      StopBean stop = _stopBeanService.getStopForId(stopId);
      stops.add(stop);
    }

    return stops;
  }

  private StopsForRouteBean getStopsForRouteCollectionAndNarrative(
      RouteCollectionEntry routeCollection, RouteCollectionNarrative narrative) {

    StopsForRouteBean result = new StopsForRouteBean();

    AgencyAndId routeCollectionId = routeCollection.getId();
    result.setRoute(getRouteBeanForRouteCollection(routeCollectionId, narrative));
    result.setStops(getStopBeansForRoute(routeCollectionId));

    result.setPolylines(getEncodedPolylinesForRoute(routeCollection));

    StopGroupingBean directionGrouping = new StopGroupingBean();
    directionGrouping.setType(TransitDataConstants.STOP_GROUPING_TYPE_DIRECTION);
    List<StopGroupBean> directionGroups = new ArrayList<StopGroupBean>();
    directionGrouping.setStopGroups(directionGroups);
    directionGrouping.setOrdered(true);
    result.addGrouping(directionGrouping);

    List<BlockTripIndex> blockIndices = _blockIndexService.getBlockTripIndicesForRouteCollectionId(routeCollectionId);
    List<FrequencyBlockTripIndex> frequencyBlockIndices = _blockIndexService.getFrequencyBlockTripIndicesForRouteCollectionId(routeCollectionId);

    List<BlockTripEntry> blockTrips = new ArrayList<BlockTripEntry>();

    getBlockTripsForIndicesMatchingRouteCollection(blockIndices,
        routeCollectionId, blockTrips);
    getBlockTripsForIndicesMatchingRouteCollection(frequencyBlockIndices,
        routeCollectionId, blockTrips);

    List<StopSequence> sequences = _stopSequencesService.getStopSequencesForTrips(blockTrips);

    List<StopSequenceCollection> blocks = _stopSequenceBlocksService.getStopSequencesAsCollections(sequences);

    for (StopSequenceCollection block : blocks) {

      NameBean name = new NameBean(NameBeanTypes.DESTINATION,
          block.getDescription());

      List<StopEntry> stops = getStopsInOrder(block);
      List<String> groupStopIds = new ArrayList<String>();
      for (StopEntry stop : stops)
        groupStopIds.add(ApplicationBeanLibrary.getId(stop.getId()));

      Set<AgencyAndId> shapeIds = getShapeIdsForStopSequenceBlock(block);
      List<EncodedPolylineBean> polylines = _shapeBeanService.getMergedPolylinesForShapeIds(shapeIds);

      StopGroupBean group = new StopGroupBean();
      group.setId(block.getPublicId());
      group.setName(name);
      group.setStopIds(groupStopIds);
      group.setPolylines(polylines);
      directionGroups.add(group);
    }

    sortResult(result);

    return result;
  }

  /**
   * A block index potentially includes trips with different routes. We only
   * want the trips matching our route collection.
   * 
   * @param <T>
   * @param blockIndices
   * @param routeCollectionId
   * @param resultingTrips
   */
  private <T extends AbstractBlockTripIndex> void getBlockTripsForIndicesMatchingRouteCollection(
      List<T> blockIndices, AgencyAndId routeCollectionId,
      List<BlockTripEntry> resultingTrips) {
    for (AbstractBlockTripIndex blockIndex : blockIndices) {
      for (BlockTripEntry blockTrip : blockIndex.getTrips()) {
        TripEntry trip = blockTrip.getTrip();
        AgencyAndId rcId = trip.getRouteCollection().getId();
        if (!rcId.equals(routeCollectionId))
          continue;
        resultingTrips.add(blockTrip);
      }
    }
  }

  private List<EncodedPolylineBean> getEncodedPolylinesForRoute(
      RouteCollectionEntry routeCollection) {

    Set<AgencyAndId> shapeIds = new HashSet<AgencyAndId>();
    for (RouteEntry route : routeCollection.getChildren()) {
      for (TripEntry trip : route.getTrips()) {
        if (trip.getShapeId() != null)
          shapeIds.add(trip.getShapeId());
      }
    }

    return _shapeBeanService.getMergedPolylinesForShapeIds(shapeIds);
  }

  private List<StopEntry> getStopsInOrder(StopSequenceCollection block) {
    DirectedGraph<StopEntry> graph = new DirectedGraph<StopEntry>();
    for (StopSequence sequence : block.getStopSequences()) {
      StopEntry prev = null;
      for (StopEntry stop : sequence.getStops()) {
        if (prev != null) {
          // Normalize stop in case there are service changes
          stop = _transitGraphDao.getStopEntryForId(stop.getId());

          // We do this to avoid cycles
          if (!graph.isConnected(stop, prev))
            graph.addEdge(prev, stop);
        }
        prev = stop;
      }
    }

    StopGraphComparator c = new StopGraphComparator(graph);
    return graph.getTopologicalSort(c);
  }

  private Set<AgencyAndId> getShapeIdsForStopSequenceBlock(
      StopSequenceCollection block) {
    Set<AgencyAndId> shapeIds = new HashSet<AgencyAndId>();
    for (StopSequence sequence : block.getStopSequences()) {
      for (BlockTripEntry blockTrip : sequence.getTrips()) {
        TripEntry trip = blockTrip.getTrip();
        AgencyAndId shapeId = trip.getShapeId();
        if (shapeId != null && shapeId.hasValues())
          shapeIds.add(shapeId);
      }
    }
    return shapeIds;
  }

  private void sortResult(StopsForRouteBean result) {

    Collections.sort(result.getStops(), new StopBeanIdComparator());

    Collections.sort(result.getStopGroupings(),
        new Comparator<StopGroupingBean>() {
          public int compare(StopGroupingBean o1, StopGroupingBean o2) {
            return o1.getType().compareTo(o2.getType());
          }
        });

    for (StopGroupingBean grouping : result.getStopGroupings()) {
      Collections.sort(grouping.getStopGroups(),
          new Comparator<StopGroupBean>() {

            public int compare(StopGroupBean o1, StopGroupBean o2) {
              return getName(o1).compareTo(getName(o2));
            }

            private String getName(StopGroupBean bean) {
              StringBuilder b = new StringBuilder();
              for (String name : bean.getName().getNames())
                b.append(name);
              return b.toString();
            }
          });
    }
  }

}
