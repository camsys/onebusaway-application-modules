package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component("diffCache")
public class TripModificationDiffCacheImpl implements org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffCache {

    private final ConcurrentHashMap<AgencyAndId, TripModificationDiff> cache = new ConcurrentHashMap<>();

    @Override
    public void put(AgencyAndId tripId, TripModificationDiff diff) {
        cache.put(tripId, diff);
    }

    @Override
    public Optional<TripModificationDiff> get(AgencyAndId tripId) {
        TripModificationDiff diff = cache.get(tripId);
        if (diff == null) return Optional.empty();

        if (!isActiveToday(diff)) {
            cache.remove(tripId);
            return Optional.empty();
        }

        return Optional.of(diff);
    }

    @Override
    public void remove(AgencyAndId tripId) {
        cache.remove(tripId);
    }

    @Override
    public void invalidateAndReplace(AgencyAndId tripId, TripModificationDiff newDiff) {
        cache.put(tripId, newDiff);
    }

    @Override
    public Collection<TripModificationDiff> getAll() {
//        LocalDate today = LocalDate.now();
//        // Evict entries lazily... better solution?
//        cache.entrySet().removeIf(e -> !isActiveToday(e.getValue()));
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public void clear() {
        cache.clear();
    }

}