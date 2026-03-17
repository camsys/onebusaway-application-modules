package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.ModifiedTrips;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffComputer;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffService;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class TripModificationDiffServiceImpl implements TripModificationDiffService {

    private static final Logger _log = LoggerFactory.getLogger(TripModificationDiffServiceImpl.class);

    @Autowired
    private TripModificationDiffCacheImpl _diffCache;

    private final TransitGraphDao _dao;

    private final TripModificationDiffComputer _tripModificationDiffComputer;

    @Autowired
    public TripModificationDiffServiceImpl(TransitGraphDao dao, TripModificationDiffComputer tripModificationDiffComputer) {
        _dao = dao;
        _tripModificationDiffComputer = tripModificationDiffComputer;
    }

    @Override
    public Collection<TripModificationDiff> getAllActiveDiffs() {
        return _diffCache.getAll();
    }

    @Override
    public Collection<TripModificationDiff> createDiffsFromModifications(ModifiedTrips modifiedTrips) {

        for (ModifiedTrip modifiedTrip : modifiedTrips.getModifiedTrips()) {
            TripEntry tripEntry = modifiedTrip.getTripEntry();
            ShapePoints originalShape = _dao.getShape(tripEntry.getShapeId());

            TripModificationDiff diff = _tripModificationDiffComputer.computeDiff(
                    tripEntry.getId(),
                    tripEntry.getStopTimes(),
                    modifiedTrip.getStopTimes(),
                    originalShape,
                    modifiedTrip.getShapeId(),
                    modifiedTrip.getServiceDate()
            );

            if (diff == null) {
                _log.warn("Unable to compute TripModificationDiff for tripId {}. Skipping caching of diff.", tripEntry.getId());
                continue;
            }

            _diffCache.invalidateAndReplace(tripEntry.getId(), diff);
        }

        return _diffCache.getAll();
    }
}