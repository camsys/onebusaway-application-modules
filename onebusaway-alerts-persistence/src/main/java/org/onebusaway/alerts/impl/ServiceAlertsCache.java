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
package org.onebusaway.alerts.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import java.util.Collection;
import java.util.Set;

/**
 * Cache for service alerts and their indexes.
 *
 * This interface makes no thread-safety guarantees. Callers are
 * responsible for ensuring appropriate locking before calling any method.
 */
public interface ServiceAlertsCache {
  void clear();
  void putServiceAlert(AgencyAndId id, ServiceAlertRecord alert);
  ServiceAlertRecord removeServiceAlert(AgencyAndId id);
  ServiceAlertRecord getServiceAlert(AgencyAndId id);
  Collection<ServiceAlertRecord> getAllServiceAlerts();

  Set<AgencyAndId> getAlertIdsByServiceAlertAgencyId(String agencyId);
  Set<AgencyAndId> getAlertIdsByAgencyId(String agencyId);
  Set<AgencyAndId> getAlertIdsByStopId(AgencyAndId stopId);
  Set<AgencyAndId> getAlertIdsByRouteId(AgencyAndId routeId);
  Set<AgencyAndId> getAlertIdsByRouteAndDirectionId(RouteAndDirectionRef ref);
  Set<AgencyAndId> getAlertIdsByRouteAndStop(RouteAndStopCallRef ref);
  Set<AgencyAndId> getAlertIdsByRouteDirectionAndStopCall(RouteDirectionAndStopCallRef ref);
  Set<AgencyAndId> getAlertIdsByTripId(AgencyAndId tripId);
  Set<AgencyAndId> getAlertIdsByTripAndStopId(TripAndStopCallRef ref);
}