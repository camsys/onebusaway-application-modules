package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.StopChangeDiff;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.TripModificationDiff;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModificationDiffComputer;

import java.util.*;
import java.util.stream.Collectors;

public class TripModificationDiffComputerImpl implements TripModificationDiffComputer {
    @Override
    public TripModificationDiff computeDiff(String tripId, List<StopTime> originalStopTimes, List<StopTime> modifiedStopTimes, long effectiveStartTime, long effectiveEndTime) {

        List<StopChangeDiff> changes = diffStopLists(originalStopTimes, modifiedStopTimes);

        TripModificationDiff diff = new TripModificationDiff();
        diff.setTripId(tripId);
        diff.setOriginalStopTimes(toSnapshots(originalStopTimes));
        diff.setModifiedStopTimes(toSnapshots(modifiedStopTimes));
        diff.setChanges(changes);
        diff.setLastUpdated(System.currentTimeMillis());
        diff.setEffectiveStartTime(effectiveStartTime);
        diff.setEffectiveEndTime(effectiveEndTime);

        return diff;
    }

    private List<StopChangeDiff>  diffStopLists(
            List<StopTime> original,
            List<StopTime> modified) {

        List<StopChangeDiff> stopChangeDiffList = new ArrayList<>();

        Map<AgencyAndId, Integer> originalIndexByStopId = new LinkedHashMap<>();
        for (int i = 0; i < original.size(); i++) {
            originalIndexByStopId.put(original.get(i).getStop().getId(), i);
        }

        Set<AgencyAndId> modifiedStopIds = modified.stream()
                .map(st -> st.getStop().getId())
                .collect(Collectors.toSet());

        for (int i = 0; i < original.size(); i++) {
            StopTime orig = original.get(i);
            if (!modifiedStopIds.contains(orig.getStop().getId())) {
                StopChangeDiff change = new StopChangeDiff();
                change.setChangeType(StopChangeDiff.ChangeType.REMOVED);
                change.setStopId(orig.getStop().getId());
                change.setOriginalStopTime(toSnapshot(orig));
                change.setOriginalIndex(i);
                stopChangeDiffList.add(change);
            }
        }

        for (int i = 0; i < modified.size(); i++) {
            StopTime mod = modified.get(i);
            StopChangeDiff change = new StopChangeDiff();
            change.setStopId(mod.getStop().getId());
            change.setModifiedStopTime(toSnapshot(mod));
            change.setModifiedIndex(i);

            if (!originalIndexByStopId.containsKey(mod.getStop().getId())) {
                change.setChangeType(StopChangeDiff.ChangeType.ADDED);
            } else {
                int origIdx = originalIndexByStopId.get(mod.getStop().getId());
                StopTime orig = original.get(origIdx);
                change.setOriginalStopTime(toSnapshot(orig));
                change.setOriginalIndex(origIdx);
                // Check if times changed TODO
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
}
