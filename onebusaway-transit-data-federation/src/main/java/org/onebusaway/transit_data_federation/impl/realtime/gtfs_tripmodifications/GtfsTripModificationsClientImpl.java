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
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class GtfsTripModificationsClientImpl implements GtfsTripModificationsClient {

    private static final Logger _log = LoggerFactory.getLogger(GtfsTripModificationsClientImpl.class);

    private String _gtfsTripModificationsUrl;

    private ScheduledExecutorService _scheduledExecutorService;

    private boolean _enabled = false;

    private GtfsTripModificationsHandler _gtfsTripModificationsHandler;

    private GtfsTripModificationsFetcher _gtfsTripModificationsFetcher;

    private int _refreshInterval = 60;


    public void setRefreshInterval(int refreshInterval) {
        _refreshInterval = refreshInterval;
    }
    
    public void setGtfsTripModificationsUrl(String gtfsTripModificationsUrl) {
        _gtfsTripModificationsUrl = gtfsTripModificationsUrl;
        try{
            _gtfsTripModificationsFetcher = new GtfsTripModificationsFetcherImpl(_gtfsTripModificationsUrl);
        } catch (URISyntaxException | IllegalArgumentException e) {
            _gtfsTripModificationsFetcher = null;
        }
    }

    public void setTripModificationsEnabled(boolean enabled) {
        _enabled = enabled;
    }

    @Autowired
    public void setGtfsTripModificationsHandler(GtfsTripModificationsHandler gtfsTripModificationsHandler) {
        _gtfsTripModificationsHandler = gtfsTripModificationsHandler;
    }


    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    @PostConstruct
    public void init() {
        if(!_enabled){
            _log.warn("GtfsTripModificationsClientImpl is disabled");
        }
        if(_gtfsTripModificationsFetcher == null){
            _log.warn("Gtfs Trip Modifications Fetcher is undefined. Likely cause is invalid Trip Modifications URL {}", _gtfsTripModificationsUrl);
        }
        _scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        _scheduledExecutorService.scheduleWithFixedDelay(this::update, 0, _refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void update() {
        try {
            if(!_enabled){
                _log.debug("GtfsTripModificationsClientImpl is not enabled");
                return;
            }
            if(_gtfsTripModificationsFetcher == null){
                _log.debug("Gtfs Trip Modifications Fetcher is undefined. Likely cause is invalid Trip Modifications URL {}", _gtfsTripModificationsUrl);
                return;
            }

            _log.info("Fetching GTFS Trip Modifications from {}", _gtfsTripModificationsUrl);

            byte[] rawFeedMessage = _gtfsTripModificationsFetcher.fetchFeed();

            FeedMessage feedMessage =  GtfsRealtimeDeserializer.parseFeedMessage(rawFeedMessage);

            _log.info("Successfully fetched and parsed GTFS Trip Modifications feed");

            this.processFeed(feedMessage);
        } catch (IOException e) {
            _log.error("Error fetching or parsing feed: {}", e.getMessage(), e);
        } catch (Exception e) {
            _log.error("Unexpected error: {}", e.getMessage(), e);
        } catch (Throwable t) {
            _log.error("Error ({}): {}", t.getClass().getName(), t.getMessage(), t);
        }

    }

    private void processFeed(FeedMessage feedMessage) {
        if(isValidFeed(feedMessage)){
            try {
                handleNewFeed(feedMessage);
            } catch (NoSuchAlgorithmException e) {
                _log.error("SHA-256 algorithm is unavailable; unable to process GTFS Trip Modifications feed", e);
            }
        } else{
            _log.warn("Unable to process GTFS Trip Modifications feed");
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

    private void handleNewFeed(FeedMessage feedMessage) throws NoSuchAlgorithmException {
        _log.info("Processing feed with {} entities.", feedMessage.getEntityList().size());
        TripModificationsChanges tripModificationsChanges = new TripModificationsChanges();
        tripModificationsChanges.setFeedTimestamp(extractFeedTimeStamp(feedMessage));
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        List<FeedEntity> sortedFeedEntities = feedMessage.getEntityList().stream()
                .sorted(Comparator.comparing(FeedEntity::getId)
                        .thenComparing(GtfsTripModificationsClientImpl::getEntityType)
                )
                .collect(Collectors.toList());

        for (FeedEntity entity : sortedFeedEntities) {
            if (entity.hasShape()) {
                tripModificationsChanges.addShape(entity.getShape());
            } else if (entity.hasStop()) {
                tripModificationsChanges.addStop(entity.getStop());
            } else if (entity.hasTripModifications()) {
                tripModificationsChanges.addTripModification(entity.getTripModifications());
            }
            md.update(entity.toByteArray());
        }
         tripModificationsChanges.setHash(md.digest());
        _gtfsTripModificationsHandler.handleTripModifications(tripModificationsChanges);

    }

    static int getEntityType(FeedEntity entity) {
        if(entity.hasAlert()){
            return 0;
        }
        if(entity.hasShape()){
            return 1;
        }
        if(entity.hasStop()){
            return 2;
        }
        if(entity.hasTripModifications()){
            return 3;
        }
        return 4;
    }

    private long extractFeedTimeStamp(FeedMessage feedMessage) {
        long feedTimeStamp = 0;
        if(feedMessage.getHeader().hasTimestamp()){
            feedTimeStamp = TimeUnit.SECONDS.toMillis(feedMessage.getHeader().getTimestamp());
        }
        return feedTimeStamp;
    }

    @Override
    public void reapplyTripModifications() {
        _gtfsTripModificationsHandler.resetLastUpdatedTime();
    }

}
