package org.onebusaway.twilio.actions;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Results({
  @Result(name="registration", location="registration", type="redirectAction"),
  @Result(name="help", location="help", type="redirectAction"),
  @Result(name="stops-index", location="stops/index", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="search-index", location="search/index", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="bookmarks-index", location="bookmarks/index", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="bookmarks-manage", location="bookmarks/manage", type="redirectAction", params={"From", "${phoneNumber}"})
})
public class IndexAction extends TwilioSupport {

  private static final long serialVersionUID = 1L;
  private static Logger _log = LoggerFactory.getLogger(IndexAction.class);
  private String digits;
  private String from;
  
  public void setDigits(String digits) {
	  this.digits = digits;
  }
  public void setFrom(String from) {
	  this.from = from;
  }
  
  @Override
  public String execute() throws Exception {
    _log.debug("in execute! with input=" + getInput());
    
    if( _currentUserService.hasPhoneNumberRegistration() ) {
      _log.debug("registration needed");
      return "registration";
    }
    _log.debug("no registration necessary");
    
    if ("0".equals(getInput())) {
      clearNextAction();
      return "help";
    } else if ("1".equals(getInput())) {
      clearNextAction();
      return "stops-index";
    } else if ("2".equals(getInput())) {
      clearNextAction();
      return "stops-index";
    } else if ("3".equals(getInput())) {
      clearNextAction();
      return "bookmarks-index";
    } else if ("4".equals(getInput())) {
      clearNextAction();
      return "bookmarks-manage";
    } else if ("6".equals(getInput())) {
    	clearNextAction();
    	return "search-index";
    } else {
      setNextAction("index");
    }
    return INPUT;
  }

}