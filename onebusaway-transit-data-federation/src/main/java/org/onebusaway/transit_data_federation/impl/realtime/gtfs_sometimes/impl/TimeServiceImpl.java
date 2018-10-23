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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl;

import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Component
public class TimeServiceImpl implements TimeService {
    private long _time = -1;

    private ZoneId _timeZone;

    @Override
    public long getCurrentTime() {
        if (_time != -1)
            return _time;
        return new Date().getTime();
    }

    @Override
    public LocalDate getCurrentDate() {
        return Instant.ofEpochMilli(getCurrentTime())
                .atZone(getTimeZone()).toLocalDate();
    }

    @Override
    public ZoneId getTimeZone() {
        if (_timeZone != null) {
            return _timeZone;
        } else {
            return ZoneId.systemDefault();
        }
    }

    public void setTime(long time) {
        _time = time;
    }

    public void setTimeZone(ZoneId timeZone) {
        _timeZone = timeZone;
    }
}
