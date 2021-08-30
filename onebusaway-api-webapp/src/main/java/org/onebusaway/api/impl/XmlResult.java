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
package org.onebusaway.api.impl;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionInvocation;
import org.apache.struts2.result.StrutsResultSupport;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.result.StrutsResultSupport;

public class XmlResult extends StrutsResultSupport {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doExecute(String finalLocation, ActionInvocation invocation)
      throws Exception {

    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();
    RequestDispatcher dispatcher = request.getRequestDispatcher(finalLocation);
    response.setContentType("text/xml");
    dispatcher.include(request, response);
  }

}
