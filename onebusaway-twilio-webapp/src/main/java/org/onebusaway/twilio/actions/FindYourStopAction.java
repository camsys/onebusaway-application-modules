package org.onebusaway.twilio.actions;

import java.util.Map;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.apache.struts2.interceptor.SessionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Results({
//  @Result(name="stops-index", location="stops/index", type="redirectAction", params={"From", "${phoneNumber}"}),
//  @Result(name="search-index", location="search/index", type="redirectAction", params={"From", "${phoneNumber}"})
	@Result(name="stops-index", location="stops/index", type="chain"),
  @Result(name="stops-index", location="stops/index", type="chain"),
  @Result(name="search-index", location="search/index", type="chain")
})
public class FindYourStopAction extends TwilioSupport implements SessionAware {

  private static final long serialVersionUID = 1L;
  private static Logger _log = LoggerFactory.getLogger(FindYourStopAction.class);
  
	private Map sessionMap;
	
	public void setSession(Map map) {
	  this.sessionMap = map;
	}
		
  @Override
  public String execute() throws Exception {
    _log.debug("in HelpAction with input=" + getInput());
        
      	Integer navState = (Integer)sessionMap.get("navState");
		if (navState == null) {
			navState = DISPLAY_DATA;
		}

//    if (getInput() == null) {
//    	return INPUT;
	  if (navState == DISPLAY_DATA) {
			sessionMap.put("navState", DO_ROUTING);
	  	  return SUCCESS;
    } else {	// Process input and route to the appropriate action.
    	_log.debug("Help: input: " + getInput());
		sessionMap.put("navState", DISPLAY_DATA);
	    if ("0".equals(getInput())) {
	        clearNextAction();
	        return "help";
	    } else if ("1".equals(getInput())) {
	        clearNextAction();
	        return "stops-index";
	    } else if ("2".equals(getInput())) {
	    	clearNextAction();
	    	return "search-index";
	    } else if ("*".equals(getInput())) {
	    	return "index";
	    } else {
	    	return "";
	    }
    }
  }

}