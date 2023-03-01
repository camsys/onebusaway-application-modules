package org.onebusaway.transit_data_federation.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.KneelingVehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KneelingVehicleServiceImpl implements KneelingVehicleService {

    private Set<AgencyAndId> _kneelingVehicles =  ConcurrentHashMap.newKeySet();
    protected static Logger _log = LoggerFactory.getLogger(KneelingVehicleServiceImpl.class);

    @Override
    public boolean isVehicleKneeling(String vehicleId) {
        return _kneelingVehicles.contains(AgencyAndId.convertFromString(vehicleId));
    }

    @Override
    public boolean isVehicleKneeling(AgencyAndId vehicleId) {
        return _kneelingVehicles.contains(vehicleId);
    }

    @Override
    public Set<AgencyAndId> getKneelingVehicleIds() {
        return Set.copyOf(_kneelingVehicles);
    }

    @Override
    public void updateKneelingVehicles(Set<AgencyAndId> kneelingVehicleCache) {
        _kneelingVehicles = Set.copyOf(kneelingVehicleCache);
    }

    @Override
    public void addKneelingVehicle(AgencyAndId kneelingVehicleId) {
        try {
            _kneelingVehicles.add(kneelingVehicleId);
        } catch (IllegalArgumentException e){
            _log.error("Unable to add kneelingVehicle {}", kneelingVehicleId, e);
        }
    }
}
