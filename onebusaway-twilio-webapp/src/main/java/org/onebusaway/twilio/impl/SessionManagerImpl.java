package org.onebusaway.twilio.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.twilio.services.SessionManager;
import org.springframework.stereotype.Component;

@Component
public class SessionManagerImpl implements SessionManager {
  private ConcurrentHashMap<String, ContextEntry> _contextEntriesByKey = new ConcurrentHashMap<String, ContextEntry>();

  private ScheduledExecutorService _executor;

  private int _sessionReaperFrequency = 60;

  private int _sessionTimeout = 7 * 60;

  /**
   * The frequency with which we'll check for stale sessions
   * 
   * @param sessionReaperFrequency time, in seconds
   */
  public void setSessionReapearFrequency(int sessionReaperFrequency) {
    _sessionReaperFrequency = sessionReaperFrequency;
  }

  /**
   * Timeout, in seconds, at which point a session will be considered stale
   * 
   * @param sessionTimeout time, in seconds
   */
  public void setSessionTimeout(int sessionTimeout) {
    _sessionTimeout = sessionTimeout;
  }

  @PostConstruct
  public void start() {
    _executor = Executors.newSingleThreadScheduledExecutor();
    _executor.scheduleAtFixedRate(new SessionCleanup(),
        _sessionReaperFrequency, _sessionReaperFrequency, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    _executor.shutdownNow();
  }

  /****
   * {@link SessionManager} Interface
   ****/

  @Override
  public Map<String, Object> getContext(String key) {
    ContextEntry entry = getOrCreateContextEntry(key);
    return entry.getContext();
  }
  
  @Override
  public boolean hasContext(String key) {
    return _contextEntriesByKey.containsKey(key);
  }

  /****
   * Private Method
   ****/

  private ContextEntry getOrCreateContextEntry(String key) {
    while (true) {
      ContextEntry entry = new ContextEntry();
      ContextEntry existingEntry = _contextEntriesByKey.putIfAbsent(key, entry);
      entry = (existingEntry == null) ? entry : existingEntry;
      if (entry.isValidAfterTouch())
        return entry;
    }
  }

  private static class ContextEntry {

    private long _lastAccess;

    private Map<String, Object> _context = new HashMap<String, Object>();

    private boolean _valid = true;

    public synchronized boolean isValidAfterTouch() {
      if (!_valid)
        return false;
      _lastAccess = System.currentTimeMillis();
      return true;
    }

    public synchronized boolean isValidAfterAccessCheck(long minTime) {
      if (_lastAccess < minTime)
        _valid = false;
      return _valid;
    }

    public Map<String, Object> getContext() {
      return _context;
    }

  }

  private class SessionCleanup implements Runnable {

    public void run() {

      long minTime = System.currentTimeMillis() - _sessionTimeout * 1000;

      Iterator<ContextEntry> it = _contextEntriesByKey.values().iterator();

      while (it.hasNext()) {
        ContextEntry entry = it.next();
        if (!entry.isValidAfterAccessCheck(minTime))
          it.remove();
      }
    }
  }
}
