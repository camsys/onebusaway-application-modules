package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import com.google.transit.realtime.GtfsRealtime.Stop;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;

import java.util.List;

public interface StopHandler {
    /**
     * Add new stops, returning the list of successfully added stops
     *
     * @param stops to add
     * @return list of successfully added stops
     */
    List<StopEntry> addStops(List<Stop> stops);
}
