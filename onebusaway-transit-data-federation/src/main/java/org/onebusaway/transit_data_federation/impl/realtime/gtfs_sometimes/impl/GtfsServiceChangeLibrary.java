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

import com.camsys.transit.servicechange.DateDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class GtfsServiceChangeLibrary {
    private static Logger _log = LoggerFactory.getLogger(GtfsServiceChangeLibrary.class);

    static boolean dateIsApplicable(LocalDate date, List<DateDescriptor> serviceDates) {
        for (DateDescriptor dateDescriptor : serviceDates) {
            if (dateDescriptor.getDate() != null && dateDescriptor.getDate().isEqual(date)) {
                return true;
            }
            if (dateDescriptor.getFrom() != null) {
                if (dateDescriptor.getTo() != null) {
                    LocalDate from = dateDescriptor.getFrom();
                    LocalDate to = dateDescriptor.getTo();
                    if ((date.isEqual(from) || date.isAfter(from)) && (date.isEqual(to) || date.isBefore(to))) {
                        return true;
                    }
                } else {
                    LocalDate from = dateDescriptor.getFrom();
                    if ((date.isEqual(from) || date.isAfter(from))) {
                        return true;
                    }
                }
            } else if (dateDescriptor.getTo() != null) {
                _log.error("Not supported: to-date with no from-date specified.");
            }
        }
        return false;
    }

    static LocalDate toLocalDate(long epochTime, ZoneId timeZone) {
        return Instant.ofEpochMilli(epochTime).atZone(timeZone).toLocalDate();
    }
}
