package org.onebusaway.twilio.services;

import java.util.Map;

public interface SessionManager {
  public Map<String, Object> getContext(String sessionId);

  boolean hasContext(String key);
}
