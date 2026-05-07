/**
 * Copyright (C) 2026 Cambridge Systematics
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
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.alerts.service.ServiceAlertsService;
import org.onebusaway.util.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Component
public class ServiceAlertsServiceImpl implements ServiceAlertsService {

	private static final Logger _log = LoggerFactory.getLogger(ServiceAlertsServiceImpl.class);

	private enum AffectsType {
		AGENCY, ROUTE, ROUTE_DIRECTION, ROUTE_STOP, ROUTE_DIRECTION_STOP, TRIP, TRIP_STOP, STOP, UNSUPPORTED
	}

	private final ReadWriteLock _lock = new ReentrantReadWriteLock();
	private final Lock _readLock = _lock.readLock();
	private final Lock _writeLock = _lock.writeLock();

	private ServiceAlertsCache _cache;
	private ServiceAlertsPersistence _persister;

	@Autowired
	public void setServiceAlertsCache(ServiceAlertsCache cache) {
		_cache = cache;
	}

	@Autowired
	public void setServiceAlertsPersistence(ServiceAlertsPersistence persister) {
		_persister = persister;
	}


	@PostConstruct
	public void start() {
		try {
			loadServiceAlerts();
		} catch (Throwable t) {
			_log.error("issue loading service alerts: ", t);
		}
	}

	@PreDestroy
	public void stop() {
		_log.info("Stopping ServiceAlertsService");
	}


	/****
	 * {@link ServiceAlertsService} Interface
	 ****/

	@Override
	public ServiceAlertRecord createOrUpdateServiceAlert(ServiceAlertRecord record) {
		if (record.getServiceAlertId() == null) {
			record.setServiceAlertId(UUID.randomUUID().toString());
		}
		long lastModified = SystemTime.currentTimeMillis();
		if (record.getCreationTime() < 1L) {
			record.setCreationTime(lastModified);
		}
		AgencyAndId id = ServiceAlertLibrary.agencyAndId(record.getAgencyId(), record.getServiceAlertId());

		_writeLock.lock();
		try {
			_cache.putServiceAlert(id, record);
		} finally {
			_writeLock.unlock();
		}

		saveDBServiceAlert(record, lastModified);
		return record;
	}

	@Override
	public List<ServiceAlertRecord> createOrUpdateServiceAlerts(String agencyId, List<ServiceAlertRecord> records) {
		long lastModified = SystemTime.currentTimeMillis();
		List<ServiceAlertRecord> changed = new ArrayList<>();

		for (ServiceAlertRecord record : records) {
			if (record.getServiceAlertId() == null) {
				record.setServiceAlertId(UUID.randomUUID().toString());
			}
			if (record.getCreationTime() < 1L) {
				record.setCreationTime(lastModified);
			}
		}

		_writeLock.lock();
		try {
			for (ServiceAlertRecord record : records) {
				AgencyAndId id = ServiceAlertLibrary.agencyAndId(record.getAgencyId(), record.getServiceAlertId());
				ServiceAlertRecord existing = _cache.getServiceAlert(id);
				if (existing == null || !existing.equals(record)) {
					changed.add(record);
				}
				_cache.putServiceAlert(id, record);
			}
		} finally {
			_writeLock.unlock();
		}

		if (!changed.isEmpty()) {
			saveDBServiceAlerts(changed, lastModified);
		}
		return records;
	}

	@Override
	public ServiceAlertRecord copyServiceAlert(ServiceAlertRecord record) {
		record.setServiceAlertId(UUID.randomUUID().toString());
		long lastModified = SystemTime.currentTimeMillis();
		record.setCreationTime(lastModified);
		record.setCopy(Boolean.TRUE);
		AgencyAndId id = ServiceAlertLibrary.agencyAndId(record.getAgencyId(), record.getServiceAlertId());

		_writeLock.lock();
		try {
			_cache.putServiceAlert(id, record);
		} finally {
			_writeLock.unlock();
		}

		saveDBServiceAlert(record, lastModified);
		return record;
	}

	@Override
	public void removeServiceAlert(AgencyAndId serviceAlertId) {
		removeServiceAlerts(Arrays.asList(serviceAlertId));
	}

	@Override
	public void removeServiceAlerts(List<AgencyAndId> serviceAlertIds) {
		List<ServiceAlertRecord> toDelete = new ArrayList<>();

		_writeLock.lock();
		try {
			for (AgencyAndId serviceAlertId : serviceAlertIds) {
				ServiceAlertRecord removed = _cache.removeServiceAlert(serviceAlertId);
				if (removed != null) {
					toDelete.add(removed);
				}
			}
		} finally {
			_writeLock.unlock();
		}

		for (ServiceAlertRecord record : toDelete) {
			_log.info("deleting service alert {}", record.getServiceAlertId());
			_persister.delete(record);
		}
	}

	@Override
	public void removeAllServiceAlertsForFederatedAgencyId(String agencyId) {
		_writeLock.lock();
		try {
			// copy the set since removeServiceAlerts will modify it via cache
			Set<AgencyAndId> ids = new HashSet<>(_cache.getAlertIdsByServiceAlertAgencyId(agencyId));
			removeServiceAlerts(new ArrayList<>(ids));
		} finally {
			_writeLock.unlock();
		}
	}

	@Override
	public boolean sync() {
		List<ServiceAlertRecord> alerts = _persister.getAlerts();
		ServiceAlertsCache newCache = new ServiceAlertsCacheInMemoryImpl();
		for (ServiceAlertRecord alert : alerts) {
			AgencyAndId id = ServiceAlertLibrary.agencyAndId(alert.getAgencyId(), alert.getServiceAlertId());
			newCache.putServiceAlert(id, alert);
		}

		_writeLock.lock();
		try {
			_cache = newCache;
			_persister.markSynced();
		} finally {
			_writeLock.unlock();
		}
		return true;
	}

	@Override
	public ServiceAlertRecord getServiceAlertForId(AgencyAndId serviceAlertId) {
		return withReadLock(() -> _cache.getServiceAlert(serviceAlertId));
	}

	@Override
	public List<ServiceAlertRecord> getAllServiceAlerts() {
		return withReadLock(() -> new ArrayList<>(_cache.getAllServiceAlerts()));
	}

	@Override
	public List<ServiceAlertRecord> getServiceAlertsForFederatedAgencyId(String agencyId) {
		return withReadLock(() ->
				getServiceAlertIdsAsObjects(_cache.getAlertIdsByServiceAlertAgencyId(agencyId)));
	}

	@Override
	public List<ServiceAlertRecord> getServiceAlertsForAgencyId(long time, String agencyId) {
		return withReadLock(() ->
				getServiceAlertIdsAsObjects(_cache.getAlertIdsByAgencyId(agencyId), time));
	}

	@Override
	public List<ServiceAlertRecord> getServiceAlertsForStopId(long time, AgencyAndId stopId) {
		return withReadLock(() -> {
			Set<AgencyAndId> ids = new HashSet<>();
			ids.addAll(_cache.getAlertIdsByAgencyId(stopId.getAgencyId()));
			ids.addAll(_cache.getAlertIdsByStopId(stopId));
			return getServiceAlertIdsAsObjects(ids, time);
		});
	}

	@Override
	public List<ServiceAlertRecord> getServiceAlertsForRouteId(long time, AgencyAndId routeId) {
		return withReadLock(() ->
				getServiceAlertIdsAsObjects(_cache.getAlertIdsByRouteId(routeId), time));
	}

	@Override
	public List<ServiceAlertRecord> getServiceAlertsForRouteAndStopId(long time, AgencyAndId routeId, AgencyAndId stopId) {
		return withReadLock(() ->
				getServiceAlertIdsAsObjects(
						_cache.getAlertIdsByRouteAndStop(new RouteAndStopCallRef(routeId, stopId)), time));
	}

	@Override
	public List<ServiceAlertRecord> getServiceAlertsForRouteAndDirection(long time, AgencyAndId routeId, AgencyAndId tripId, String directionId) {
		return withReadLock(() -> {
			Set<AgencyAndId> ids = new HashSet<>();
			ids.addAll(_cache.getAlertIdsByAgencyId(routeId.getAgencyId()));
			ids.addAll(_cache.getAlertIdsByRouteId(routeId));
			ids.addAll(_cache.getAlertIdsByRouteAndDirectionId(new RouteAndDirectionRef(routeId, directionId)));
			ids.addAll(_cache.getAlertIdsByTripId(tripId));
			return getServiceAlertIdsAsObjects(ids, time);
		});
	}

	@Override
	public List<ServiceAlertRecord> getServiceAlertsForTripAndStopId(long time, AgencyAndId tripId, AgencyAndId stopId) {
		return withReadLock(() -> {
			Set<AgencyAndId> ids = new HashSet<>();
			ids.addAll(_cache.getAlertIdsByTripId(tripId));
			ids.addAll(_cache.getAlertIdsByTripAndStopId(new TripAndStopCallRef(tripId, stopId)));
			return getServiceAlertIdsAsObjects(ids, time);
		});
	}

	@Override
	public List<ServiceAlertRecord> getServiceAlerts(SituationQueryBean query) {
		return withReadLock(() -> {
			Set<AgencyAndId> ids = new HashSet<>();
			for (SituationQueryBean.AffectsBean affects : query.getAffects()) {
				AgencyAndId routeId = AgencyAndId.convertFromString(affects.getRouteId());
				AgencyAndId tripId = AgencyAndId.convertFromString(affects.getTripId());
				AgencyAndId stopId = AgencyAndId.convertFromString(affects.getStopId());

				AffectsType type = getAffectsType(affects.getAgencyId(), affects.getRouteId(),
						affects.getDirectionId(), affects.getTripId(), affects.getStopId());
				switch (type) {
					case AGENCY:
						ids.addAll(_cache.getAlertIdsByAgencyId(affects.getAgencyId()));
						break;
					case ROUTE:
						ids.addAll(_cache.getAlertIdsByRouteId(routeId));
						break;
					case TRIP:
						ids.addAll(_cache.getAlertIdsByTripId(tripId));
						break;
					case STOP:
						ids.addAll(_cache.getAlertIdsByStopId(stopId));
						break;
					case ROUTE_DIRECTION:
						ids.addAll(_cache.getAlertIdsByRouteAndDirectionId(
								new RouteAndDirectionRef(routeId, affects.getDirectionId())));
						break;
					case ROUTE_DIRECTION_STOP:
						ids.addAll(_cache.getAlertIdsByRouteDirectionAndStopCall(
								new RouteDirectionAndStopCallRef(routeId, affects.getDirectionId(), stopId)));
						break;
					case ROUTE_STOP:
						ids.addAll(_cache.getAlertIdsByRouteAndStop(
								new RouteAndStopCallRef(routeId, stopId)));
						break;
					case TRIP_STOP:
						ids.addAll(_cache.getAlertIdsByTripAndStopId(
								new TripAndStopCallRef(tripId, stopId)));
						break;
					default:
						_log.warn("Unhandled affects type: {}", type);
				}
			}
			return getServiceAlertIdsAsObjects(ids);
		});
	}

	@Override
	public boolean deleteOrphans() {
		return _persister.deleteOrphans();
	}

	/****
	 * Private Methods
	 ****/

	private <T> T withReadLock(Supplier<T> action) {
		_readLock.lock();
		try {
			return action.get();
		} finally {
			_readLock.unlock();
		}
	}

	private List<ServiceAlertRecord> getServiceAlertIdsAsObjects(Collection<AgencyAndId> ids) {
		return getServiceAlertIdsAsObjects(ids, -1);
	}

	private List<ServiceAlertRecord> getServiceAlertIdsAsObjects(Collection<AgencyAndId> ids, long time) {
		if (ids == null || ids.isEmpty()) return Collections.emptyList();
		List<ServiceAlertRecord> alerts = new ArrayList<>(ids.size());
		for (AgencyAndId id : ids) {
			ServiceAlertRecord alert = _cache.getServiceAlert(id);
			if (alert != null && filterByTime(alert, time)) {
				alerts.add(alert);
			}
		}
		return alerts;
	}

	private boolean filterByTime(ServiceAlertRecord alert, long time) {
		if (time == -1 || alert.getPublicationWindows().isEmpty()) return true;
		for (ServiceAlertTimeRange window : alert.getPublicationWindows()) {
			if ((window.getFromValue() == null || window.getFromValue() <= time)
					&& (window.getToValue() == null || window.getToValue() >= time)) {
				return true;
			}
		}
		return false;
	}

	private AffectsType getAffectsType(String agencyId, String routeId,
									   String directionId, String tripId, String stopId) {
		int count = getNonNullCount(agencyId, routeId, directionId, tripId, stopId);
		switch (count) {
			case 1:
				if (agencyId != null) return AffectsType.AGENCY;
				if (routeId != null) return AffectsType.ROUTE;
				if (tripId != null) return AffectsType.TRIP;
				if (stopId != null) return AffectsType.STOP;
				break;
			case 2:
				if (routeId != null && directionId != null) return AffectsType.ROUTE_DIRECTION;
				if (routeId != null && stopId != null) return AffectsType.ROUTE_STOP;
				if (tripId != null && stopId != null) return AffectsType.TRIP_STOP;
				break;
			case 3:
				if (routeId != null && directionId != null && stopId != null)
					return AffectsType.ROUTE_DIRECTION_STOP;
				break;
		}
		_log.warn("unsupported affects clause: agencyId={} routeId={} directionId={} tripId={} stopId={}",
				agencyId, routeId, directionId, tripId, stopId);
		return AffectsType.UNSUPPORTED;
	}

	private int getNonNullCount(String... ids) {
		int count = 0;
		for (String id : ids) if (id != null) count++;
		return count;
	}

	/****
	 * Persistence
	 ****/

	/**
	 * Must be called while holding the write lock, or during @PostConstruct
	 * before the bean is exposed to other threads.
	 */
	private void loadServiceAlerts() {
		List<ServiceAlertRecord> alerts = _persister.getAlerts();
		_cache.clear();
		_log.info("Loaded {} service alerts from DB", alerts.size());
		for (ServiceAlertRecord alert : alerts) {
			AgencyAndId id = ServiceAlertLibrary.agencyAndId(alert.getAgencyId(), alert.getServiceAlertId());
			_cache.putServiceAlert(id, alert);
		}
		_persister.markSynced();
		_log.info("Cache populated with {} total alerts", alerts.size());
	}

	private void saveDBServiceAlert(ServiceAlertRecord alert, long lastModified) {
		alert.setModifiedTime(lastModified);
		ServiceAlertRecord existing = _persister.getServiceAlertRecordByAlertId(
				alert.getAgencyId(), alert.getServiceAlertId());
		if (existing != null) alert.setId(existing.getId());
		_persister.saveOrUpdate(alert);
	}

	private void saveDBServiceAlerts(List<ServiceAlertRecord> alerts, long lastModified) {
		for (ServiceAlertRecord alert : alerts) {
			alert.setModifiedTime(lastModified);
			ServiceAlertRecord existing = _persister.getServiceAlertRecordByAlertId(
					alert.getAgencyId(), alert.getServiceAlertId());
			if (existing != null) alert.setId(existing.getId());
		}
		_persister.saveOrUpdate(alerts);
	}
}