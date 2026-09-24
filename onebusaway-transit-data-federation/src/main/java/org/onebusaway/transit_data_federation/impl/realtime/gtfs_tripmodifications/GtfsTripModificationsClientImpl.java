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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications;

import org.onebusaway.realtime.gtfsrt.util.GtfsRealtimeDeserializer;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl.GtfsTripModificationsFetcherImpl;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.TripModificationsChanges;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.TripModificationsFeedDefinition;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.*;
import org.onebusaway.transit_data_federation.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class GtfsTripModificationsClientImpl implements GtfsTripModificationsClient {

    private static final Logger _log = LoggerFactory.getLogger(GtfsTripModificationsClientImpl.class);

    private static final String DEFAULT_FEED_ID = "default";

    // Legacy single-feed configuration, retained for any consumer of this shared library that
    // configures a single feed via the scalar setters below rather than setFeedDefinitions(...).
    private String _gtfsTripModificationsUrl;
    private boolean _enabled = false;
    private int _refreshInterval = 60;

    private List<TripModificationsFeedDefinition> _feedDefinitions;

    private final Map<String, TripModificationsFeedDefinition> _feedDefinitionsById = new ConcurrentHashMap<>();

    private final Map<String, GtfsTripModificationsFetcher> _fetchersByFeedId = new ConcurrentHashMap<>();

    private ScheduledExecutorService _scheduledExecutorService;

    private GtfsTripModificationsHandler _gtfsTripModificationsHandler;

    private TripModificationDiffCache _diffCache;

    private TripModificationConfiguration _tripModificationConfiguration;


    public void setRefreshInterval(int refreshInterval) {
        _refreshInterval = refreshInterval;
    }

    public void setGtfsTripModificationsUrl(String gtfsTripModificationsUrl) {
        _gtfsTripModificationsUrl = gtfsTripModificationsUrl;
    }

    public void setTripModificationConfiguration(TripModificationConfiguration tripModificationConfiguration) {
        _tripModificationConfiguration = tripModificationConfiguration;
    }

    public void setTripModificationsEnabled(boolean enabled) {
        _enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    /**
     * Configure the set of Trip Modifications feeds to poll. Takes precedence over the legacy
     * scalar setters above (setGtfsTripModificationsUrl/setRefreshInterval/setEnabled), which
     * remain only for backward compatibility with single-feed deployments.
     */
    public void setFeedDefinitions(List<TripModificationsFeedDefinition> feedDefinitions) {
        _feedDefinitions = feedDefinitions;
    }

    @Autowired
    public void setGtfsTripModificationsHandler(GtfsTripModificationsHandler gtfsTripModificationsHandler) {
        _gtfsTripModificationsHandler = gtfsTripModificationsHandler;
    }

    @Autowired
    public void setDiffCache(TripModificationDiffCache diffCache) {
        _diffCache = diffCache;
    }

    @PostConstruct
    public void init() {
        initializeFeeds();

        _scheduledExecutorService = Executors.newScheduledThreadPool(Math.max(1, _feedDefinitionsById.size()));

        for (TripModificationsFeedDefinition def : _feedDefinitionsById.values()) {
            if (!def.isEnabled()) {
                _log.warn("Trip Modifications feed {} is disabled", def.getFeedId());
            }
            if (!_fetchersByFeedId.containsKey(def.getFeedId())) {
                _log.warn("Gtfs Trip Modifications Fetcher is undefined for feed {}. Likely cause is invalid Trip Modifications URL {}",
                        def.getFeedId(), def.getUrl());
            }
            String feedId = def.getFeedId();
            _scheduledExecutorService.scheduleWithFixedDelay(() -> update(feedId), 0, def.getRefreshIntervalSeconds(), TimeUnit.SECONDS);
        }
    }

    private void initializeFeeds() {
        if (_feedDefinitions == null) {
            _feedDefinitions = Collections.singletonList(new TripModificationsFeedDefinition(
                    DEFAULT_FEED_ID, _gtfsTripModificationsUrl, _enabled, _refreshInterval, 0));
        }
        for (TripModificationsFeedDefinition def : _feedDefinitions) {
            _feedDefinitionsById.put(def.getFeedId(), def);
            try {
                _fetchersByFeedId.put(def.getFeedId(), new GtfsTripModificationsFetcherImpl(def.getUrl()));
            } catch (URISyntaxException | IllegalArgumentException e) {
                _log.warn("Invalid Trip Modifications URL for feed {}: {}", def.getFeedId(), def.getUrl());
            }
            if (_diffCache != null) {
                _diffCache.registerFeedPriority(def.getFeedId(), def.getPriority());
            }
        }
    }

    @Override
    public void update() {
        for (String feedId : _feedDefinitionsById.keySet()) {
            update(feedId);
        }
    }

    @Override
    public void update(String feedId) {
        try {
            TripModificationsFeedDefinition def = _feedDefinitionsById.get(feedId);
            if (def == null) {
                _log.warn("Unknown Trip Modifications feed id {}", feedId);
                return;
            }
            if (!def.isEnabled()) {
                _log.debug("Trip Modifications feed {} is not enabled", feedId);
                return;
            }
            GtfsTripModificationsFetcher fetcher = _fetchersByFeedId.get(feedId);
            if (fetcher == null) {
                _log.debug("Gtfs Trip Modifications Fetcher for feed {} is undefined. Likely cause is invalid Trip Modifications URL {}",
                        feedId, def.getUrl());
                return;
            }
            if (_tripModificationConfiguration == null) {
                _tripModificationConfiguration = new TripModificationConfiguration();
            }

            _log.info("Fetching GTFS Trip Modifications for feed {} from {}", feedId, def.getUrl());

            byte[] rawFeedMessage = fetcher.fetchFeed();

            FeedMessage feedMessage = GtfsRealtimeDeserializer.parseFeedMessage(rawFeedMessage);

            _log.info("Successfully fetched and parsed GTFS Trip Modifications feed {}", feedId);

            this.processFeed(feedId, feedMessage);
        } catch (IOException e) {
            _log.error("Error fetching or parsing feed {}: {}", feedId, e.getMessage(), e);
        } catch (Exception e) {
            _log.error("Unexpected error processing feed {}: {}", feedId, e.getMessage(), e);
        } catch (Throwable t) {
            _log.error("Error ({}) processing feed {}: {}", t.getClass().getName(), feedId, t.getMessage(), t);
        }
    }

    private void processFeed(String feedId, FeedMessage feedMessage) {
        if (isValidFeed(feedMessage)) {
            try {
                handleNewFeed(feedId, feedMessage);
            } catch (NoSuchAlgorithmException e) {
                _log.error("SHA-256 algorithm is unavailable; unable to process GTFS Trip Modifications feed {}", feedId, e);
            }
        } else {
            _log.warn("Unable to process GTFS Trip Modifications feed {}", feedId);
        }
    }

    private boolean isValidFeed(FeedMessage feedMessage) {
        if (!feedMessage.hasHeader() || feedMessage.getEntityList().isEmpty()) {
            _log.warn("Feed is empty or invalid.");
            return false;
        }
        if (!feedMessage.getHeader().hasIncrementality()) {
            _log.error("Feed incrementality not supported.");
            return false;
        }
        return true;
    }

    private void handleNewFeed(String feedId, FeedMessage feedMessage) throws NoSuchAlgorithmException {
        _log.info("Processing feed {} with {} entities.", feedId, feedMessage.getEntityList().size());
        TripModificationsChanges tripModificationsChanges = new TripModificationsChanges();
        tripModificationsChanges.setFeedTimestamp(extractFeedTimeStamp(feedMessage));
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        List<FeedEntity> sortedFeedEntities = feedMessage.getEntityList().stream()
                .sorted(Comparator.comparing(FeedEntity::getId)
                        .thenComparing(GtfsTripModificationsClientImpl::getEntityType)
                )
                .collect(Collectors.toList());

        for (FeedEntity entity : sortedFeedEntities) {
            if (entity.hasShape() && entity.getShape().getShapeId() != null) {
                tripModificationsChanges.addShape(HashUtil.getJoinedIdentifier(entity.getId(), entity.getShape().getShapeId()), entity.getShape());
            } else if (entity.hasStop()) {
                tripModificationsChanges.addStop(entity.getId(), entity.getStop());
            } else if (entity.hasTripModifications()) {
                String allTripModificationTrips = getAllTripModificationTrips(entity.getTripModifications());
                try {
                    tripModificationsChanges.addTripModification(HashUtil.getJoinedIdentifier(entity.getId(), HashUtil.getEncodedString(allTripModificationTrips)), entity.getTripModifications());
                } catch (NoSuchAlgorithmException e) {
                    _log.error("SHA-256 algorithm is unavailable", e);
                } catch (IllegalArgumentException e) {
                    _log.error("Error getting an encoded string of all of the trip modification trips: {}", allTripModificationTrips, e);
                }
            }
            md.update(entity.toByteArray());
        }
        tripModificationsChanges.setHash(md.digest());
        _gtfsTripModificationsHandler.handleTripModifications(feedId, tripModificationsChanges, _tripModificationConfiguration);

    }

    static String getAllTripModificationTrips(com.google.transit.realtime.GtfsRealtime.TripModifications tripModifications) {
        return tripModifications.getSelectedTripsList().stream()
                .flatMap(st -> st.getTripIdsList().stream())
                .sorted()
                .collect(Collectors.joining(","));
    }

    static int getEntityType(FeedEntity entity) {
        if (entity.hasAlert()) {
            return 0;
        }
        if (entity.hasShape()) {
            return 1;
        }
        if (entity.hasStop()) {
            return 2;
        }
        if (entity.hasTripModifications()) {
            return 3;
        }
        return 4;
    }

    private long extractFeedTimeStamp(FeedMessage feedMessage) {
        long feedTimeStamp = 0;
        if (feedMessage.getHeader().hasTimestamp()) {
            feedTimeStamp = TimeUnit.SECONDS.toMillis(feedMessage.getHeader().getTimestamp());
        }
        return feedTimeStamp;
    }

    @Override
    public void reapplyTripModifications() {
        for (String feedId : _feedDefinitionsById.keySet()) {
            reapplyTripModifications(feedId);
        }
    }

    @Override
    public void reapplyTripModifications(String feedId) {
        _gtfsTripModificationsHandler.resetLastUpdatedTime(feedId);
    }

}
