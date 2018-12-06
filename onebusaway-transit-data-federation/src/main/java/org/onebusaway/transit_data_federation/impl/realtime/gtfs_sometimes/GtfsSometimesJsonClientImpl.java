/**
 * Copyright (C) 2018 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes;

import com.camsys.transit.servicechange.Feed;
import com.camsys.transit.servicechange.FeedEntity;
import com.camsys.transit.servicechange.FeedIncrementality;
import com.camsys.transit.servicechange.ServiceChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.GtfsSometimesHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GtfsSometimesJsonClientImpl {

    private static final Logger _log = LoggerFactory.getLogger(GtfsSometimesJsonClientImpl.class);

    private String _gtfsSometimesUrl;

    private int _refreshInterval = 30;

    private ScheduledExecutorService _scheduledExecutorService;

    private GtfsSometimesHandler _gtfsSometimesHandler;

    private TransitDataService _transitDataService;

    private ObjectMapper _mapper = new ObjectMapper();

    private long _lastUpdatedTimestamp = -1;

    private boolean _disabled = false;

    @Autowired
    public void setUrl(String url) {
        _gtfsSometimesUrl = url;
    }

    public void setRefreshInterval(int refreshInterval) {
        _refreshInterval = refreshInterval;
    }

    @Autowired
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        _scheduledExecutorService = scheduledExecutorService;
    }

    @Autowired
    public void setGtfsSometimesHandler(GtfsSometimesHandler gtfsSometimesHandler) {
        _gtfsSometimesHandler = gtfsSometimesHandler;
    }

    @Autowired
    public void setTransitDataService(TransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    public void setDisabled(boolean disabled) {
        _disabled = disabled;
    }

    @PostConstruct
    public void init() {
        if (!_disabled) {
            _scheduledExecutorService.scheduleAtFixedRate(this::update, 0, _refreshInterval, TimeUnit.SECONDS);
        }
    }

    public void update() {
        try {
            URL url = new URL(_gtfsSometimesUrl);
            Feed feed;
            if (url.getProtocol().equals("file")) {
                File file = new File(url.getPath());
                feed = _mapper.readValue(file, Feed.class);
            } else if (url.getProtocol().equals("http")) {
                feed = _mapper.readValue(url.openStream(), Feed.class);
            } else {
                _log.error("Protocol not supported: " + url.getProtocol());
                return;
            }
            processFeed(feed);
        } catch (IOException ex) {
            _log.error("Error processing feed: {}", ex.getMessage());
        }
    }

    private void processFeed(Feed feed) {
        // TODO check feed_version_number
        if (!feed.getFeedHeader().getIncrementality().equals(FeedIncrementality.FULL_DATASET)) {
            _log.error("Feed incrementality not supported.");
            return;
        }
        String feedName = feed.getFeedHeader().getFeedName();
        if (feedName != null) {
            if (!_transitDataService.getActiveBundleId().equals(feedName)) {
                _log.error("Feed is for a different bundle");
                return;
            }
        }
        handleNewFeed(feed);
    }

    private void handleNewFeed(Feed feed) {
        List<ServiceChange> changes = new ArrayList<>();
        for (FeedEntity entity : feed.getFeedEntities()) {
            if (entity.getServiceChange() != null) {
                changes.add(entity.getServiceChange());
            }
        }
        int total = changes.size(),
                success = _gtfsSometimesHandler.handleServiceChanges(feed.getFeedHeader().getTimestamp(), changes);
        _log.info("Service changes: processed {} service changes, corresponding to {} successful internal changes", total, success);
    }
}
