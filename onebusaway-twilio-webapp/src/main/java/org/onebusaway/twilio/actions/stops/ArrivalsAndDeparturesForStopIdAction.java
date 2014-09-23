package org.onebusaway.twilio.actions.stops;

import java.util.List;
import java.util.Set;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.onebusaway.twilio.impl.PhoneArrivalsAndDeparturesModel;
import org.springframework.beans.factory.annotation.Autowired;

@Results({
  @Result(name="success", location="arrivals-and-departures", type="chain"),
  @Result(name="input", location="arrivals-and-departures", type="chain")
})
public class ArrivalsAndDeparturesForStopIdAction extends TwilioSupport {

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

    if (_model.isMissingData())
      return INPUT;

    _model.process();

    logUserInteraction("stopIds", _model.getStopIds(), "routeIds",
        _model.getRouteFilter());

    return SUCCESS;
  }
}
