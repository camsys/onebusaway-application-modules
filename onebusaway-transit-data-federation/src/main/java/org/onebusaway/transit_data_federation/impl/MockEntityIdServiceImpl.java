/**
 * Copyright (C) 2018 Cambridge Systematics, Inc.
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
import org.onebusaway.transit_data_federation.services.EntityIdService;

public class MockEntityIdServiceImpl implements EntityIdService {

    private String agencyId = "1";

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    private AgencyAndId id(String id) {
        return new AgencyAndId(agencyId, id);
    }

    @Override
    public AgencyAndId getTripId(String tripId) {
        return id(tripId);
    }

    @Override
    public AgencyAndId getStopId(String stopId) {
        return id(stopId);
    }

    @Override
    public AgencyAndId getRouteId(String routeId) {
        return id(routeId);
    }

    @Override
    public AgencyAndId getShapeId(String shapeId) {
        return id(shapeId);
    }

    @Override
    public AgencyAndId getServiceId(String serviceId) {
        return id(serviceId);
    }

    @Override
    public String getDefaultAgencyId() {
        return agencyId;
    }
}
