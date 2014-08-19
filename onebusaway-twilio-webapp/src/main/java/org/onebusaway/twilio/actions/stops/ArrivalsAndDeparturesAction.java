package org.onebusaway.twilio.actions.stops;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.onebusaway.presentation.client.RoutePresenter;
import org.onebusaway.presentation.impl.AgencyPresenter;
import org.onebusaway.presentation.impl.ArrivalAndDepartureComparator;
import org.onebusaway.presentation.services.text.TextModification;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopsWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.TransitDataConstants;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.twilio.actions.Messages;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.onebusaway.twilio.impl.PhoneArrivalsAndDeparturesModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.ValueStack;

public class ArrivalsAndDeparturesAction extends TwilioSupport {

  private static Logger _log = LoggerFactory.getLogger(ArrivalsAndDeparturesAction.class);
  
  private TextModification _routeNumberPronunciation;

  private TextModification _destinationPronunciation;

  @Autowired
  public void setDestinationPronunciation(
      @Qualifier("destinationPronunciation") TextModification destinationPronunciation) {
    _destinationPronunciation = destinationPronunciation;
  }

  @Autowired
  public void setRouteNumberPronunciation(
      @Qualifier("routeNumberPronunciation") TextModification routeNumberPronunciation) {
    _routeNumberPronunciation = routeNumberPronunciation;
  }
  
  @Override
  public String execute() throws Exception {
    _log.debug("in!");
    ActionContext context = ActionContext.getContext();
    ValueStack valueStack = context.getValueStack();
    PhoneArrivalsAndDeparturesModel model = (PhoneArrivalsAndDeparturesModel) valueStack.findValue("model");
    StopsWithArrivalsAndDeparturesBean result = model.getResult();

    buildPredictedArrivals(result.getArrivalsAndDepartures());
    return SUCCESS;
  }
  
  
  protected void buildPredictedArrivals(List<ArrivalAndDepartureBean> arrivals) {
    if (arrivals.isEmpty()) {
      addMessage(Messages.ARRIVAL_INFO_NO_SCHEDULED_ARRIVALS);
    }
    Collections.sort(arrivals, new ArrivalAndDepartureComparator());

    long now = System.currentTimeMillis();

    for (ArrivalAndDepartureBean adb : arrivals) {

      TripBean trip = adb.getTrip();
      RouteBean route = trip.getRoute();

      addMessage(Messages.ROUTE);

      String routeNumber = RoutePresenter.getNameForRoute(route);
      addText(_routeNumberPronunciation.modify(routeNumber));

      String headsign = trip.getTripHeadsign();
      if (headsign != null) {
        addMessage(Messages.TO);

        String destination = _destinationPronunciation.modify(headsign);
        addText(destination);
      }

      if (TransitDataConstants.STATUS_CANCELLED.equals(adb.getStatus())) {
        addText("is currently not in service");
        continue;
      }

      long t = adb.computeBestDepartureTime();
      boolean isPrediction = adb.hasPredictedDepartureTime();

      int min = (int) ((t - now) / 1000 / 60);

      if (min < 0) {
        min = -min;
        if (min > 60) {
          String message = isPrediction ? Messages.PREDICTED_AT_PAST_DATE
              : Messages.SCHEDULED_AT_PAST_DATE;
          addMessage(message, new Date(t));
        } else {
          String message = isPrediction ? Messages.PREDICTED_IN_PAST
              : Messages.SCHEDULED_IN_PAST;
          addMessage(message, min);
        }
      } else {
        if (min > 60) {
          String message = isPrediction ? Messages.PREDICTED_AT_FUTURE_DATE
              : Messages.SCHEDULED_AT_FUTURE_DATE;
          addMessage(message, new Date(t));
        } else {
          String message = isPrediction ? Messages.PREDICTED_IN_FUTURE
              : Messages.SCHEDULED_IN_FUTURE;
          addMessage(message, min);
        }
      }

      if (TransitDataConstants.STATUS_REROUTE.equals(adb.getStatus()))
        addText("but is currently on adverse weather re-route.");
    }

    addMessage(Messages.ARRIVAL_INFO_DISCLAIMER);

    List<AgencyBean> agencies = AgencyPresenter.getAgenciesForArrivalAndDepartures(arrivals);

    if (!agencies.isEmpty()) {
      addMessage(Messages.ARRIVAL_INFO_DATA_PROVIDED_BY);
      for (int i = 0; i < agencies.size(); i++) {
        AgencyBean agency = agencies.get(i);
        if (i == agencies.size() - 1 && agencies.size() > 1)
          addText(Messages.AND);
        addText(agency.getName());
        addText(",");
      }
    }
  }
}
