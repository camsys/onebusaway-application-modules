package org.onebusaway.twilio.actions.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.presentation.model.StopSelectionBean;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;

public class NavigationBean implements Serializable {
	  private static final long serialVersionUID = 1L;

	  private RouteBean _route;

	  private StopsForRouteBean _stopsForRoute;

	  private List<Integer> _selectionIndices = new ArrayList<Integer>();

	  private int _currentIndex = 0;

	  private StopSelectionBean _selection;

	  private List<NameBean> _names = new ArrayList<NameBean>();

	  public NavigationBean() {

	  }

	  public NavigationBean(NavigationBean bean) {
	    _route = bean._route;
	    _stopsForRoute = bean._stopsForRoute;
	    _selectionIndices = bean._selectionIndices;
	    _currentIndex = bean._currentIndex;
	    _selection = bean._selection;
	    _names = bean._names;
	  }

	  public void setRoute(RouteBean route) {
	    _route = route;
	  }

	  public RouteBean getRoute() {
	    return _route;
	  }

	  public void setStopsForRoute(StopsForRouteBean stopsForRoute) {
	    _stopsForRoute = stopsForRoute;
	  }

	  public StopsForRouteBean getStopsForRoute() {
	    return _stopsForRoute;
	  }

	  public void setSelectionIndices(List<Integer> selectionIndices) {
	    _selectionIndices = selectionIndices;
	  }

	  public List<Integer> getSelectionIndices() {
	    return _selectionIndices;
	  }

	  public int getCurrentIndex() {
	    return _currentIndex;
	  }

	  public void setCurrentIndex(int currentIndex) {
	    _currentIndex = currentIndex;
	  }

	  public void setSelection(StopSelectionBean selection) {
	    _selection = selection;
	  }

	  public StopSelectionBean getSelection() {
	    return _selection;
	  }

	  public List<NameBean> getNames() {
	    return _names;
	  }

	  public void setNames(List<NameBean> names) {
	    _names = names;
	  }
}
