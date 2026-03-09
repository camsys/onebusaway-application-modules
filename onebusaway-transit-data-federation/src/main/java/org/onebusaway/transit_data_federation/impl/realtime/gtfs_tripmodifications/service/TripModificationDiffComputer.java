package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.TripModificationDiff;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import com.google.transit.realtime.GtfsRealtime.TripModifications.Modification;

import java.time.LocalDate;
import java.util.List;

public interface TripModificationDiffComputer {

    /**
     * Compute the diff between the original and modified stop times for a trip, and return a TripModificationDiff object that captures the differences.
     * @param tripId trip id
     * @param originalStopTimes original stop times for the trip
     * @param modifiedStopTimes modified stop times for the trip
     * @param originalShape the original shape for the trip
     * @param replacementShapeId the shape id for the replacement shape, if the shape is modified; null otherwise
     * @param effectiveServiceDates the service dates on which the trip modification is effective
     * @return
     */
    TripModificationDiff computeDiff(
            AgencyAndId tripId,
            List<StopTimeEntry> originalStopTimes,
            List<StopTimeEntry> modifiedStopTimes,
            ShapePoints originalShape,
            AgencyAndId replacementShapeId,
            List<LocalDate> effectiveServiceDates);
}
