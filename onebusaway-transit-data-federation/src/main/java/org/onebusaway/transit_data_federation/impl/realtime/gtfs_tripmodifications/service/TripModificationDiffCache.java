package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.trip_mods.TripModificationDiff;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripModificationDiffCache {
    void put(AgencyAndId tripId, TripModificationDiff diff);

    Optional<TripModificationDiff> get(AgencyAndId tripId);

    void remove(AgencyAndId tripId);

    void invalidateAndReplace(AgencyAndId tripId, TripModificationDiff newDiff);

    Collection<TripModificationDiff> getAll();

    void clear();

    default boolean isActiveToday(TripModificationDiff diff) {
        String effectiveDate = diff.getEffectiveServiceDate();
        if (effectiveDate == null || effectiveDate.isEmpty()) return false;
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return effectiveDate.equals(today);
    }
}
