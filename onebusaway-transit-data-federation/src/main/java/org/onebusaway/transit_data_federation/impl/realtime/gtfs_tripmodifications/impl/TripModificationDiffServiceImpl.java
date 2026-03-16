package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffService;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class TripModificationDiffServiceImpl implements TripModificationDiffService {

    @Autowired
    private TripModificationDiffCacheImpl _diffCache;

    @Override
    public Collection<TripModificationDiff> getAllActiveDiffs() {
        return _diffCache.getAll();
    }
}