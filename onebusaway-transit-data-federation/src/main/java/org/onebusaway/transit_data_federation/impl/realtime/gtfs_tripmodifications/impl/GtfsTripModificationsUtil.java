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

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class GtfsTripModificationsUtil {

    private static final Logger _log = LoggerFactory.getLogger(GtfsTripModificationsUtil.class);

    private final TimeService _timeService;

    private final BlockCalendarService _blockCalendarService;

    private final DateTimeFormatter SERVICE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    public GtfsTripModificationsUtil(TimeService timeService,
                                     BlockCalendarService blockCalendarService) {
        _timeService = timeService;
        _blockCalendarService = blockCalendarService;
    }

    // Note: If current time is 3pm, and a block ends at 1pm: it's next service date is tomorrow.
    // This has implications for block consistency.
    public LocalDate getActiveServiceDateForTrip(TripEntry trip) {
        long now = _timeService.getCurrentTimeAsEpochMs();
        long nowPlusOneDay = now + TimeUnit.DAYS.toMillis(1);
        List<BlockInstance> blocks = _blockCalendarService.getActiveBlocks(trip.getBlock().getId(), now, nowPlusOneDay);
        if (blocks.isEmpty())
            return null;
        // Get BlockInstance which is active with minimum service date
        BlockInstance block = Collections.min(blocks, Comparator.comparingLong(BlockInstance::getServiceDate));
        return toLocalDate(block.getServiceDate(), _timeService.getTimeZone());
    }

    public Set<LocalDate> parseServiceDates(List<String> serviceDateStrings) {
        serviceDateStrings.add("20260407");
        serviceDateStrings.add("20260408");
        serviceDateStrings.add("20260409");
        serviceDateStrings.add("20260410");
        serviceDateStrings.add("20260411");
        serviceDateStrings.add("20260412");
        return serviceDateStrings.stream()
                .map(s -> LocalDate.parse(s, SERVICE_DATE_FORMAT))
                .collect(Collectors.toSet());
    }

    public int getReferenceTime(List<StopTimeEntry> stopTimes, int startIndex) {
        if (startIndex > 1) {
            return stopTimes.get(startIndex - 1).getArrivalTime();
        }
        return stopTimes.get(0).getArrivalTime();
    }

    public int calculateReplacementStopArrivalTime(GtfsRealtime.ReplacementStop replacementStop, int referenceTime) {
        if (replacementStop.hasTravelTimeToStop()) {
            return referenceTime + replacementStop.getTravelTimeToStop();
        }
        return referenceTime;
    }

    public boolean areIdsEqual(AgencyAndId agencyAndId, String id) {
        return agencyAndId.getId().equals(id) ||
                AgencyAndId.convertToString(agencyAndId).equals(id);
    }

    /**
     * Returns Index of StopTime within the StopTimes list.
     * Possibly equivalent to OBA Stop Sequence (Not to be confused with GTFS Sequence).
     * @param originalStopTimes
     * @param selector
     * @return
     */
    public int findStopTimeIndexForSelector(List<StopTimeEntry> originalStopTimes, GtfsRealtime.StopSelector selector) {
        if (selector.hasStopSequence()) {
            int selectorStopSequence = selector.getStopSequence();
            for (int i = 0; i < originalStopTimes.size(); i++) {
                int gtfsStopSequence =  originalStopTimes.get(i).getGtfsSequence();
                if (selectorStopSequence == gtfsStopSequence) {
                    return i;
                }
            }
        }
        if (selector.hasStopId()) {
            String selectorStopId = selector.getStopId();
            for (int i = 0; i < originalStopTimes.size(); i++) {
                AgencyAndId currentAgencyAndStopId = originalStopTimes.get(i).getStop().getId();
                if (areIdsEqual(currentAgencyAndStopId, selectorStopId)) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Stop not found for selector: " + selector);
    }

    public LocalDate toLocalDate(long epochTime, ZoneId timeZone) {
        return Instant.ofEpochMilli(epochTime).atZone(timeZone).toLocalDate();
    }

    public ServiceDate toServiceDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        return new ServiceDate(year, month, day);
    }
}
