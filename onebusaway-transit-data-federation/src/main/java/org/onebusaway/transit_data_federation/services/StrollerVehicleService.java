package org.onebusaway.transit_data_federation.services;

import org.onebusaway.gtfs.model.AgencyAndId;

import java.util.Set;

public interface StrollerVehicleService {

    boolean isVehicleStroller(String vehicleId);

    boolean isVehicleStroller(AgencyAndId vehicleId);

    Set<AgencyAndId> getStrollerVehicleIds();

    void updateStrollerVehicles(Set<AgencyAndId> strollerVehicleCache);

    void addStrollerVehicle(AgencyAndId strollerVehicleId);

}
