/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google, Inc.
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
package org.onebusaway.transit_data_federation.impl.service_alerts;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts;

import java.util.HashSet;
import java.util.Set;

class AffectsStopCallKeyFactory implements
    AffectsKeyFactory<TripAndStopCallRef> {

  public static final AffectsStopCallKeyFactory INSTANCE = new AffectsStopCallKeyFactory();

  @Override
  public Set<TripAndStopCallRef> getKeysForAffects(ServiceAlertRecord serviceAlert) {

    Set<TripAndStopCallRef> refs = new HashSet<TripAndStopCallRef>();

    for (ServiceAlertsSituationAffectsClause affects : serviceAlert.getAllAffects()) {
      if (affects.getTripId() != null
          && affects.getStopId() != null
          && !(affects.getTripId() != null || affects.getDirectionId() != null || affects.getRouteId() != null)) {
        AgencyAndId tripId = ServiceAlertLibrary.agencyAndIdAndId(affects.getAgencyId(), affects.getTripId());
        AgencyAndId stopId = ServiceAlertLibrary.agencyAndIdAndId(affects.getAgencyId(), affects.getStopId());
        TripAndStopCallRef ref = new TripAndStopCallRef(tripId, stopId);
        refs.add(ref);
      }
    }

    return refs;
  }

  @Override
  public Set<TripAndStopCallRef> getKeysForAffects(ServiceAlerts.ServiceAlert serviceAlert) {

    Set<TripAndStopCallRef> refs = new HashSet<TripAndStopCallRef>();

    for (ServiceAlerts.Affects affects : serviceAlert.getAffectsList()) {
      if (affects.hasTripId()
              && affects.hasStopId()
              && !(affects.hasTripId() || affects.hasDirectionId() || affects.hasRouteId())) {
        AgencyAndId tripId = ServiceAlertLibrary.agencyAndId(affects.getTripId().getAgencyId(), affects.getTripId().getId());
        AgencyAndId stopId = ServiceAlertLibrary.agencyAndId(affects.getStopId().getAgencyId(), affects.getStopId().getId());
        TripAndStopCallRef ref = new TripAndStopCallRef(tripId, stopId);
        refs.add(ref);
      }
    }

    return refs;
  }
}
