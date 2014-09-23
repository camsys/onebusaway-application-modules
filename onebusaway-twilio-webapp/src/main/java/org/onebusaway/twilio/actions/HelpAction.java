package org.onebusaway.twilio.actions;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Results({
  @Result(name="stops-index", location="stops/index", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="search-index", location="search/index", type="redirectAction", params={"From", "${phoneNumber}"})
})
public class HelpAction extends TwilioSupport {

  private static final long serialVersionUID = 1L;
  private static Logger _log = LoggerFactory.getLogger(HelpAction.class);
  
  @Override
  public String execute() throws Exception {
    _log.debug("in HelpAction with input=" + getInput());
        
    if (getInput() == null) {
    	return INPUT;
    } else {
    	_log.debug("Help: input: " + getInput());
	    if ("0".equals(getInput())) {
	        clearNextAction();
	        return "help";
	    } else if ("1".equals(getInput())) {
	        clearNextAction();
	        return "stops-index";
	    } else if ("6".equals(getInput())) {
	    	clearNextAction();
	    	return "search-index";
	    } else {
	    	return "";
	    }
    }
  }

}