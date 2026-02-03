package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import com.camsys.transit.servicechange.ServiceChange;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripModifications;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChangeSet;

import java.util.Collection;

public interface TripModsTripChangeHandler {

    /**
     * Get all trip changes from a TripModifications message
     *
     * @param tripModifications to process
     * @return all trip changes
     */
    TripChangeSet getAllTripChanges(TripModifications tripModifications);

    /**
     * Apply trip changes to the graph
     *
     * @param tripChangeSet to apply
     * @return the applied tripChangeSet
     */
    TripChangeSet applyChanges(TripChangeSet tripChangeSet);
}
