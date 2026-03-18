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

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.trip_mods.*;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffComputer;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TripModificationDiffComputerImpl implements TripModificationDiffComputer {

    private static final Logger _log = LoggerFactory.getLogger(TripModificationDiffComputerImpl.class);

    private TransitGraphDao _dao;

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Override
    public TripModificationDiff computeDiff(AgencyAndId tripId,
                                            List<StopTimeEntry> originalStopTimes,
                                            List<StopTimeEntry> modifiedStopTimes,
                                            ShapePoints originalShape,
                                            AgencyAndId replacementShapeId,
                                            LocalDate effectiveServiceDate) {

        List<StopChangeDiff> changes = diffStopLists(originalStopTimes, modifiedStopTimes);

        TripModificationDiff diff = new TripModificationDiff();
        diff.setTripId(tripId.toString());
        diff.setOriginalStopTimes(toSnapshots(originalStopTimes));
        diff.setModifiedStopTimes(toSnapshots(modifiedStopTimes));
        diff.setChanges(changes);
        diff.setLastUpdated(System.currentTimeMillis());
        diff.setEffectiveServiceDate(
                effectiveServiceDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        );

        // Shape diff only if a replacement shape was provided
        if (replacementShapeId != null && originalShape != null && !originalShape.isEmpty()) {
            StopTimeEntry startStop = originalStopTimes.get(0);
            StopTimeEntry endStop   = originalStopTimes.get(originalStopTimes.size() - 1);

            try {
                ShapeModificationDiff shapeDiff = computeShapeDiff(originalShape, replacementShapeId, startStop, endStop);
                diff.setShapeDiff(shapeDiff);
            } catch (Exception e) {
                _log.warn("Failed to compute shape diff for trip {}: {}", tripId, e.getMessage());
            }
        }

        return diff;
    }

    private List<StopChangeDiff>  diffStopLists(
            List<StopTimeEntry> original,
            List<StopTimeEntry> modified) {

        List<StopChangeDiff> stopChangeDiffList = new ArrayList<>();

        Map<AgencyAndId, Integer> originalIndexByStopId = new LinkedHashMap<>();
        for (int i = 0; i < original.size(); i++) {
            originalIndexByStopId.put(original.get(i).getStop().getId(), i);
        }

        Set<AgencyAndId> modifiedStopIds = modified.stream()
                .map(st -> st.getStop().getId())
                .collect(Collectors.toSet());

        for (int i = 0; i < original.size(); i++) {
            StopTimeEntry orig = original.get(i);
            if (!modifiedStopIds.contains(orig.getStop().getId())) {
                StopChangeDiff change = new StopChangeDiff();
                change.setChangeType(StopChangeDiff.ChangeType.REMOVED);
                change.setStopId(orig.getStop().getId().toString());
                change.setOriginalStopTime(toSnapshot(orig));
                change.setOriginalIndex(i);
                stopChangeDiffList.add(change);
            }
        }

        for (int i = 0; i < modified.size(); i++) {
            StopTimeEntry mod = modified.get(i);
            StopChangeDiff change = new StopChangeDiff();
            change.setStopId(mod.getStop().getId().toString());
            change.setModifiedStopTime(toSnapshot(mod));
            change.setModifiedIndex(i);

            if (!originalIndexByStopId.containsKey(mod.getStop().getId())) {
                change.setChangeType(StopChangeDiff.ChangeType.ADDED);
            } else {
                int origIdx = originalIndexByStopId.get(mod.getStop().getId());
                StopTimeEntry orig = original.get(origIdx);
                change.setOriginalStopTime(toSnapshot(orig));
                change.setOriginalIndex(origIdx);
                if (timesChanged(orig, mod)) {
                    change.setChangeType(StopChangeDiff.ChangeType.TIME_CHANGED);
                } else {
                    change.setChangeType(StopChangeDiff.ChangeType.UNCHANGED);
                }
            }
            stopChangeDiffList.add(change);
        }

        stopChangeDiffList.sort(Comparator.comparingInt(c ->
                c.getModifiedIndex() != null ? c.getModifiedIndex() : c.getOriginalIndex()));

        return stopChangeDiffList;
    }

    public ShapeModificationDiff computeShapeDiff(
            ShapePoints originalShape,
            AgencyAndId replacementShapeId,
            StopTimeEntry startStop,
            StopTimeEntry endStop) {

        // Find splice indices in the original shape
        int startIdx = findSpliceIndex(originalShape, startStop);
        int endIdx   = findSpliceIndex(originalShape, endStop);

        // If they're equal or inverted, bail
        if (startIdx >= endIdx) {
            _log.warn("Invalid splice indices [{}, {}] for shape {}, skipping shape diff",
                    startIdx, endIdx, originalShape.getShapeId());
            return null;
        }

        // Slice into three zones
        ShapePoints prefix          = sliceShapePoints(originalShape, 0, startIdx);
        ShapePoints originalSegment = sliceShapePoints(originalShape, startIdx, endIdx);
        ShapePoints suffix          = sliceShapePoints(originalShape, endIdx, originalShape.getSize() - 1);

        ShapePoints replacement = _dao.getShape(replacementShapeId);
        if (replacement == null || replacement.isEmpty()) {
            _log.warn("No replacement shape found for shape_id {}, skipping shape diff",
                    replacementShapeId);
            return null;
        }

        // Stitch together the modified full shape
        ShapePoints modifiedShape = stitchShapePoints(prefix, replacement, suffix);

        ShapeModificationDiff diff = new ShapeModificationDiff();
        diff.setPrefixSegment(toShapeSnapshots(prefix));
        diff.setSuffixSegment(toShapeSnapshots(suffix));
        diff.setOriginalShape(toShapeSnapshots(originalShape));
        diff.setModifiedShape(toShapeSnapshots(modifiedShape));
        diff.setOriginalSegment(toShapeSnapshots(originalSegment));
        diff.setReplacementSegment(toShapeSnapshots(replacement));
        diff.setStartStopId(startStop.getStop().getId().toString());
        diff.setEndStopId(endStop.getStop().getId().toString());

        return diff;
    }

    private int findSpliceIndex(ShapePoints shape, StopTimeEntry stop) {
        double shapeDistTraveled = stop.getShapeDistTraveled();

        if (shapeDistTraveled > 0 && shape.getDistTraveled() != null) {
            return findIndexByDistTraveled(shape, shapeDistTraveled);
        }

        // Fallback: nearest point by spherical distance
        _log.debug("No shapeDistTraveled for stop {}, falling back to nearest-point projection",
                stop.getStop().getId());

        return findIndexByNearestPoint(shape, stop);
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

    private int findIndexByDistTraveled(ShapePoints shape, double targetDist) {
        double[] dists = shape.getDistTraveled();
        int best = 0;
        double bestDelta = Math.abs(dists[0] - targetDist);

        for (int i = 1; i < dists.length; i++) {
            double delta = Math.abs(dists[i] - targetDist);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = i;
            }
            // dist_traveled is monotonically increasing — once we're past it, stop
            if (dists[i] > targetDist && delta > bestDelta) break;
        }
        return best;
    }

    private int findIndexByNearestPoint(ShapePoints shape, StopTimeEntry stop) {
        double stopLat = stop.getStop().getStopLat();
        double stopLon = stop.getStop().getStopLon();
        int best = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < shape.getSize(); i++) {
            double d = SphericalGeometryLibrary.distance(
                    stopLat, stopLon,
                    shape.getLatForIndex(i),
                    shape.getLonForIndex(i));
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

    private StopTimeSnapshot toSnapshot(StopTimeEntry stopTime) {
        StopTimeSnapshot snapshot = new StopTimeSnapshot();

        StopEntry stop = stopTime.getStop();
        snapshot.setStopId(stop.getId().toString());
        snapshot.setLat(stop.getStopLat());
        snapshot.setLon(stop.getStopLon());
        snapshot.setStopSequence(stopTime.getSequence());
        snapshot.setArrivalOffset(stopTime.getArrivalTime());
        snapshot.setDepartureOffset(stopTime.getDepartureTime());
        snapshot.setShapeDistTraveled(stopTime.getShapeDistTraveled());

        return snapshot;
    }

    private List<StopTimeSnapshot> toSnapshots(List<StopTimeEntry> stopTimes) {
        return stopTimes.stream()
                .map(this::toSnapshot)
                .collect(Collectors.toList());
    }

    private boolean timesChanged(StopTimeEntry original, StopTimeEntry modified) {
        return original.getArrivalTime() != modified.getArrivalTime()
                || original.getDepartureTime() != modified.getDepartureTime();
    }
}
