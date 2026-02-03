package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import com.google.transit.realtime.GtfsRealtime.TripModifications;

import java.util.Collection;

public interface GtfsTripModificationsHandler {

    /**
     * Process TripModifications; make the appropriate changes in the graph.
     *
     * @param tripModifications to process
     * @return number of tripmodification fully successfully handled
     */
    int handleTripModifications(long timestamp, Collection<TripModifications> tripModifications);
}
