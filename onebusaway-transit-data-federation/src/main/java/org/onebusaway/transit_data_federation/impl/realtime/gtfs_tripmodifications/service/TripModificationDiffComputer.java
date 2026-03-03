package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.TripModificationDiff;

import java.util.List;

public interface TripModificationDiffComputer {

    /**
     * Compute the diff between the original and modified stop times for a trip, and return a TripModificationDiff object that captures the differences.
     * @param tripId
     * @param originalStopTimes
     * @param modifiedStopTimes
     * @param effectiveStartTime
     * @param effectiveEndTime
     * @return
     */
    TripModificationDiff computeDiff(
            String tripId,
            List<StopTime> originalStopTimes,
            List<StopTime> modifiedStopTimes,
            long effectiveStartTime,
            long effectiveEndTime);
}
