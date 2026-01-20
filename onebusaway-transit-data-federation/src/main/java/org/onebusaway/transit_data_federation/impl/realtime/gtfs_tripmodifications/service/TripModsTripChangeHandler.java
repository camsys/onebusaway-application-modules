package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import com.google.transit.realtime.GtfsRealtime.TripModifications;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChangeSet;

public interface TripModsTripChangeHandler {

    /**
     * Read in GTFS-TripModifications model classes and return a TripChangeSet to apply.
     */
    TripChangeSet getAllTripChanges(TripModifications tripModifications);

    /**
     * Apply a set of trip changes, returning a set that will revert the changes.
     *
     * @param changeset changes to apply
     * @return changes that would revert the supplied changes.
     */
    TripChangeSet handleTripChanges(TripChangeSet changeset);
}
