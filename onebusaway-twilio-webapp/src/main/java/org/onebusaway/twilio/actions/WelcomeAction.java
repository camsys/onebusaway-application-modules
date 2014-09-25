package org.onebusaway.twilio.actions;

import org.onebusaway.users.services.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

public class WelcomeAction extends TwilioSupport {
  private CurrentUserService _currentUserService;
  
  @Autowired
  public void setCurrentUserService(CurrentUserService currentUserService) {
    _currentUserService = currentUserService;
  }
  
  @Override
  public String execute() {
    logUserInteraction();
    if( _currentUserService.hasPhoneNumberRegistration() )
      return "registration";
    return SUCCESS;
  }
}

