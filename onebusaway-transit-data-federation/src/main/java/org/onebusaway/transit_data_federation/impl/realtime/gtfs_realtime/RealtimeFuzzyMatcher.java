/**
 * Copyright (C) 2024 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Incoming real-time may paritally match the tripIds in the schedule.  In those cases,
 * evaluate the match and select the "best" match for consideration.
 *
 * Currently, this is a trivial implementation based on trip_id subset and service_id temporal
 * relevance.  But these algorithms could be pulled out and implemented in agency specific ways.
 */
public class RealtimeFuzzyMatcher {

  private static final Logger _log = LoggerFactory.getLogger(RealtimeFuzzyMatcher.class);
  private static final int DEFAULT_REFRESH_INTERVAL = 60;
  private TransitGraphDao dao;
  private ExtendedCalendarService calendarService;
  private Map<AgencyAndId, TripEntry> _cache = new HashMap<AgencyAndId, TripEntry>();
  private Set<AgencyAndId> _nullCache = new HashSet<>();
  private ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();
  private int _cacheResetFrequencyMinutes = DEFAULT_REFRESH_INTERVAL;
  private List<String> _tripIdRegexs = new ArrayList<>();
  private Map<AgencyAndId, List<TripEntry>> _baseTripsByAgencyId = new HashMap<>();
  private Set<String> _agencies = new HashSet<>();
  boolean isInitialized = false;
  boolean firstRun = true;

  public void setAgencies(Set<String> agencies) {
    _agencies.addAll(agencies);
  }

  public void setTripIdRegexs(List<String> tripIdRegexs) {
    _tripIdRegexs.addAll(tripIdRegexs);
  }

  public void setRefreshInterval(int refreshIntervalMinutes) {
    _cacheResetFrequencyMinutes = refreshIntervalMinutes;
  }

  public RealtimeFuzzyMatcher(TransitGraphDao transitGraphDao, ExtendedCalendarService calendarService) {
    this.dao = transitGraphDao;
    this.calendarService = calendarService;
  }

  @PostConstruct
  public void start() {
    _executor.scheduleAtFixedRate(new RefreshTask(), 2,
            _cacheResetFrequencyMinutes,
            TimeUnit.MINUTES);
  }

  @PreDestroy
  public void stop() {
    if (_executor != null) {
      _executor.shutdown();
    }
  }

  public synchronized void reset() {
    isInitialized = false;
    long start = System.currentTimeMillis();
   _cache.clear();
   _nullCache.clear();
    for (TripEntry trip : dao.getAllTrips()) {
      if (_agencies.contains(trip.getId().getAgencyId())) {
        String baseTripId = trip.getId().getId();
        for (String tripIdRegex : _tripIdRegexs) {
          baseTripId = baseTripId.replaceAll(tripIdRegex, "");
        }
        AgencyAndId baseTripAndAgencyId = new AgencyAndId(trip.getId().getAgencyId(), baseTripId);
        if (!_baseTripsByAgencyId.containsKey(baseTripAndAgencyId)) {
          _baseTripsByAgencyId.put(baseTripAndAgencyId, new ArrayList<>());
        }
        _baseTripsByAgencyId.get(baseTripAndAgencyId).add(trip);
      }
    }

    long end = System.currentTimeMillis();
    _log.info("index built in " + (end - start) + " ms");
    isInitialized = true;
  }

  public synchronized TripEntry findTrip(AgencyAndId tripId, long currentTime) {
    if (firstRun)
    {
      start();
      firstRun = false;
    }

    if (!isInitialized) {
      return null;
    }

    if (_cache.containsKey(tripId)) {
      return _cache.get(tripId);
    }
    if (_nullCache.contains(tripId)) {
      return null;
    }
    TripEntry trip = getBestTrip(tripId, currentTime);
    if (trip != null) {
      logResult(trip, tripId);
      _cache.put(tripId, trip);
    } else {
      _nullCache.add(tripId);
      // we can't log this here as we try multiple agencies
    }
    return trip;
  }

  private void logResult(TripEntry trip, AgencyAndId tripId) {
    Date currentServiceDay = new ServiceDate().getAsDate();
    Set<Date> datesForServiceIds = calendarService.getDatesForServiceIds(new ServiceIdActivation(trip.getServiceId()));
    if (!datesForServiceIds.contains(currentServiceDay)) {
      _log.error("trip {} matched to {} on serviceId {}/{}", tripId, trip.getId(), trip.getServiceId(), datesForServiceIds);
    }
  }

  private TripEntry getBestTrip(AgencyAndId tripId, long currentTime) {
    List<TripEntry> possibleTrips = getPossibleTrips(tripId);
    if (possibleTrips.isEmpty()) {
      return null;
    }
    List<TripScore> scoredTrips = getScore(possibleTrips, currentTime);
    if (scoredTrips.isEmpty()) {
      return null;
    }
    Collections.sort(scoredTrips);
    return scoredTrips.get(0).getTrip();
  }

  private List<TripScore> getScore(List<TripEntry> possibleTrips, long currentTime) {
    // score is the noon-time service date difference from now
    List<TripScore> scores = new ArrayList<>();
    for (TripEntry trip : possibleTrips) {
      LocalizedServiceId serviceId = trip.getServiceId();
      Set<Date> datesForServiceIds = calendarService.getDatesForServiceIds(new ServiceIdActivation(serviceId));
      Date closestDate = getClosestDate(datesForServiceIds, currentTime);
      scores.add(new TripScore(trip, score(closestDate, currentTime)));
    }

    return scores;
  }

  private double score(Date closestDate, long currentTime) {
    // score from midday service day
    return (closestDate.getTime()+ (6 * 60 * 60 * 1000) - currentTime) / 1000 / 60;
  }

  private Date getClosestDate(Set<Date> datesForServiceIds, long currentTime) {
    List<DateDiff> diffs = new ArrayList<>();
    for (Date date : datesForServiceIds) {
      // difference from middle of service day
      diffs.add(new DateDiff(date, Math.abs(date.getTime() + (6 * 60 * 60 * 1000) - currentTime)));
    }
    return Collections.min(diffs).getDate();
  }

  private List<TripEntry> getPossibleTrips(AgencyAndId tripId) {
    List<TripEntry> possibleTrips = new ArrayList<TripEntry>();
    if (_baseTripsByAgencyId.containsKey(tripId)) {
      possibleTrips.addAll(_baseTripsByAgencyId.get(tripId));
    }
    return possibleTrips;
  }

  private static class TripScore implements Comparable<TripScore> {
    private TripEntry trip;
    private double score;

    public TripScore(TripEntry trip, double score) {
      this.trip = trip;
      this.score = score;
    }

    public TripEntry getTrip() {
      return trip;
    }
    @Override
    public int compareTo(TripScore o) {
      return Double.compare(Math.abs(score), Math.abs(o.score));
    }
  }

  private static class DateDiff implements Comparable<DateDiff> {
    private Date date;
    private long diff;
    public DateDiff(Date date, long diff) {
      this.date = date;
      this.diff = diff;
    }

    public Date getDate() {
      return date;
    }
    @Override
    public int compareTo(DateDiff o) {
      return Long.compare(Math.abs(diff), Math.abs(o.diff));
    }
  }

  private class RefreshTask implements Runnable {
    @Override
    public void run() {
      try {
        reset();
      } catch (Throwable t) {
        _log.error("error refreshing trips", t);
      }
    }
  }
}