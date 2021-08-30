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
/**
 * 
 */
package org.onebusaway.presentation.model;

import org.apache.struts2.dispatcher.Parameter;

import java.util.HashMap;
import java.util.Map;

public class NextAction {

  private final String _action;

  private final Map<String, Parameter> _parameters;

  public NextAction(String action) {
    this(action, new HashMap<>());
  }

  public NextAction(String action, Parameter parameter) {
    this(action, getMap(parameter.getName(), parameter));
  }

  public NextAction(String action, Map<String, Parameter> parameters) {
    _action = action;
    _parameters = parameters;
  }

  public String getAction() {
    return _action;
  }

  public Map<String, Parameter> getParameters() {
    return _parameters;
  }

  private static Map<String, Parameter> getMap(String key, Parameter value) {
    Map<String, Parameter> params = new HashMap<>();
    params.put(key, value);
    return params;
  }

  @Override
  public String toString() {
    return "NextAction(action=" + _action + " params=" + _parameters+")";
  }  
}