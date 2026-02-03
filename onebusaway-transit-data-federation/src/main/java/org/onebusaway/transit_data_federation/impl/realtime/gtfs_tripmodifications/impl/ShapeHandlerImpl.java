package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.Shape;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.ShapeHandler;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.onebusaway.geospatial.services.PolylineEncoder;
import org.onebusaway.geospatial.model.CoordinatePoint;

import java.util.ArrayList;
import java.util.List;

@Component
public class ShapeHandlerImpl implements ShapeHandler {
    private Logger _log = LoggerFactory.getLogger(ShapeHandlerImpl.class);

    private EntityIdService _entityIdService;

    private TransitGraphDao _dao;

    @Autowired
    public void setEntityIdService(EntityIdService entityIdService) {
        _entityIdService = entityIdService;
    }

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Override
    public List<ShapePoints> addShapes(List<Shape> shapes) {

        List<ShapePoints> successfullyAddedShapes = new ArrayList<>();

        for (Shape shape : shapes) {
            if (shape.getShapeId() == null || shape.getShapeId().isEmpty()) {
                _log.warn("Shape with empty id found, skipping addition.");
                continue;
            }

            if (!shape.hasEncodedPolyline()) {
                _log.warn("Stop with id {} missing polyline, skipping addition.", shape.getShapeId());
                continue;
            }

            if (_entityIdService.getShapeId(shape.getShapeId()) != null) {
                _log.info("Shape with id {} already exists, skipping addition.", shape.getShapeId());
                continue;
            }

            List<CoordinatePoint> coordinatePointsList = PolylineEncoder.decode(shape.getEncodedPolyline());
            double[] lat = new double[coordinatePointsList.size()];
            double[] lon = new double[coordinatePointsList.size()];

            for (int i = 0; i < coordinatePointsList.size(); i++) {
                CoordinatePoint cp = coordinatePointsList.get(i);
                lat[i] = cp.getLat();
                lon[i] = cp.getLon();
            }


            //TODO find agency ID
            AgencyAndId shapeAgencyAndId = new AgencyAndId("MTA", shape.getShapeId());
            ShapePoints sp = new ShapePoints();
            sp.setLats(lat);
            sp.setLons(lon);
            sp.ensureDistTraveled();
            sp.setShapeId(shapeAgencyAndId);
            _dao.addShape(sp);

            successfullyAddedShapes.add(sp);
        }

        return successfullyAddedShapes;
    }
}
