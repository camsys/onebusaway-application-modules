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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ServiceAlertsRefreshTask {

    private static final Logger _log = LoggerFactory.getLogger(ServiceAlertsRefreshTask.class);

    private ServiceAlertsServiceImpl _service;
    private ServiceAlertsPersistence _persister;

    @Autowired
    public void setServiceAlertsService(ServiceAlertsServiceImpl service) {
        _service = service;
    }

    @Autowired
    public void setServiceAlertsPersistence(ServiceAlertsPersistence persister) {
        _persister = persister;
    }

    @Scheduled(fixedDelayString = "${serviceAlerts.refreshInterval:60000}")
    @Transactional(readOnly = true)
    public void refresh() {
        try {
            if (_persister.needsSync()) {
                _service.sync();
            }
        } catch (Throwable t) {
            _log.error("error during background service alert refresh: ", t);
        }
    }
}
