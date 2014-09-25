package org.onebusaway.twilio.actions.stops;

import java.util.List;
import java.util.Set;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.onebusaway.twilio.impl.PhoneArrivalsAndDeparturesModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Results({
  @Result(name="success", location="arrivals-and-departures", type="chain"),
  @Result(name="input", location="index", type="redirectAction")
})
public class ArrivalsAndDeparturesForStopIdAction extends TwilioSupport {

  private static Logger _log = LoggerFactory.getLogger(ArrivalsAndDeparturesForStopIdAction.class);
  
  private PhoneArrivalsAndDeparturesModel _model;

  @Autowired
  public void setModel(PhoneArrivalsAndDeparturesModel model) {
    _model = model;
  }

  public void setStopIds(List<String> stopIds) {
    _model.setStopIds(stopIds);
  }

  public void setRouteIds(Set<String> routeIds) {
    _model.setRouteFilter(routeIds);
  }

  public PhoneArrivalsAndDeparturesModel getModel() {
    return _model;
  }

  public String execute() throws Exception {
    _log.debug("in execute with stops=" + _model.getStopIds());
    if (_model.isMissingData()) {
      _log.warn("missing execpted data");
      return INPUT;
    }

    _model.process();

    logUserInteraction("stopIds", _model.getStopIds(), "routeIds",
        _model.getRouteFilter());
    
    return SUCCESS;
  }
}
