package org.onebusaway.twilio.actions;

import org.onebusaway.users.services.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

public class RegistrationHandleAction extends TwilioSupport {

  private String _code;

  private CurrentUserService _currentUserService;

  public void setCode(String code) {
    _code = code;
  }

  @Autowired
  public void setCurrentUserService(CurrentUserService currentUserService) {
    _currentUserService = currentUserService;
  }

  @Override
  public String execute() {
    if (_code == null || _code.length() == 0) {
      addMessage(Messages.INVALID_REGISTRATION_CODE);
      return INPUT;
    }

    if (!_currentUserService.completePhoneNumberRegistration(_code)) {
      addMessage(Messages.INVALID_REGISTRATION_CODE);
      return INPUT;
    }

    addMessage(Messages.REGISTRATION_SUCCESSFUL);
    return SUCCESS;
  }

}
