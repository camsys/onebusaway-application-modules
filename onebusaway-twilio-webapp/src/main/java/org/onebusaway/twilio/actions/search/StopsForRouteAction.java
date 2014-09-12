package org.onebusaway.twilio.actions.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.presentation.model.StopSelectionBean;
import org.onebusaway.presentation.services.StopSelectionService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Results({
	  @Result(name="success", location="stops-for-route-navigation", type="chain")
	  //@Result(name="success", location="stops-for-route-navigation", type="redirect")
})
public class StopsForRouteAction extends TwilioSupport {
	  private static final long serialVersionUID = 1L;
	  private static Logger _log = LoggerFactory.getLogger(IndexAction.class);

	  private StopSelectionService _stopSelectionService;

	  private RouteBean _route;

	  private NavigationBean _navigation;

	  private StopBean _stop;

	  @Autowired
	  public void setStopSelectionService(StopSelectionService stopSelectionService) {
	    _stopSelectionService = stopSelectionService;
	  }

	  public void setRoute(RouteBean route) {
	    _route = route;
	  }

	  public RouteBean getRoute() {
	    return _route;
	  }

	  public void setNavigation(NavigationBean navigation) {
	    _navigation = navigation;
	  }

	  public NavigationBean getNavigation() {
	    return _navigation;
	  }

	  public StopBean getStop() {
	    return _stop;
	  }

	  @Override
	  public String execute() throws Exception {
		  
		_log.debug("in StopsForRoute with input=" + getInput()); 
		clearInput();

	    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(_route.getId());
	    List<Integer> selectionIndices = Collections.emptyList();
	    StopSelectionBean selection = _stopSelectionService.getSelectedStops(
	        stopsForRoute, selectionIndices);
	    List<NameBean> names = new ArrayList<NameBean>(selection.getNames());

	    _navigation = new NavigationBean();
	    _navigation.setRoute(_route);
	    _navigation.setStopsForRoute(stopsForRoute);
	    _navigation.setSelectionIndices(selectionIndices);
	    _navigation.setCurrentIndex(0);
	    _navigation.setSelection(selection);
	    _navigation.setNames(names);

	    if (selection.hasStop()) {
	      _log.debug("in StopsForRoute with input=" + getInput());
	      _stop = selection.getStop();
	      return "stopFound";
	    }
    
	    return SUCCESS;
	  }

}
