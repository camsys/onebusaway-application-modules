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

import com.google.transit.realtime.GtfsRealtime.Shape;
import com.google.transit.realtime.GtfsRealtime.Stop;

import org.onebusaway.realtime.gtfsrt.util.GtfsRealtimeDeserializer;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl.GtfsTripModificationsFetcherImpl;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripModifications;

public class GtfsTripModificationsClientImpl implements GtfsTripModificationsClient {

    private static final Logger _log = LoggerFactory.getLogger(GtfsTripModificationsClientImpl.class);

    private String _gtfsTripModificationsUrl;

    private ScheduledExecutorService _scheduledExecutorService;

    private StopHandler _stopHandler;

    private ShapeHandler _shapeHandler;

    private GtfsTripModificationsHandler _gtfsTripModificationsHandler;

    private GtfsTripModificationsFetcher _gtfsTripModificationsFetcher;

    private int _refreshInterval;


    @Autowired
    public void setRefreshInterval(int refreshInterval) {
        _refreshInterval = refreshInterval;
    }
    
    @Autowired
    public void setGtfsTripModificationsUrl(String gtfsTripModificationsUrl) {
        _gtfsTripModificationsUrl = gtfsTripModificationsUrl;
    }

    @Autowired
    public void setStopHandler(StopHandler stopHandler) {
        _stopHandler = stopHandler;
    }

    @Autowired
    public void setShapeHandler(ShapeHandler shapeHandler) {
        _shapeHandler = shapeHandler;
    }

    @Autowired
    public void setGtfsTripModificationsHandler(GtfsTripModificationsHandler gtfsTripModificationsHandler) {
        _gtfsTripModificationsHandler = gtfsTripModificationsHandler;
    }

    @Autowired
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        _scheduledExecutorService = scheduledExecutorService;
    }

    @PostConstruct
    public void init() {
        try {
            _gtfsTripModificationsFetcher = new GtfsTripModificationsFetcherImpl(_gtfsTripModificationsUrl);
            _scheduledExecutorService.scheduleAtFixedRate(this::update, 0, _refreshInterval, TimeUnit.SECONDS);
        } catch (URISyntaxException ex) {
            _log.error("init failed", ex);
        }
    }

    @Override
    public void update() {
        try {
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
        if (!feedMessage.hasHeader() || feedMessage.getEntityList().isEmpty()) {
            _log.warn("Feed is empty or invalid.");
            return;
        }
        if (!feedMessage.getHeader().hasIncrementality()) {
            _log.error("Feed incrementality not supported.");
            return;
        }

        handleNewFeed(feedMessage);
    }

    private void handleNewFeed(FeedMessage feedMessage) {
        List<TripModifications> tripModificationsList = new ArrayList<>();
        _log.info("Processing feed with {} entities.", feedMessage.getEntityList().size());

        List<Shape> shapesList = new ArrayList<>();
        List<Stop> stopsList = new ArrayList<>();

        for (FeedEntity entity : feedMessage.getEntityList()) {
            if (entity.hasShape()) {
                shapesList.add(entity.getShape());
            }
            if (entity.hasStop()) {
                Stop stop = entity.getStop();
                stopsList.add(stop);
                _log.info("Reading new stop ID: {}, name: {}, Latlon: {},{}",
                        stop.getStopId(),
                        stop.getStopName(),
                        stop.getStopLat(),
                        stop.getStopLon());
            }
            if (entity.hasTripModifications()) {
                _log.info("Reading trip modifications for entity ID: {}", entity.getId());
                tripModificationsList.add(entity.getTripModifications());
                TripModifications tm = entity.getTripModifications();
                _log.info("TripModification: {}", tm.toString());
            }
        }
        int totalMods = tripModificationsList.size();
        int totalShapes = shapesList.size();
        int totalStops = stopsList.size();
        int numberOfSuccessfullyAddedStops = _stopHandler.addStops(stopsList).size();
        _log.info("Stops: processed {} stops, corresponding to {} successful new stop additions", totalStops, numberOfSuccessfullyAddedStops);
        int numberOfSuccessfullyAddedShapes = _shapeHandler.addShapes(shapesList).size();
        _log.info("Shapes: processed {} shapes, corresponding to {} successful new shape additions", totalShapes, numberOfSuccessfullyAddedShapes);
        int successfulTripChanges = _gtfsTripModificationsHandler.handleTripModifications(feedMessage.getHeader().getTimestamp(), tripModificationsList);
        _log.info("Trip changes: processed {} trip changes, corresponding to {} successful internal changes", totalMods, successfulTripChanges);
    }

}
