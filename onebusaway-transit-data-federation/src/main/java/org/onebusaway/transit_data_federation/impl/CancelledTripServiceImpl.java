/*
 * Copyright (C)  2022 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.transit_data_federation.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.onebusaway.transit_data_federation.services.CancelledTripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CancelledTripServiceImpl implements CancelledTripService {

    protected static Logger _log = LoggerFactory.getLogger(CancelledTripServiceImpl.class);

    private Map<AgencyAndId, CancelledTripBean> _cancelledTripsCache = new ConcurrentHashMap<AgencyAndId, CancelledTripBean>();

    @Override
    public boolean isTripCancelled(String tripId) {
        CancelledTripBean cancelledTripBean = _cancelledTripsCache.get(AgencyAndId.convertFromString(tripId));

        if (cancelledTripBean != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isTripCancelled(AgencyAndId tripId) {
        CancelledTripBean cancelledTripBean = _cancelledTripsCache.get(tripId);
        if (cancelledTripBean != null) {
            return true;
        }
        return false;
    }

    @Override
    public Set<AgencyAndId> getCancelledTripIds() {
        return _cancelledTripsCache.keySet();
    }

    @Override
    public ListBean<CancelledTripBean> getAllCancelledTrips() {
        List<CancelledTripBean> serializedList = new ArrayList<>();
        for (CancelledTripBean bean : _cancelledTripsCache.values()) {
            serializedList.add(bean);
        }
        return new ListBean(serializedList, false);
    }

    @Override
    public void updateCancelledTrips(Map<AgencyAndId, CancelledTripBean> cancelledTripsCache){
        _cancelledTripsCache = cancelledTripsCache;
    }
}
