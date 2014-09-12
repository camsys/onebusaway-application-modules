package org.onebusaway.twilio.actions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.struts2.interceptor.ParameterAware;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.twilio.actions.stops.StopForCodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class TwilioSupport extends ActionSupport implements ParameterAware {

  public static final String INPUT_KEY = "Digits";
  public static final String PHONE_NUMBER_KEY = "From";
  public static final String NEEDS_DEFAULT_SEARCH_LOCATION = "needsDefaultSearchLocation";
  private static Logger _log = LoggerFactory.getLogger(StopForCodeAction.class);
  
  protected TransitDataService _transitDataService;
  private ServiceAreaService _serviceAreaService;
  private Map<String, String[]> _parameters;
  private StringBuffer _message = new StringBuffer();
  
  
  protected void addText(String txt) {
    _log.debug(txt);
    _message.append(txt);
  }
  
  protected void addMessage(String msg) {
    _log.debug(msg);
    _message.append(" " + getText(msg) + " ");
  }
  
  protected void addMessage(String msg, Object... args) {
    _log.error("todo: discarding additonal args");
    _log.debug(msg);
    //_message.append(getText(msg));
    _message.append(" " + getText(msg) + " ");
    _log.debug(getText(msg));
  }
  
  public String getMessage() {
    return _message.toString();
  }
  
  @Autowired
  public void setTransitDataService(TransitDataService transitDataService) {
    _transitDataService = transitDataService;
  }
  
  @Autowired
  public void setServiceAreaService(ServiceAreaService serviceAreaService) {
    _serviceAreaService = serviceAreaService;
  }
  @Override
  public void setParameters(Map<String, String[]> arg0) {
    _parameters = arg0;
  }
  
  public String getInput() {
    if (_parameters != null && _parameters.containsKey(INPUT_KEY)) {
      Object val = _parameters.get(INPUT_KEY);
      if (val instanceof String[]) {
        return ((String[])val)[0];
      }
      return (String)val;
    }
    return null;
  }
  
  public String clearInput() {
    if (_parameters != null && _parameters.containsKey(INPUT_KEY)) {
      _parameters.remove(INPUT_KEY);
      Object val = _parameters.remove(INPUT_KEY);
      if (val instanceof String[]) {
        return ((String[])val)[0];
      }
      return (String)val;
    }
    return null;
  }
  
  public String getPhoneNumber() {
    if (_parameters != null && _parameters.containsKey(PHONE_NUMBER_KEY)) {
    	Object val = _parameters.get(PHONE_NUMBER_KEY);
    	if (val instanceof String[]) {
      	return ((String[])val)[0];
    	}
      return (String)val;
    }
    return null;
  }

  protected void setNextAction(String actionName) {
    ActionContext.getContext().getSession().put("twilio.nextAction", actionName);
  }
  
  protected void clearNextAction() {
    ActionContext.getContext().getSession().remove("twilio.nextAction");
  }
  
  protected CoordinateBounds getDefaultSearchArea() {
    return _serviceAreaService.getServiceArea();
  }
  
  protected void logUserInteraction(Object... objects) {
	  String text = "logUserInteraction(";
	  for (int i=0; i<objects.length; ++i) {
		  text += objects[i].toString();
		  if (i<objects.length-1) {
			  text += ", ";
		  }
	  }
	  text += ")";
	  _log.info(text);
  }
}
