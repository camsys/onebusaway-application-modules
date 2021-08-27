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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeServiceImpl implements TimeService {
    private LocalDateTime _time = null;

    private ZoneId _timeZone;

    @Override
    public LocalDateTime getCurrentTime() {
        if (_time != null)
            return _time;
        return LocalDateTime.now(getTimeZone());
    }

    @Override
    public long getCurrentTimeAsEpochMs() {
        ZonedDateTime zdt = ZonedDateTime.ofLocal(getCurrentTime(), getTimeZone(), null);
        return zdt.toEpochSecond() * 1000;
    }

    @Override
    public LocalDate getCurrentDate() {
        return getCurrentTime().toLocalDate();
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
        _time = Instant.ofEpochMilli(time)
                .atZone(getTimeZone()).toLocalDateTime();
    }

    public void setTime(String timeString) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime ldt = LocalDateTime.parse(timeString, fmt);
        ZonedDateTime zdt = ZonedDateTime.ofLocal(ldt, _timeZone, null);
        setTime(zdt.toEpochSecond() * 1000);
    }

    public void setTimeZone(ZoneId timeZone) {
        _timeZone = timeZone;
    }
}
