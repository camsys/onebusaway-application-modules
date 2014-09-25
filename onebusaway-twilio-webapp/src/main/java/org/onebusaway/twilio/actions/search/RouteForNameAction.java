package org.onebusaway.twilio.actions.search;

import java.util.List;
import java.util.Map;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.apache.struts2.interceptor.SessionAware;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.SearchQueryBean.EQueryType;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Results({
	  @Result(name="success", location="stops-for-route", type="chain"),
	  @Result(name="multipleRoutesFound", location="multiple-routes-found", type="chain")
})
public class RouteForNameAction extends TwilioSupport implements SessionAware {
	  private static final long serialVersionUID = 1L;
	  private static Logger _log = LoggerFactory.getLogger(IndexAction.class);

	  private String _routeName;
	  private RouteBean _route;
	  private List<RouteBean> _routes;
	  private Map sessionMap;
	  
	  public void setRouteName(String routeName) {
	    _routeName = routeName;
	  }

	  public String getRouteName() {
	    return _routeName;
	  }

	  public RouteBean getRoute() {
	    return _route;
	  }

	  public List<RouteBean> getRoutes() {
	    return _routes;
	  }
	  
	  public void setSession(Map map) {
	  	  this.sessionMap = map;
	  }
		
	  public String execute() throws Exception {
	    _log.debug("in RouteForName with routeName " + _routeName); 
		  
	    CoordinateBounds bounds = getDefaultSearchArea();
	    
	    if( bounds == null) {
	      return NEEDS_DEFAULT_SEARCH_LOCATION;
	    }
	    
	    if( _routeName == null || _routeName.length() == 0) {
	    	return INPUT;
	    }

	    SearchQueryBean routesQuery = new SearchQueryBean();
	    routesQuery.setBounds(bounds);
	    routesQuery.setMaxCount(10);
	    routesQuery.setQuery(_routeName);
	    routesQuery.setType(EQueryType.BOUNDS_OR_CLOSEST);
	    
	    RoutesBean routesBean = _transitDataService.getRoutes(routesQuery);
	    List<RouteBean> routes = routesBean.getRoutes();
	    sessionMap.put("navState", new Integer(DISPLAY_DATA));	      
	    
	    logUserInteraction("route", _routeName);

	    if (routes.size() == 0) {
	      return "noRoutesFound";
	    } else if (routes.size() == 1 ) {
	      _route = routes.get(0);
	      return SUCCESS;
	    } else {
	      _routes = routes;
	      return "multipleRoutesFound";
	    }
	  }
}
