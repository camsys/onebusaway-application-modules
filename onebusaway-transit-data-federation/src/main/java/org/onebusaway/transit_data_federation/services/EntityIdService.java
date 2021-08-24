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
package org.onebusaway.transit_data_federation.services;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * EntityIdService: lookup graph entities via bare IDs.
 *
 * This was refactored from GtfsRealtimeEntitySource
 */
public interface EntityIdService {
    AgencyAndId getTripId(String tripId);

    AgencyAndId getStopId(String stopId);

    AgencyAndId getRouteId(String routeId);

    AgencyAndId getShapeId(String shapeId);

    AgencyAndId getServiceId(String serviceId);

    String getDefaultAgencyId();
}
