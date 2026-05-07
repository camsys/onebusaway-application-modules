/**
 * Copyright (C) 2026 Cambridge Systematics
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
package org.onebusaway.alerts.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Component
public class ServiceAlertsRefreshTask {

    private static final Logger _log = LoggerFactory.getLogger(ServiceAlertsRefreshTask.class);

    private ServiceAlertsServiceImpl _service;
    private ServiceAlertsPersistence _persister;
    private ThreadPoolTaskScheduler _taskScheduler;
    private ScheduledFuture<?> _scheduledFuture;

    @Autowired
    public void setServiceAlertsService(ServiceAlertsServiceImpl service) {
        _service = service;
    }

    @Autowired
    public void setServiceAlertsPersistence(ServiceAlertsPersistence persister) {
        _persister = persister;
    }

    @Autowired
    public void setTaskScheduler(ThreadPoolTaskScheduler taskScheduler) {
        _taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void setup() {
        _scheduledFuture = _taskScheduler.scheduleWithFixedDelay(this::refresh, Duration.ofMillis(60000));
    }

    @PreDestroy
    public void stop() {
        _log.info("Stopping ServiceAlertsRefreshTask");
        if (_scheduledFuture != null) {
            _scheduledFuture.cancel(false); // false = let current execution finish
        }
    }

    public void refresh() {
        try {
            _log.debug("refresh() called");
            if (_persister.needsSync()) {
                _service.sync();
            }
        } catch (Throwable t) {
            _log.error("error during background service alert refresh: ", t);
        }
    }
}