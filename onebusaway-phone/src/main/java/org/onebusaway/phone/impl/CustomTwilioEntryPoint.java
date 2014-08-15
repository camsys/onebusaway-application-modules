package org.onebusaway.phone.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onebusaway.presentation.impl.users.XWorkRequestAttributes;
import org.onebusaway.probablecalls.twilio.TwilioEntryPoint;
import org.onebusaway.probablecalls.twilio.TwilioRequest;
import org.onebusaway.probablecalls.twilio.TwilioScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.opensymphony.xwork2.ActionContext;
import com.twilio.sdk.verbs.TwiMLResponse;

public class CustomTwilioEntryPoint extends TwilioEntryPoint implements HttpRequestHandler {

  private static final long timeout = 7; // minutes
  private static final String ACTIVE_SCRIPT = "tActiveScript";
  private static Logger _log = LoggerFactory.getLogger(CustomTwilioEntryPoint.class);
  
  private Cache<String, TwilioRequest> sessions = CacheBuilder.newBuilder().expireAfterAccess(timeout,
      TimeUnit.MINUTES).build();

  

  @Override
  public void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws ServletException, IOException {
    String to = servletRequest.getParameter("To");
    String from = servletRequest.getParameter("From");
    String digits = servletRequest.getParameter("Digits");

    debugMap(servletRequest);

    // we need to manage our own sessions as we are called from a stateless API
    _log.debug("looking session(" + from + ")");
    TwilioRequest r = sessions.getIfPresent(from);
    
    if (r == null) {
      _log.debug("did not find session(" + from + ")");
      r = createTwilioRequest(servletRequest);
    } else {
      _log.debug("found session(" + from + ")");
    }
   
    TwilioScript script = (TwilioScript) r.getContextMap().get(ACTIVE_SCRIPT);
    
    if (script == null) {
      script = _mappingStrategy.determineScript("index.twiml");
      r.getContextMap().put(ACTIVE_SCRIPT, script);
    }
    
    setRequestParams(r.getContextMap(), servletRequest);
      
    script.service(r);
    TwiMLResponse twiml = null;
    try {
      twiml = r.getResponse();
    } catch (Exception e) {
      _log.error("response broke:" + e);
    }
    if (twiml != null) {
      servletResponse.setContentType("application/xml");
      servletResponse.getWriter().print(twiml.toXML());
    }

  }

  
  private void setRequestParams(Map contextMap, HttpServletRequest servletRequest) {
    // TODO this should really be a set of static params to prevent injection attacks
    for (Object key : servletRequest.getParameterMap().keySet()) {
      contextMap.put(key, servletRequest.getParameterMap().get(key));
    }
  }

  private void debugMap(HttpServletRequest servletRequest) {
    String debugMap = "";
    for (Object key: servletRequest.getParameterMap().keySet()) {
      String value=(String)servletRequest.getParameter((String)key);
      debugMap = debugMap + " " + key +"=" + value;
    }
    _log.debug("debugMap:" + debugMap);
    
  }

  private TwilioRequest createTwilioRequest(HttpServletRequest servletRequest) {
    TwilioRequest r = new TwilioRequest();
    String contextPath = servletRequest.getContextPath();
    r.setContextPath(contextPath);
    Map<String, Object> contextMap = new HashMap<String, Object>();
    r.setContextMap(contextMap);
    String from = servletRequest.getParameter("From");
    sessions.put(from, r);
    
    HashMap<Object, Object> applicationMap = new HashMap<Object, Object>();
    applicationMap.put(PAUSE_AFTER_ACTION, getPauseBetweenActions());
    contextMap.put(ActionContext.APPLICATION, applicationMap);
    contextMap.put(ActionContext.SESSION, applicationMap);

    return r;
  }


  protected void onActionSetup(Map<String, Object> contextMap) {
    
    super.onActionSetup(contextMap);
    String sessionId = UUID.randomUUID().toString();
    XWorkRequestAttributes attributes = new XWorkRequestAttributes(
        new ActionContext(contextMap), sessionId);
    RequestContextHolder.setRequestAttributes(attributes);
  }
  
  protected void onActionTearDown() {

    super.onActionTearDown();

    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    RequestContextHolder.resetRequestAttributes();
    if (attributes instanceof XWorkRequestAttributes)
      ((XWorkRequestAttributes) attributes).requestCompleted();
  }


}
