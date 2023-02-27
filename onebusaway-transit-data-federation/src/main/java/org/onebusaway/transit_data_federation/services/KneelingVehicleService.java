package org.onebusaway.transit_data_federation.services;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data_federation.impl.CancelledTripServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public interface KneelingVehicleService {

    boolean isVehicleKneeling(String vehicleId);

    boolean isVehicleKneeling(AgencyAndId vehicleId);

    Set<AgencyAndId> getKneelingVehicleIds();

    void updateKneelingVehicles(Set<AgencyAndId> kneelingVehicleCache);

    void addKneelingVehicle(AgencyAndId kneelingVehicleId);

}
