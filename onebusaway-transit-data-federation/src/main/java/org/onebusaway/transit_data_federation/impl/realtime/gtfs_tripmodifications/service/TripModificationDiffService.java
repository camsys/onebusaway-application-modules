package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrips;

import java.util.Collection;

public interface TripModificationDiffService {
    Collection<TripModificationDiff> getAllActiveDiffs();

    Collection<TripModificationDiff> createDiffsFromModifications(ModifiedTrips modifiedTrips);
}