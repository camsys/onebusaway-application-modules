/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import com.google.transit.realtime.GtfsRealtime.Shape;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShape;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShapes;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsShapeCreationService;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.onebusaway.geospatial.services.PolylineEncoder;
import org.onebusaway.geospatial.model.CoordinatePoint;

import java.util.List;

@Component
public class TripModsShapeCreationServiceImpl implements TripModsShapeCreationService {

    private final Logger _log = LoggerFactory.getLogger(TripModsShapeCreationServiceImpl.class);

    private EntityIdService _entityIdService;

    private TransitGraphDao _transitGraph;

    @Autowired
    public void setEntityIdService(EntityIdService entityIdService) {
        _entityIdService = entityIdService;
    }

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _transitGraph = dao;
    }

    @Override
    public AddedShapes createAddedShapes(List<Shape> shapes){

        AddedShapes addedShapes = new AddedShapes();

        for (Shape shape : shapes) {
            if(!isValidShape(shape)){
                addedShapes.addFailedAddedShapeId(shape.getShapeId());
                continue;
            }

            AgencyAndId shapeId = _entityIdService.getShapeId(shape.getShapeId());
            if(shapeAlreadyExists(shapeId)){
                addedShapes.addFailedAddedShapeId(shape.getShapeId());
                continue;
            }

            ShapePoints shapePoints = createShapePoints(shape, shapeId);
            AddedShape addedShape = new AddedShape(shapePoints, shapeId);
            addedShapes.addShape(addedShape);
        }

        return addedShapes;
    }

    boolean isValidShape(Shape shape) {
        if (shape.getShapeId() == null || shape.getShapeId().isBlank()) {
            _log.warn("Shape with empty id found, skipping addition.");
            return false;
        }

        if (!shape.hasEncodedPolyline()) {
            _log.warn("Stop with id {} missing polyline, skipping addition.", shape.getShapeId());
            return false;
        }

        return true;
    }

    boolean shapeAlreadyExists(AgencyAndId shapeId){
        ShapePoints shapePoints = _transitGraph.getShape(shapeId);
        if (shapePoints != null) {
            _log.info("Shape with id {} already exists, skipping addition.", shapeId.toString());
            return true;
        }
        return false;
    }

    ShapePoints createShapePoints(Shape shape, AgencyAndId shapeId) {
        List<CoordinatePoint> coordinatePointsList = PolylineEncoder.decode(shape.getEncodedPolyline());
        int numPoints = coordinatePointsList.size();
        double[] lat = new double[numPoints];
        double[] lon = new double[numPoints];
        double[] distTraveled = new double[numPoints];

        for (int i = 0; i < coordinatePointsList.size(); i++) {
            CoordinatePoint cp = coordinatePointsList.get(i);
            lat[i] = cp.getLat();
            lon[i] = cp.getLon();
        }

        ShapePoints sp = new ShapePoints();
        sp.setLats(lat);
        sp.setLons(lon);
        sp.setShapeId(shapeId);
        sp.setDistTraveled(distTraveled);
        sp.ensureDistTraveled();

        return sp;
    }
}
