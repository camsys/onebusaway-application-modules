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
import org.onebusaway.probablecalls.twilio.SimpleMappingStrategy;
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

public class CustomTwilioEntryPoint extends TwilioEntryPoint  {

  protected void onActionSetup(Map<String, Object> contextMap) {
    
    //super.onActionSetup(contextMap);
    String sessionId = UUID.randomUUID().toString();
    XWorkRequestAttributes attributes = new XWorkRequestAttributes(
        new ActionContext(contextMap), sessionId);
    RequestContextHolder.setRequestAttributes(attributes);
  }
  
  protected void onActionTearDown() {

    //super.onActionTearDown();

    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    RequestContextHolder.resetRequestAttributes();
    if (attributes instanceof XWorkRequestAttributes)
      ((XWorkRequestAttributes) attributes).requestCompleted();
  }


}
