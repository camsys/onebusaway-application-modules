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
package org.onebusaway.presentation.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.components.Component;
import org.apache.struts2.views.jsp.ContextBeanTag;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.xwork2.util.ValueStack;

public class ResourcesUrlTag extends ContextBeanTag {

  private static final long serialVersionUID = 1L;
  
  private String _id;

  public Component getBean(ValueStack stack, HttpServletRequest req,
      HttpServletResponse res) {
    ResourcesUrlComponent component = new ResourcesUrlComponent(stack);
    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(pageContext.getServletContext());
    context.getAutowireCapableBeanFactory().autowireBean(component);
    return component;
  }
  
  public void setId(String id) {
    _id = id;
  }

  protected void populateParams() {
    super.populateParams();
    ResourcesUrlComponent tag = (ResourcesUrlComponent) getComponent();
    tag.setId(_id);
  }
}
