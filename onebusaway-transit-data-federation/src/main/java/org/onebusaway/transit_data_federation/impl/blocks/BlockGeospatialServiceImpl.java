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
package org.onebusaway.transit_data_federation.impl.blocks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.collections.FactoryMap;
import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.collections.Min;
import org.onebusaway.collections.tuple.T2;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.model.XYPoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.shapes.PointAndIndex;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockLayoverIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockSequenceIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.shapes.ProjectedShapePointService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class BlockGeospatialServiceImpl implements BlockGeospatialService {

  private static Logger _log = LoggerFactory.getLogger(BlockGeospatialServiceImpl.class);

  private static double DEFAULT_GRID_SIZE = 500.0;

  private FederatedTransitDataBundle _bundle;

  private TransitGraphDao _transitGraphDao;

  private BlockCalendarService _blockCalendarService;

  private BlockIndexService _blockIndexService;

  private ShapePointService _shapePointService;

  private Map<AgencyAndId, List<BlockSequenceIndex>> _blockSequenceIndicesByShapeId = new HashMap<AgencyAndId, List<BlockSequenceIndex>>();

  private STRtree _tree = new STRtree();

  private ProjectedShapePointService _projectedShapePointService;

  private ShapePointsLibrary _shapePointsLibrary;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private double _gridSize = DEFAULT_GRID_SIZE;

  @Autowired
  public void setBundle(FederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  @Autowired
  public void setBlockIndexService(BlockIndexService blockIndexService) {
    _blockIndexService = blockIndexService;
  }

  @Autowired
  public void setShapePointService(ShapePointService shapePointService) {_shapePointService = shapePointService; }

  @Autowired
  public void setProjected(ProjectedShapePointService projectedShapePointService) {
    _projectedShapePointService = projectedShapePointService;
  }

  @Autowired
  public void setShapePointsLibrary(ShapePointsLibrary shapePointsLibrary) {
    _shapePointsLibrary = shapePointsLibrary;
  }

  @Autowired
  public void setScheduledBlockLocationService(
      ScheduledBlockLocationService scheduledBlockLocationService) {
    _scheduledBlockLocationService = scheduledBlockLocationService;
  }

  @PostConstruct
  @Refreshable(dependsOn = {
      RefreshableResources.SHAPE_GEOSPATIAL_INDEX,
      RefreshableResources.BLOCK_INDEX_SERVICE})
  public void setup() throws IOException, ClassNotFoundException {
    _blockSequenceIndicesByShapeId.clear();
    groupBlockSequenceIndicesByShapeIds();

    buildShapeSpatialIndexFromBundle();
  }

  @Refreshable(dependsOn = RefreshableResources.BLOCK_INDEX_DATA_GRAPH)
  public boolean reloadFromGraph() {
    Map<CoordinateBounds, List<AgencyAndId>> shapeIdsByGridCell = buildShapeSpatialIndexFromGraph();

    try {
      _blockSequenceIndicesByShapeId.clear();
      groupBlockSequenceIndicesByShapeIds();

      buildShapeSpatialIndex(shapeIdsByGridCell);
    } catch (Exception e) {
      _log.error("rebuild of index failed", e);
      return false;
    }
    return true;
  }

  @Override
  public List<BlockInstance> getActiveScheduledBlocksPassingThroughBounds(
      CoordinateBounds bounds, long timeFrom, long timeTo) {

    List<StopEntry> stops = _transitGraphDao.getStopsByLocation(bounds);

    Set<AgencyAndId> blockIds = new HashSet<AgencyAndId>();

    for (StopEntry stop : stops) {
      List<BlockStopTimeIndex> stopTimeIndices = _blockIndexService.getStopTimeIndicesForStop(stop);
      
      Set<BlockConfigurationEntry> blockConfigs = new HashSet<BlockConfigurationEntry>();
      
      List<List<BlockConfigurationEntry>> blockConfigsList = MappingLibrary.map(stopTimeIndices, "blockConfigs");
      
      for (List<BlockConfigurationEntry> l: blockConfigsList) {
        blockConfigs.addAll(l);
      }
     
      List<AgencyAndId> stopBlockIds = MappingLibrary.map(blockConfigs, "block.id");
      blockIds.addAll(stopBlockIds);      
    }

    Set<BlockTripIndex> blockIndices = new HashSet<BlockTripIndex>();

    for (AgencyAndId blockId: blockIds) {
      blockIndices.addAll(_blockIndexService.getBlockTripIndicesForBlock(blockId));
    }
    
    List<BlockLayoverIndex> layoverIndices = Collections.emptyList();
    List<FrequencyBlockTripIndex> frequencyIndices = Collections.emptyList();

    return _blockCalendarService.getActiveBlocksInTimeRange(blockIndices,
        layoverIndices, frequencyIndices, timeFrom, timeTo);
  }

  @Override
  public Set<BlockSequenceIndex> getBlockSequenceIndexPassingThroughBounds(
      CoordinateBounds bounds) {

    Envelope env = new Envelope(bounds.getMinLon(), bounds.getMaxLon(),
        bounds.getMinLat(), bounds.getMaxLat());


    Set<AgencyAndId> visitedShapeIds = new HashSet<AgencyAndId>();
    Set<BlockSequenceIndex> allIndices = new HashSet<BlockSequenceIndex>();

    if (_tree != null) {
      @SuppressWarnings("unchecked")
      List<List<AgencyAndId>> results = _tree.query(env);
      for (List<AgencyAndId> shapeIds : results) {
        for (AgencyAndId shapeId : shapeIds) {
          if (visitedShapeIds.add(shapeId)) {
            List<BlockSequenceIndex> indices = _blockSequenceIndicesByShapeId.get(shapeId);
            if (!CollectionsLibrary.isEmpty(indices)) {
              allIndices.addAll(indices);
            }
          }
        }
      }
    }
    return allIndices;
  }

  public ScheduledBlockLocation getBestScheduledBlockLocationForLocation(
      BlockInstance blockInstance, CoordinatePoint location, long timestamp,
      double blockDistanceFrom, double blockDistanceTo) {

    BlockConfigurationEntry block = blockInstance.getBlock();

    ProjectedPoint targetPoint = ProjectedPointFactory.forward(location);

    List<AgencyAndId> shapePointIds = MappingLibrary.map(block.getTrips(),
        "trip.shapeId");

    T2<List<XYPoint>, double[]> tuple = _projectedShapePointService.getProjectedShapePoints(
        shapePointIds, targetPoint.getSrid());

    if (tuple == null) {
      throw new IllegalStateException("block had no shape points: "
          + block.getBlock().getId());
    }

    List<XYPoint> projectedShapePoints = tuple.getFirst();
    double[] distances = tuple.getSecond();

    int fromIndex = 0;
    int toIndex = distances.length;

    if (blockDistanceFrom > 0) {
      fromIndex = Arrays.binarySearch(distances, blockDistanceFrom);
      if (fromIndex < 0) {
        fromIndex = -(fromIndex + 1);
        // Include the previous point if we didn't get an exact match
        if (fromIndex > 0)
          fromIndex--;
      }
    }

    if (blockDistanceTo < distances[distances.length - 1]) {
      toIndex = Arrays.binarySearch(distances, blockDistanceTo);
      if (toIndex < 0) {
        toIndex = -(toIndex + 1);
        // Include the previous point if we didn't get an exact match
        if (toIndex < distances.length)
          toIndex++;
      }
    }

    XYPoint xyPoint = new XYPoint(targetPoint.getX(), targetPoint.getY());

    List<PointAndIndex> assignments = _shapePointsLibrary.computePotentialAssignments(
        projectedShapePoints, distances, xyPoint, fromIndex, toIndex);

    Min<ScheduledBlockLocation> best = new Min<ScheduledBlockLocation>();

    for (PointAndIndex index : assignments) {

      double distanceAlongBlock = index.distanceAlongShape;

      if (distanceAlongBlock > block.getTotalBlockDistance())
        distanceAlongBlock = block.getTotalBlockDistance();

      ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
          block, distanceAlongBlock);

      if (blockLocation != null) {
        int scheduledTime = blockLocation.getScheduledTime();
        long scheduleTimestamp = blockInstance.getServiceDate() + scheduledTime
            * 1000;

        double delta = Math.abs(scheduleTimestamp - timestamp);
        best.add(delta, blockLocation);
      }
    }

    return best.getMinElement();
  }

  /****
   * Private Methods
   ****/

  private void groupBlockSequenceIndicesByShapeIds() {
    List<BlockSequenceIndex> indices = _blockIndexService.getAllBlockSequenceIndices();

    for (BlockSequenceIndex index : indices) {

      Set<AgencyAndId> shapeIdsForIndex = new HashSet<AgencyAndId>();

      for (BlockSequence sequence : index.getSequences()) {
        for (BlockStopTimeEntry bst : sequence.getStopTimes()) {
          BlockTripEntry blockTrip = bst.getTrip();
          TripEntry trip = blockTrip.getTrip();
          AgencyAndId shapeId = trip.getShapeId();
          if (shapeId != null)
            shapeIdsForIndex.add(shapeId);
        }
      }

      for (AgencyAndId shapeId : shapeIdsForIndex) {
        List<BlockSequenceIndex> list = _blockSequenceIndicesByShapeId.get(shapeId);
        if (list == null) {
          list = new ArrayList<BlockSequenceIndex>();
          _blockSequenceIndicesByShapeId.put(shapeId, list);
        }
        list.add(index);
      }
    }
  }

  private void buildShapeSpatialIndexFromBundle() throws IOException,
      ClassNotFoundException {

    File path = _bundle.getShapeGeospatialIndexDataPath();

    if (!path.exists()) {
      _tree = null;
      return;
    }

    _log.info("loading shape point geospatial index...");

    Map<CoordinateBounds, List<AgencyAndId>> shapeIdsByGridCell = ObjectSerializationLibrary.readObject(path);

    buildShapeSpatialIndex(shapeIdsByGridCell);



  }

  private void buildShapeSpatialIndex(Map<CoordinateBounds, List<AgencyAndId>> shapeIdsByGridCell) throws IOException,
          ClassNotFoundException {
    _log.info("block shape geospatial nodes: " + shapeIdsByGridCell.size());

    if (shapeIdsByGridCell.isEmpty()) {
      _tree = null;
      return;
    }

    _tree = new STRtree(shapeIdsByGridCell.size());

    for (Map.Entry<CoordinateBounds, List<AgencyAndId>> entry : shapeIdsByGridCell.entrySet()) {
      CoordinateBounds b = entry.getKey();
      Envelope env = new Envelope(b.getMinLon(), b.getMaxLon(), b.getMinLat(),
              b.getMaxLat());
      List<AgencyAndId> shapeIds = entry.getValue();
      _tree.insert(env, shapeIds);
    }

    _tree.build();
    _log.info("STRtree has size " + _tree.size());
  }

  // this implicitly selects referenced shapes only
  private Set<AgencyAndId> getAllShapeIds() {

    Set<AgencyAndId> shapeIds = new HashSet<AgencyAndId>();

    for (TripEntry trip : _transitGraphDao.getAllTrips()) {
      AgencyAndId shapeId = trip.getShapeId();
      if (shapeId != null)
        shapeIds.add(shapeId);
    }

    return shapeIds;
  }

  /*
   * todo this code copied from ShapeGeospatialIndexTask:  find a way to refactor into common helper
   */
  private Map<CoordinateBounds, List<AgencyAndId>> buildShapeSpatialIndexFromGraph() {

    Map<CoordinatePoint, Set<AgencyAndId>> shapeIdsByGridCellCorner = new FactoryMap<CoordinatePoint, Set<AgencyAndId>>(
            new HashSet<AgencyAndId>());

    CoordinateBounds fullBounds = new CoordinateBounds();
    for (StopEntry stop : _transitGraphDao.getAllStops())
      fullBounds.addPoint(stop.getStopLat(), stop.getStopLon());

    if (fullBounds.isEmpty()) {
      return Collections.emptyMap();
    }

    double centerLat = (fullBounds.getMinLat() + fullBounds.getMaxLat()) / 2;
    double centerLon = (fullBounds.getMinLon() + fullBounds.getMaxLon()) / 2;
    CoordinateBounds gridCellExample = SphericalGeometryLibrary.bounds(
            centerLat, centerLon, _gridSize / 2);

    double latStep = gridCellExample.getMaxLat() - gridCellExample.getMinLat();
    double lonStep = gridCellExample.getMaxLon() - gridCellExample.getMinLon();

    Set<AgencyAndId> allShapeIds = getAllShapeIds();

    _log.info("generating shape point geospatial index from " + _transitGraphDao.getAllStops().size() + " stops and "
    + allShapeIds.size() + " shapes");

    for (AgencyAndId shapeId : allShapeIds) {

      ShapePoints shapePoints = getShapePointsForShapeIdNonCached(shapeId);

      if (shapePoints != null) {
        for (int i = 0; i < shapePoints.getSize(); i++) {

          double lat = shapePoints.getLatForIndex(i);
          double lon = shapePoints.getLonForIndex(i);

          addGridCellForShapePoint(shapeIdsByGridCellCorner, lat, lon, latStep,
                  lonStep, shapeId);

          /**
           * If there is a particularly long stretch between shape points, we want
           * to fill in grid cells in-between
           */
          if (i > 0) {
            double prevLat = shapePoints.getLatForIndex(i - 1);
            double prevLon = shapePoints.getLonForIndex(i - 1);
            double totalDistance = SphericalGeometryLibrary.distance(prevLat,
                    prevLon, lat, lon);
            for (double d = _gridSize; d < totalDistance; d += _gridSize) {
              double r = d / totalDistance;
              double latPart = (lat - prevLat) * r + prevLat;
              double lonPart = (lon - prevLon) * r + prevLon;
              addGridCellForShapePoint(shapeIdsByGridCellCorner, latPart,
                      lonPart, latStep, lonStep, shapeId);
            }
          }
        }
      }
    }

    _log.info("block shape geospatial nodes: "
            + shapeIdsByGridCellCorner.size());

    Map<CoordinateBounds, List<AgencyAndId>> shapeIdsByGridCell = new HashMap<CoordinateBounds, List<AgencyAndId>>();

    for (Map.Entry<CoordinatePoint, Set<AgencyAndId>> entry : shapeIdsByGridCellCorner.entrySet()) {
      CoordinatePoint p = entry.getKey();
      CoordinateBounds bounds = new CoordinateBounds(p.getLat(), p.getLon(),
              p.getLat() + latStep, p.getLon() + lonStep);

      List<AgencyAndId> shapeIds = new ArrayList<AgencyAndId>(entry.getValue());
      shapeIdsByGridCell.put(bounds, shapeIds);
    }

    return shapeIdsByGridCell;
  }


  private ShapePoints getShapePointsForShapeIdNonCached(AgencyAndId shapeId) {
    return _shapePointService.getShapePointsForShapeId(shapeId);

  }


  private void addGridCellForShapePoint(
          Map<CoordinatePoint, Set<AgencyAndId>> shapeIdsByGridCellCorner,
          double lat, double lon, double latStep, double lonStep,
          AgencyAndId shapeId) {

    CoordinatePoint gridCellCorner = getGridCellCornerForPoint(lat, lon,
            latStep, lonStep);
    shapeIdsByGridCellCorner.get(gridCellCorner).add(shapeId);
  }

  private CoordinatePoint getGridCellCornerForPoint(double lat, double lon,
                                                    double latStep, double lonStep) {

    double latCorner = Math.floor(lat / latStep) * latStep;
    double lonCorner = Math.floor(lon / lonStep) * lonStep;
    return new CoordinatePoint(latCorner, lonCorner);
  }

}
