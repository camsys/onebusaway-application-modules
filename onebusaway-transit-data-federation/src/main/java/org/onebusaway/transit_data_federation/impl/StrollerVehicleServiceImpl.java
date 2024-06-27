/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.transit_data_federation.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.StrollerVehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StrollerVehicleServiceImpl implements StrollerVehicleService {

    private Set<AgencyAndId> _strollerVehicles =  ConcurrentHashMap.newKeySet();
    protected static Logger _log = LoggerFactory.getLogger(StrollerVehicleServiceImpl.class);

    @Override
    public boolean isVehicleStroller(String vehicleId) {
        return _strollerVehicles.contains(AgencyAndId.convertFromString(vehicleId));
    }

    @Override
    public boolean isVehicleStroller(AgencyAndId vehicleId) {
        return _strollerVehicles.contains(vehicleId);
    }

    @Override
    public Set<AgencyAndId> getStrollerVehicleIds() {
        return Set.copyOf(_strollerVehicles);
    }

    @Override
    public void updateStrollerVehicles(Set<AgencyAndId> strollerVehicleCache) {
        _strollerVehicles = Set.copyOf(strollerVehicleCache);
    }

    @Override
    public void addStrollerVehicle(AgencyAndId strollerVehicleId) {
        try {
            _strollerVehicles.add(strollerVehicleId);
        } catch (IllegalArgumentException e){
            _log.error("Unable to add strollerVehicle {}", strollerVehicleId, e);
        }
    }
}
