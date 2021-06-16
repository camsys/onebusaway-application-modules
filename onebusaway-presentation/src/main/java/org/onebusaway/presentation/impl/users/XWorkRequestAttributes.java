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
package org.onebusaway.presentation.impl.users;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.struts2.dispatcher.Parameter;
import org.springframework.web.context.request.AbstractRequestAttributes;

import com.opensymphony.xwork2.ActionContext;
import org.springframework.web.context.request.RequestAttributes;

public class XWorkRequestAttributes extends AbstractRequestAttributes {

  private ActionContext _context;

  private String _sessionId;

  public XWorkRequestAttributes(ActionContext context, String sessionId) {
    _context = context;
    _sessionId = sessionId;
  }

  @Override
  public String[] getAttributeNames(int scope) {
    Set<String> attributeNamesSet = getScopedAttributeNames(scope);
    String[] names = new String[attributeNamesSet.size()];
    int index = 0;
    for (String name : attributeNamesSet)
      names[index++] = name;
    return names;
  }

  @Override
  public Object getAttribute(String name, int scope) {
    if(scope == RequestAttributes.SCOPE_REQUEST){
      Parameter parameter = getRequestScope().get(name);
      return parameter != null ? parameter.getObject() : null;
    } else if(scope == RequestAttributes.SCOPE_SESSION){
      return getSessionScope().get(name);
    }
    throw new IllegalStateException("unknown scope=" + scope);
  }

  @Override
  public void removeAttribute(String name, int scope) {
    if(scope == RequestAttributes.SCOPE_REQUEST){
      getRequestScope().remove(name);
    } else if(scope == RequestAttributes.SCOPE_SESSION){
      getSessionScope().remove(name);
    }
    throw new IllegalStateException("unknown scope=" + scope);
  }

  @Override
  public void setAttribute(String name, Object value, int scope) {
    if(scope == RequestAttributes.SCOPE_REQUEST){
      getRequestScope().put(name, new Parameter.Request(name, value));
    } else if(scope == RequestAttributes.SCOPE_SESSION){
      getSessionScope().remove(name);
    }
    throw new IllegalStateException("unknown scope=" + scope);
  }

  @Override
  public String getSessionId() {
    return _sessionId;
  }

  @Override
  public Object getSessionMutex() {
    return getSessionScope();
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback,
      int scope) {

  }

  @Override
  public Object resolveReference(String key) {
    if (REFERENCE_REQUEST.equals(key)) {
      return getRequestScope();
    } else if (REFERENCE_SESSION.equals(key)) {
      return getSessionScope();
    } else {
      return null;
    }
  }

  /****
   * Private Methods
   ****/

  @Override
  protected void updateAccessedSessionAttributes() {

  }

  private Set<String> getScopedAttributeNames(int scope){
    switch (scope) {
      case RequestAttributes.SCOPE_REQUEST:
        return _context.getParameters().keySet();
      case RequestAttributes.SCOPE_SESSION:
        return _context.getSession().keySet();
      default:
        throw new IllegalStateException("unknown scope=" + scope);
    }
  }

  private Map<String, Parameter> getRequestScope(){
    return _context.getParameters();
  }

  private Map<String, Object> getSessionScope(){
    return _context.getSession();
  }

}
