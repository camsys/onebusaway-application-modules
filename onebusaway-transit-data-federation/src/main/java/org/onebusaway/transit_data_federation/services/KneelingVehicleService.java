package org.onebusaway.transit_data_federation.services;

import org.onebusaway.gtfs.model.AgencyAndId;

import java.util.Set;

public interface KneelingVehicleService {

    boolean isVehicleKneeling(String vehicleId);

    boolean isVehicleKneeling(AgencyAndId vehicleId);

    Set<AgencyAndId> getKneelingVehicleIds();

    void updateKneelingVehicles(Set<AgencyAndId> kneelingVehicleCache);

    void addKneelingVehicle(AgencyAndId kneelingVehicleId);

}