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
package org.onebusaway.phone.templates.settings;

import com.opensymphony.xwork2.ActionContext;

import org.onebusaway.phone.templates.Messages;
import org.onebusaway.probablecalls.AbstractIvrTemplate;
import org.onebusaway.probablecalls.IvrActionName;
import org.onebusaway.probablecalls.agitemplates.IvrTemplateId;

@IvrTemplateId("/settings/setDefaultSearchLocation")
public class SetDefaultSearchLocationTemplate extends AbstractIvrTemplate {

  @Override
  public void buildTemplate(ActionContext context) {
    addMessage(Messages.SET_DEFAULT_LOCATION);
    IvrActionName action = setNextAction("/back");
    action.putParam("count", 2);
  }
}
