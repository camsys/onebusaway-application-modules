package org.onebusaway.twilio.actions.search;

import java.util.List;

import org.onebusaway.presentation.services.text.TextModification;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.twilio.actions.Messages;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.ValueStack;

public class MultipleRoutesFoundAction extends TwilioSupport {
	  private TextModification _routeNumberPronunciation;
	  
	  @Autowired
	  public void setRouteNumberPronunciation(
	      @Qualifier("routeNumberPronunciation") TextModification routeNumberPronunciation) {
	    _routeNumberPronunciation = routeNumberPronunciation;
	  }

	  @Override
	  public String execute() throws Exception {
	    ActionContext context = ActionContext.getContext();
	    ValueStack vs = context.getValueStack();
	    List<RouteBean> routes = (List<RouteBean>) vs.findValue("routes");
	    
	    int index = 1;
	    
	    addMessage(Messages.MULTIPLE_ROUTES_WERE_FOUND);
	    
	    for( RouteBean route : routes) {
	      
	      addMessage(Messages.FOR);
	      addMessage(Messages.ROUTE);
	      
	      String routeNumber = route.getShortName();
	      addText(_routeNumberPronunciation.modify(routeNumber));
	      
	      addMessage(Messages.OPERATED_BY);
	      addText(route.getAgency().getName());
	      
	      addMessage(Messages.PLEASE_PRESS);
	      
	      String key = Integer.toString(index++);
	      addText(key);
	      //AgiActionName action = addAction(key,"/search/tree");
	      //action.putParam("route", route);
	    }

	    addMessage(Messages.HOW_TO_GO_BACK);
	    //addAction("\\*", "/back");

	    addMessage(Messages.TO_REPEAT);
	    
	    return SUCCESS;
	}
}
