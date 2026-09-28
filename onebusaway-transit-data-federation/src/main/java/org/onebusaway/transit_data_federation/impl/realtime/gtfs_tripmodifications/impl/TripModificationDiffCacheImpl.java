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
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("diffCache")
public class TripModificationDiffCacheImpl implements TripModificationDiffCache {

    private static final Logger _log = LoggerFactory.getLogger(TripModificationDiffCacheImpl.class);

    private final ConcurrentHashMap<String, ConcurrentHashMap<AgencyAndId, TripModificationDiff>> _cacheByFeed = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Integer> _priorityByFeed = new ConcurrentHashMap<>();

    @Override
    public void registerFeedPriority(String feedId, int priority) {
        _priorityByFeed.put(feedId, priority);
    }

    @Override
    public void put(String feedId, AgencyAndId tripId, TripModificationDiff diff) {
        _cacheByFeed.computeIfAbsent(feedId, k -> new ConcurrentHashMap<>()).put(tripId, diff);
    }

    @Override
    public TripModificationDiff get(AgencyAndId tripId) {
        return resolveAll().get(tripId);
    }

    @Override
    public void remove(String feedId, AgencyAndId tripId) {
        ConcurrentHashMap<AgencyAndId, TripModificationDiff> feedCache = _cacheByFeed.get(feedId);
        if (feedCache != null) {
            feedCache.remove(tripId);
        }
    }

    @Override
    public void replaceAll(String feedId, Map<AgencyAndId, TripModificationDiff> newEntries) {
        _cacheByFeed.put(feedId, new ConcurrentHashMap<>(newEntries));
    }

    @Override
    public Collection<TripModificationDiff> getAll() {
        return Collections.unmodifiableCollection(resolveAll().values());
    }

    @Override
    public Map<AgencyAndId, TripModificationDiff> getAllById() {
        return Collections.unmodifiableMap(resolveAll());
    }

    @Override
    public void clear(String feedId) {
        _cacheByFeed.remove(feedId);
    }

    @Override
    public void clear() {
        _cacheByFeed.clear();
    }

    /**
     * Resolves exactly one winning diff per trip id across all feeds currently in the cache.
     * When more than one feed reports a diff for the same trip, the feed with the lower
     * registered priority wins and a warning is logged, since two feeds disagreeing about the
     * same trip id is a data problem worth surfacing rather than silently papering over.
     */
    private Map<AgencyAndId, TripModificationDiff> resolveAll() {
        Map<AgencyAndId, TripModificationDiff> winners = new HashMap<>();
        Map<AgencyAndId, String> winningFeedByTrip = new HashMap<>();

        for (Map.Entry<String, ConcurrentHashMap<AgencyAndId, TripModificationDiff>> feedEntry : _cacheByFeed.entrySet()) {
            String feedId = feedEntry.getKey();
            for (Map.Entry<AgencyAndId, TripModificationDiff> tripEntry : feedEntry.getValue().entrySet()) {
                AgencyAndId tripId = tripEntry.getKey();
                if (!winners.containsKey(tripId)) {
                    winners.put(tripId, tripEntry.getValue());
                    winningFeedByTrip.put(tripId, feedId);
                    continue;
                }

                String currentWinnerFeedId = winningFeedByTrip.get(tripId);
                int currentPriority = _priorityByFeed.getOrDefault(currentWinnerFeedId, Integer.MAX_VALUE);
                int challengerPriority = _priorityByFeed.getOrDefault(feedId, Integer.MAX_VALUE);
                boolean challengerWins = challengerPriority < currentPriority;

                _log.warn("Trip modification conflict for trip {}: feeds [{}] and [{}] both report a diff; " +
                                "resolving by priority (winner={})",
                        tripId, currentWinnerFeedId, feedId, challengerWins ? feedId : currentWinnerFeedId);

                if (challengerWins) {
                    winners.put(tripId, tripEntry.getValue());
                    winningFeedByTrip.put(tripId, feedId);
                }
            }
        }
        return winners;
    }
}
