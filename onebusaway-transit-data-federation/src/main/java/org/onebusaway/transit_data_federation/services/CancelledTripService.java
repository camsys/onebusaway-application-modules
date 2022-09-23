/**
 * Copyright (C) 2022 Metropolitan Transportation Authority
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
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;

import java.util.Map;
import java.util.Set;

public interface CancelledTripService {
    boolean isTripCancelled(String tripId);

    boolean isTripCancelled(AgencyAndId tripId);

    Set<AgencyAndId> getCancelledTripIds();

    ListBean<CancelledTripBean> getAllCancelledTrips();

    void updateCancelledTrips(Map<AgencyAndId, CancelledTripBean> cancelledTripsCache);

    void addCancelledTrip(CancelledTripBean cancelledTrip);
}
