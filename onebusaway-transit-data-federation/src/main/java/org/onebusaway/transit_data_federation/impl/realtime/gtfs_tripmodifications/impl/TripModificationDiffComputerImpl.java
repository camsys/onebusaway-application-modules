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

import org.onebusaway.geospatial.services.PolylineEncoder;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.trip_mods.*;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffComputer;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.util.services.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class TripModificationDiffComputerImpl implements TripModificationDiffComputer {

    private static final Logger _log = LoggerFactory.getLogger(TripModificationDiffComputerImpl.class);

    private TransitGraphDao _dao;

    private ConfigurationService _configurationService;

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    @Override
    public Optional<TripModificationDiff> computeDiff(String entityId,
                                            AgencyAndId tripAgencyAndId,
                                            List<StopTimeEntry> originalStopTimes,
                                            ShapePoints originalShape,
                                            Set<Integer> originalRemovedStopTimeIndices,
                                            List<StopTimeEntry> modifiedStopTimes,
                                            AgencyAndId replacementShapeId,
                                            Set<Integer> modifiedAddedStopTimeIndices,
                                            long effectiveServiceDate) {

        String tripId = AgencyAndId.convertToString(tripAgencyAndId);
        Optional<StopChangeDiffs> scDiff = getStopTimeDiffs(originalStopTimes, modifiedStopTimes,
                originalRemovedStopTimeIndices, modifiedAddedStopTimeIndices);

        if(scDiff.isPresent()) {
            StopChangeDiffs stopChangeDiffs = scDiff.get();
            List<StopChangeDiff> stopChangeDiffList = stopChangeDiffs.getStopChangeDiffs();
            Map<Integer, StopTimeSnapshot> removedBySequence = stopChangeDiffs.getRemovedStopTimes();
            Map<Integer, StopTimeSnapshot> addedBySequence = stopChangeDiffs.getAddedStopTimes();
            Map<AgencyAndId, StopTimeSnapshot> originalStopTimesSnapShot = stopChangeDiffs.getOriginalStopTimeSnapshots();
            Map<AgencyAndId, StopTimeSnapshot> modifiedStopTimesSnapShot = stopChangeDiffs.getModifiedStopTimeSnapshots();
            long lastUpdated = System.currentTimeMillis();
            ShapeModificationDiff shapeDiff = getShapeDiff(tripId, replacementShapeId, originalShape);

            return Optional.of(new TripModificationDiff(entityId,
                    tripId,
                    effectiveServiceDate,
                    lastUpdated,
                    originalStopTimesSnapShot,
                    modifiedStopTimesSnapShot,
                    stopChangeDiffList,
                    shapeDiff,
                    removedBySequence,
                    addedBySequence));
        }
        return Optional.empty();
    }

    private ShapeModificationDiff getShapeDiff(String tripId,
                                               AgencyAndId replacementShapeId,
                                               ShapePoints originalShape) {
        if (replacementShapeId != null && originalShape != null && !originalShape.isEmpty()) {
            try {
                return computeShapeDiff(originalShape, replacementShapeId);
            } catch (Exception e) {
                _log.warn("Failed to compute shape diff for trip {}: {}", tripId, e.getMessage());
            }
        }
        return null;
    }

    /**
     *
     * @param original
     * @param modified
     * @param originalRemovedStopTimeIndices
     * @param modifiedAddedStopTimeIndices
     * @return
     */
    private Optional<StopChangeDiffs> getStopTimeDiffs(
            List<StopTimeEntry> original,
            List<StopTimeEntry> modified,
            Set<Integer> originalRemovedStopTimeIndices,
            Set<Integer> modifiedAddedStopTimeIndices) {

        try {
            StopChangeDiffs stopChangeDiffs = new StopChangeDiffs();
            List<StopChangeDiff> stopChangeDiffList = new ArrayList<>();

            for (int i = 0; i < original.size(); i++) {
                StopTimeEntry originalStopTime = original.get(i);
                AgencyAndId originalStopId = originalStopTime.getStop().getId();

                StopTimeSnapshot originalStopTimeSnapshot = toSnapshot(originalStopTime, i);
                stopChangeDiffs.addOriginalStopTimeSnapshot(originalStopId, originalStopTimeSnapshot);

                if (originalRemovedStopTimeIndices.contains(i)) {
                    StopChangeDiff change = createStopChangeDiff(i, originalStopId, originalStopTimeSnapshot);
                    change.setChangeType(StopChangeDiff.ChangeType.REMOVED);
                    stopChangeDiffList.add(change);
                    stopChangeDiffs.addOriginalRemovedStopTimeBySequence(i, originalStopTimeSnapshot);
                }
            }

            for (int i = 0; i < modified.size(); i++) {
                StopTimeEntry modifiedStopTime = modified.get(i);
                StopEntry modifiedStop = modifiedStopTime.getStop();
                AgencyAndId modifiedStopId = modifiedStop.getId();
                StopTimeSnapshot modifiedStopTimeSnapshot = toSnapshot(modifiedStopTime, i);
                stopChangeDiffs.addModifiedStopTimeSnapshot(modifiedStopId, modifiedStopTimeSnapshot);

                StopChangeDiff change = createStopChangeDiff(i, modifiedStopId, modifiedStopTimeSnapshot);

                if (modifiedAddedStopTimeIndices.contains(i)) {
                    change.setChangeType(StopChangeDiff.ChangeType.ADDED);
                    stopChangeDiffs.addModifiedAddedStopTimeBySequence(i, modifiedStopTimeSnapshot);
                } else {
                    StopTimeSnapshot originalStopTimeSnapshot = stopChangeDiffs.getOriginalStopTimeSnapshots().get(modifiedStopId);
                    if(originalStopTimeSnapshot == null){
                        _log.warn("Unable to find original stop time index for stop {}",modifiedStopId);
                       continue;
                    }
                    change.setOriginalStopTime(originalStopTimeSnapshot);
                    change.setOriginalIndex(originalStopTimeSnapshot.getIndex());
                    if (timesChanged(originalStopTimeSnapshot, modifiedStopTimeSnapshot)) {
                        change.setChangeType(StopChangeDiff.ChangeType.TIME_CHANGED);
                    } else {
                        change.setChangeType(StopChangeDiff.ChangeType.UNCHANGED);
                    }
                }
                stopChangeDiffList.add(change);
            }

            stopChangeDiffList.sort(Comparator.comparingInt(c ->
                    c.getModifiedIndex() != null ? c.getModifiedIndex() : c.getOriginalIndex()));

            stopChangeDiffs.addAllStopChangeDiffs(stopChangeDiffList);

            return Optional.of(stopChangeDiffs);
        }
        catch (Exception e) {
            _log.error("Error processing stop change diffs", e);
            return Optional.empty();
        }
    }

    private StopChangeDiff createStopChangeDiff(int index,
                                               AgencyAndId originalStopId,
                                               StopTimeSnapshot originalStopTimeSnapshot) {
        StopChangeDiff change = new StopChangeDiff();
        change.setStopId(AgencyAndId.convertToString(originalStopId));
        change.setOriginalStopTime(originalStopTimeSnapshot);
        change.setOriginalIndex(index);
        return change;
    }


    // Maximum distance in meters for two shape points to be considered "the same path"
    static final double DEFAULT_SHAPE_OVERLAP_THRESHOLD_METERS = 25.0;
    static final String SHAPE_OVERLAP_THRESHOLD_CONFIG_KEY = "tripModifications.shapeOverlapThresholdMeters";

    private double getShapeOverlapThreshold() {
        return _configurationService.getConfigurationValueAsDouble(
                SHAPE_OVERLAP_THRESHOLD_CONFIG_KEY, DEFAULT_SHAPE_OVERLAP_THRESHOLD_METERS);
    }

    public ShapeModificationDiff computeShapeDiff(
            ShapePoints originalShape,
            AgencyAndId replacementShapeId) {

        ShapePoints replacement = _dao.getShape(replacementShapeId);
        if (replacement == null || replacement.isEmpty()) {
            _log.warn("No replacement shape found for shape_id {}, skipping shape diff",
                    replacementShapeId);
            return null;
        }

        // For each replacement point, record distance to nearest original point
        int replSize = replacement.getSize();
        double[] dists         = new double[replSize];
        int[]    nearestOnOrig = new int[replSize];
        for (int i = 0; i < replSize; i++) {
            double lat = replacement.getLatForIndex(i);
            double lon = replacement.getLonForIndex(i);
            int nearest = findNearestIndex(originalShape, lat, lon);
            nearestOnOrig[i] = nearest;
            dists[i] = SphericalGeometryLibrary.distance(
                    lat, lon,
                    originalShape.getLatForIndex(nearest),
                    originalShape.getLonForIndex(nearest));
        }

        double threshold = getShapeOverlapThreshold();

        // Peak divergence point ( inside the detour )
        int peakReplIdx = 0;
        for (int i = 1; i < replSize; i++) {
            if (dists[i] > dists[peakReplIdx]) peakReplIdx = i;
        }

        if (dists[peakReplIdx] < threshold) {
            _log.warn("Peak divergence {}m is below threshold {}m for shape {}, skipping shape diff",
                    String.format("%.1f", dists[peakReplIdx]), threshold, replacementShapeId);
            return null;
        }

        // Walk left from peak: last point within threshold is the divergence boundary
        int replStartIdx = 0;
        int origStartIdx = nearestOnOrig[0];
        for (int i = peakReplIdx - 1; i >= 0; i--) {
            if (dists[i] <= threshold) {
                replStartIdx = i;
                origStartIdx = nearestOnOrig[i];
                break;
            }
        }

        // Walk right from peak: first point back within threshold is the convergence boundary
        int replEndIdx = replSize - 1;
        int origEndIdx = nearestOnOrig[replSize - 1];
        for (int i = peakReplIdx + 1; i < replSize; i++) {
            if (dists[i] <= threshold) {
                replEndIdx = i;
                origEndIdx = nearestOnOrig[i];
                break;
            }
        }

        if (origStartIdx >= origEndIdx || replStartIdx >= replEndIdx) {
            _log.warn("Invalid splice indices orig=[{},{}] repl=[{},{}] for shape {}, skipping shape diff",
                    origStartIdx, origEndIdx, replStartIdx, replEndIdx, originalShape.getShapeId());
            return null;
        }

        ShapePoints prefix              = sliceShapePoints(originalShape, 0, origStartIdx);
        ShapePoints originalSegment     = sliceShapePoints(originalShape, origStartIdx, origEndIdx);
        ShapePoints suffix              = sliceShapePoints(originalShape, origEndIdx, originalShape.getSize() - 1);
        ShapePoints replacementSegment  = sliceShapePoints(replacement, replStartIdx, replEndIdx);

        ShapePoints modifiedShape = stitchShapePoints(prefix, replacementSegment, suffix);

        ShapeModificationDiff diff = new ShapeModificationDiff();
        diff.setPrefixSegment(toShapeSnapshots(prefix));
        diff.setSuffixSegment(toShapeSnapshots(suffix));
        diff.setOriginalShape(toShapeSnapshots(originalShape));
        diff.setModifiedShape(toShapeSnapshots(modifiedShape));
        diff.setOriginalSegment(toShapeSnapshots(originalSegment));
        diff.setReplacementSegment(toShapeSnapshots(replacementSegment));
        diff.setOriginalShapePolyline(encodePolyline(originalShape));
        diff.setModifiedShapePolyline(encodePolyline(modifiedShape));
        diff.setOriginalSegmentPolyline(encodePolyline(originalSegment));
        diff.setReplacementSegmentPolyline(encodePolyline(replacementSegment));
        diff.setPrefixSegmentPolyline(encodePolyline(prefix));
        diff.setSuffixSegmentPolyline(encodePolyline(suffix));

        return diff;
    }

    private String encodePolyline(ShapePoints points) {
        if (points == null || points.isEmpty()) return null;
        return PolylineEncoder.createEncodings(points.getLats(), points.getLons()).getPoints();
    }

    private List<ShapePointSnapshot> toShapeSnapshots(ShapePoints points) {
        List<ShapePointSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < points.getSize(); i++) {
            ShapePointSnapshot snap = new ShapePointSnapshot();
            snap.setLat(points.getLatForIndex(i));
            snap.setLon(points.getLonForIndex(i));
            snap.setDistTraveled(points.getDistTraveledForIndex(i));
            snapshots.add(snap);
        }
        return snapshots;
    }

    private int findNearestIndex(ShapePoints shape, double lat, double lon) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < shape.getSize(); i++) {
            double d = SphericalGeometryLibrary.distance(
                    lat, lon, shape.getLatForIndex(i), shape.getLonForIndex(i));
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private ShapePoints sliceShapePoints(ShapePoints source, int fromIdx, int toIdx) {
        int len = toIdx - fromIdx + 1;

        double[] lats        = new double[len];
        double[] lons        = new double[len];
        double[] distTraveled = new double[len];

        for (int i = 0; i < len; i++) {
            int src = fromIdx + i;
            lats[i]         = source.getLatForIndex(src);
            lons[i]         = source.getLonForIndex(src);
            distTraveled[i] = source.getDistTraveledForIndex(src);
        }

        ShapePoints slice = new ShapePoints();
        slice.setShapeId(source.getShapeId());
        slice.setLats(lats);
        slice.setLons(lons);
        slice.setDistTraveled(distTraveled);
        return slice;
    }

    private ShapePoints stitchShapePoints(ShapePoints prefix,
                                          ShapePoints middle,
                                          ShapePoints suffix) {
        int totalSize = prefix.getSize() + middle.getSize() + suffix.getSize();

        double[] lats         = new double[totalSize];
        double[] lons         = new double[totalSize];
        double[] distTraveled = new double[totalSize];

        int cursor = 0;
        for (ShapePoints part : new ShapePoints[]{prefix, middle, suffix}) {
            for (int i = 0; i < part.getSize(); i++, cursor++) {
                lats[cursor]         = part.getLatForIndex(i);
                lons[cursor]         = part.getLonForIndex(i);
                distTraveled[cursor] = part.getDistTraveledForIndex(i);
            }
        }

        // Re-compute distTraveled for the stitched shape
        recomputeDistTraveled(lats, lons, distTraveled);

        ShapePoints stitched = new ShapePoints();
        // shape_id for the modified shape ... leave null or assign a random one? TODO
        stitched.setLats(lats);
        stitched.setLons(lons);
        stitched.setDistTraveled(distTraveled);
        return stitched;
    }

    private void recomputeDistTraveled(double[] lats, double[] lons, double[] distTraveled) {
        distTraveled[0] = 0;
        for (int i = 1; i < lats.length; i++) {
            distTraveled[i] = distTraveled[i - 1] +
                    SphericalGeometryLibrary.distance(
                            lats[i - 1], lons[i - 1],
                            lats[i],     lons[i]);
        }
    }

    private StopTimeSnapshot toSnapshot(StopTimeEntry stopTime, int index) {
        StopTimeSnapshot snapshot = new StopTimeSnapshot();
        StopEntry stop = stopTime.getStop();
        snapshot.setStopId(stop.getId().toString());
        snapshot.setLat(stop.getStopLat());
        snapshot.setLon(stop.getStopLon());
        snapshot.setStopSequence(stopTime.getSequence());
        snapshot.setArrivalTime(stopTime.getArrivalTime());
        snapshot.setDepartureTime(stopTime.getDepartureTime());
        snapshot.setShapeDistTraveled(stopTime.getShapeDistTraveled());
        snapshot.setGtfsSequence(stopTime.getGtfsSequence());
        snapshot.setIndex(index);
        return snapshot;
    }

    private boolean timesChanged(StopTimeSnapshot original, StopTimeSnapshot modified) {
        return original.getArrivalTime() != modified.getArrivalTime()
                || original.getDepartureTime() != modified.getDepartureTime();
    }
}
