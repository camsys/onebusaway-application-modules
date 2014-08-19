package org.onebusaway.twilio.actions;

import java.util.Map;

import org.apache.struts2.interceptor.ParameterAware;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.twilio.actions.stops.StopForCodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionSupport;

public class TwilioSupport extends ActionSupport implements ParameterAware {

  public static final String INPUT_KEY = "Digits";
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
    _message.append(getText(msg));
  }
  
  protected void addMessage(String msg, Object... args) {
    _log.error("todo: discarding additonal args");
    _message.append(getText(msg));
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
      return _parameters.get(INPUT_KEY)[0];
    }
    return null;
  }

  protected CoordinateBounds getDefaultSearchArea() {
    return _serviceAreaService.getServiceArea();
  }
  
  protected void logUserInteraction(Object... objects) {
    _log.info("logUserInteraction(" + objects + ")");
  }
}
