/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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