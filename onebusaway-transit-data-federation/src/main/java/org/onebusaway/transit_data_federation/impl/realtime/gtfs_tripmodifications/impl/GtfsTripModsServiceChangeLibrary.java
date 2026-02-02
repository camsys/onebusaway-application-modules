package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import com.camsys.transit.servicechange.DateDescriptor;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class GtfsTripModsServiceChangeLibrary {
    private static Logger _log = LoggerFactory.getLogger(org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl.GtfsTripModsServiceChangeLibrary.class);

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

    static ServiceDate toServiceDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        return new ServiceDate(year, month, day);
    }
}
