package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

import java.util.List;

public class ShapeModificationDiff {

    // The full original shape for the trip
    private List<ShapePointSnapshot> originalShape;

    // The full new shape
    private List<ShapePointSnapshot> modifiedShape;

    // Just the portion that changed
    private ShapeSegment originalSegment;
    private ShapeSegment replacementSegment;

    // The splice points — which stop IDs delimit the changed segment
    private String startStopId;
    private String endStopId;
}
