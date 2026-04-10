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
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * NOT thread-safe. All access must be protected by the ReadWriteLock
 * in ServiceAlertsServiceImpl. Read operations require the read lock,
 * write operations require the write lock.
 */
@Component
public class ServiceAlertsCacheInMemoryImpl implements ServiceAlertsCache {

  private static final class IndexEntry<T> {
    final AffectsKeyFactory<T> factory;
    final Map<T, Set<AgencyAndId>> index = new HashMap<>();

    IndexEntry(AffectsKeyFactory<T> factory) {
      this.factory = factory;
    }

    void update(AgencyAndId id, ServiceAlertRecord existing, ServiceAlertRecord updated) {
      Set<T> oldKeys = existing != null
              ? factory.getKeysForAffects(existing)
              : Collections.emptySet();
      Set<T> newKeys = updated != null
              ? factory.getKeysForAffects(updated)
              : Collections.emptySet();

      if (oldKeys.equals(newKeys)) return;

      for (T key : oldKeys) {
        if (!newKeys.contains(key)) {
          Set<AgencyAndId> ids = index.get(key);
          if (ids != null) {
            ids.remove(id);
            if (ids.isEmpty()) index.remove(key);
          }
        }
      }

      for (T key : newKeys) {
        if (!oldKeys.contains(key)) {
          index.computeIfAbsent(key, k -> new HashSet<>()).add(id);
        }
      }
    }

    Set<AgencyAndId> get(T key) {
      Set<AgencyAndId> ids = index.get(key);
      return ids != null ? Collections.unmodifiableSet(ids) : Collections.emptySet();
    }

    void clear() {
      index.clear();
    }
  }

  private final Map<AgencyAndId, ServiceAlertRecord> _serviceAlerts = new HashMap<>();

  private final IndexEntry<String> _byServiceAlertAgencyId =
          new IndexEntry<>(AffectsServiceAlertAgencyKeyFactory.INSTANCE);
  private final IndexEntry<String> _byAgencyId =
          new IndexEntry<>(AffectsAgencyKeyFactory.INSTANCE);
  private final IndexEntry<AgencyAndId> _byStopId =
          new IndexEntry<>(AffectsStopKeyFactory.INSTANCE);
  private final IndexEntry<AgencyAndId> _byRouteId =
          new IndexEntry<>(AffectsRouteKeyFactory.INSTANCE);
  private final IndexEntry<RouteAndDirectionRef> _byRouteAndDirectionId =
          new IndexEntry<>(AffectsRouteAndDirectionKeyFactory.INSTANCE);
  private final IndexEntry<RouteAndStopCallRef> _byRouteAndStop =
          new IndexEntry<>(AffectsRouteAndStopKeyFactory.INSTANCE);
  private final IndexEntry<RouteDirectionAndStopCallRef> _byRouteDirectionAndStopCall =
          new IndexEntry<>(AffectsRouteDirectionAndStopCallKeyFactory.INSTANCE);
  private final IndexEntry<AgencyAndId> _byTripId =
          new IndexEntry<>(AffectsTripKeyFactory.INSTANCE);
  private final IndexEntry<TripAndStopCallRef> _byTripAndStopId =
          new IndexEntry<>(AffectsTripAndStopKeyFactory.INSTANCE);

  private final List<IndexEntry<?>> _allIndexes = Arrays.asList(
          _byServiceAlertAgencyId,
          _byAgencyId,
          _byStopId,
          _byRouteId,
          _byRouteAndDirectionId,
          _byRouteAndStop,
          _byRouteDirectionAndStopCall,
          _byTripId,
          _byTripAndStopId
  );

  @Override
  public void clear() {
    _serviceAlerts.clear();
    _allIndexes.forEach(IndexEntry::clear);
  }

  @Override
  public void putServiceAlert(AgencyAndId id, ServiceAlertRecord alert) {
    ServiceAlertRecord existing = _serviceAlerts.get(id);
    _serviceAlerts.put(id, alert);
    updateIndexes(id, existing, alert);
  }

  @Override
  public ServiceAlertRecord removeServiceAlert(AgencyAndId id) {
    ServiceAlertRecord existing = _serviceAlerts.remove(id);
    if (existing != null) {
      updateIndexes(id, existing, null);
    }
    return existing;
  }

  @Override
  public ServiceAlertRecord getServiceAlert(AgencyAndId id) {
    return _serviceAlerts.get(id);
  }

  @Override
  public Collection<ServiceAlertRecord> getAllServiceAlerts() {
    return Collections.unmodifiableCollection(_serviceAlerts.values());
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByServiceAlertAgencyId(String agencyId) {
    return _byServiceAlertAgencyId.get(agencyId);
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByAgencyId(String agencyId) {
    return _byAgencyId.get(agencyId);
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByStopId(AgencyAndId stopId) {
    return _byStopId.get(stopId);
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByRouteId(AgencyAndId routeId) {
    return _byRouteId.get(routeId);
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByRouteAndDirectionId(RouteAndDirectionRef ref) {
    return _byRouteAndDirectionId.get(ref);
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByRouteAndStop(RouteAndStopCallRef ref) {
    return _byRouteAndStop.get(ref);
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByRouteDirectionAndStopCall(RouteDirectionAndStopCallRef ref) {
    return _byRouteDirectionAndStopCall.get(ref);
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByTripId(AgencyAndId tripId) {
    return _byTripId.get(tripId);
  }

  @Override
  public Set<AgencyAndId> getAlertIdsByTripAndStopId(TripAndStopCallRef ref) {
    return _byTripAndStopId.get(ref);
  }

  private void updateIndexes(AgencyAndId id, ServiceAlertRecord existing, ServiceAlertRecord updated) {
    _allIndexes.forEach(entry -> entry.update(id, existing, updated));
  }
}