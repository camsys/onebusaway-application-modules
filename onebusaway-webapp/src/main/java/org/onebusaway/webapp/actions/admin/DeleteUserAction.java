/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.webapp.actions.admin;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.users.model.User;
import org.onebusaway.users.model.UserIndex;
import org.onebusaway.users.services.CurrentUserService;
import org.onebusaway.users.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionSupport;

@Results( {@Result(type = "redirectAction", params = {"actionName", "index"})})
public class DeleteUserAction extends ActionSupport {

  private static final long serialVersionUID = 1L;

  private UserService _userService;

  private CurrentUserService _currentUserService;

  private int _userId;

  @Autowired
  public void setUserService(UserService userService) {
    _userService = userService;
  }

  @Autowired
  public void setCurrentUserService(CurrentUserService currentUserService) {
    _currentUserService = currentUserService;
  }

  public void setUserId(int userId) {
    _userId = userId;
  }

  @Override
  public String execute() {

    User user = _userService.getUserForId(_userId);

    if (user == null)
      return INPUT;

    UserIndex currentUserIndex = _currentUserService.getCurrentUserAsUserIndex();
    if( currentUserIndex != null) {
      User currentUser = currentUserIndex.getUser();
      if( currentUser.equals(user))
        return ERROR;
    }
    _userService.deleteUser(user);

    return SUCCESS;
  }
}
