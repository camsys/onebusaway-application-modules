/**
 * Copyright (C) 2025 Cambridge Systematics, Inc.
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
package org.onebusaway.admin.service.alerts.impl;

import org.onebusaway.admin.service.alerts.GtfsSupportService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component("gtfsSupportService")
public class GtfsSupportServiceImpl implements GtfsSupportService {

    @Autowired
    TransitDataService _transitDataService;

    private String _defaultAgencyId = null;

    private Map<String, String> _routeShortNameToRouteIdMap;

    public void setDefaultAgencyId(String agencyId) {
        this._defaultAgencyId = agencyId;
    }

    @Override
    public String getRouteIdForRouteShortName(String routeShortName){
        return _routeShortNameToRouteIdMap.get(routeShortName);
    }

    @Override
    public void refreshRouteShortNameToRouteIdMap(){
        ListBean<RouteBean> routes =  _transitDataService.getRoutesForAgencyId(getAgencyId());
        Map<String, String> mutableRouteMap = new HashMap<String, String>();
        for(RouteBean route : routes.getList()){
            AgencyAndId routeId = AgencyAndId.convertFromString(route.getId());
            mutableRouteMap.put(route.getShortName().toUpperCase(), routeId.toString());
        }
        _routeShortNameToRouteIdMap = Collections.unmodifiableMap(mutableRouteMap);
    }

    @Override
    public boolean hasRouteShortNameMappings(){
        return !_routeShortNameToRouteIdMap.isEmpty();
    }

    @Override
    public String getAgencyId() {
        if (_defaultAgencyId != null) return _defaultAgencyId;
        // not configured, default to the first agency
        return _transitDataService.getAgenciesWithCoverage().get(0).getAgency().getId();
    }


}
