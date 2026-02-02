package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import com.google.transit.realtime.GtfsRealtime.Shape;

import java.util.List;

public interface ShapeHandler {
    /**
     * Add a set of shapes, returning the number of successfully added shapes.
     *
     * @param shapes to add
     * @return number of successfully added stops
     */
    int addShapes(List<Shape> shapes);
}
