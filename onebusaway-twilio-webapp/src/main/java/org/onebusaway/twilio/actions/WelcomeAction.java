package org.onebusaway.twilio.actions;

import java.util.Map;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.apache.struts2.interceptor.SessionAware;
import org.onebusaway.users.services.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Results ({
	@Result (name="registration", location="/registration", type="chain"),
	@Result (name="repeat", location="welcome", type="chain"),
  @Result(name="help", location="help", type="redirectAction"),
  @Result(name="stops-index", location="stops/index", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="find-your-stop", location="find-your-stop", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="bookmarks-index", location="bookmarks/index", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="bookmarks-manage", location="bookmarks/manage", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="search-index", location="search/index", type="redirectAction", params={"From", "${phoneNumber}"}),
  @Result(name="index", location="index", type="redirectAction", params={"From", "${phoneNumber}"})
})
public class WelcomeAction extends TwilioSupport implements SessionAware {
  
  private static final long serialVersionUID = 1L;
	private static Logger _log = LoggerFactory.getLogger(WelcomeAction.class);
    
	private CurrentUserService _currentUserService;
	private Map sessionMap;
	    
  @Autowired
  public void setCurrentUserService(CurrentUserService currentUserService) {
    _currentUserService = currentUserService;
  }
  
	public void setSession(Map map) {
	  this.sessionMap = map;
	}
		
  @Override
  public String execute() {
    logUserInteraction();
    Integer navState = (Integer)sessionMap.get("navState");
		if (navState == null) {
			navState = DISPLAY_DATA;
		}

		if (navState == DISPLAY_DATA) {
			if ( _currentUserService.hasPhoneNumberRegistration() ) {
				return "registration";
			}
			sessionMap.put("navState", DO_ROUTING);
			return SUCCESS;
		} else {	// Process input and route to the appropriate action.
			sessionMap.put("navState", DISPLAY_DATA);
			int key = 0;
			if (getInput().length() > 1 || !Character.isDigit(getInput().charAt(0))) {
				return "repeat";
			} else {
				key = Integer.parseInt(getInput());
			}
			_log.debug("key: " + key);
			switch(key) {
				case 1: return "stops-index";
				case 2: return "find-your-stop";
				case 6: return "search-index";
				default: return "find-your-stop";
			}
		}
  }
}

