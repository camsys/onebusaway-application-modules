package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import com.google.transit.realtime.GtfsRealtime.Stop;

import java.util.List;

public interface StopHandler {
    /**
     * Apply a set of stop changes, returning a set that will revert the changes.
     *
     * @param Stops to add
     * @return number of successfully added stops
     */
    int addStops(List<Stop> changeset);
}
