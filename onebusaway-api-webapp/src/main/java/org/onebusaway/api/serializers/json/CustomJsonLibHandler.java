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
package org.onebusaway.api.serializers.json;

import com.fasterxml.jackson.databind.*;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import org.apache.struts2.rest.handler.AbstractContentTypeHandler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


public class CustomJsonLibHandler extends AbstractContentTypeHandler {

  private static final String DEFAULT_CONTENT_TYPE = "application/json";
  private String defaultEncoding = "ISO-8859-1";
  private ObjectMapper mapper = new ObjectMapper();


  public void toObject(ActionInvocation invocation, Reader in, Object target) throws IOException {
    this.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    ObjectReader or = this.mapper.readerForUpdating(target);
    or.readValue(in);
  }

  public String fromObject(ActionInvocation invocation, Object obj, String resultCode, Writer stream) throws IOException {

    this.mapper.setSerializerProvider(new CustomSerializerProvider());
    this.mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    this.mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    this.mapper.writeValue(stream, obj);

    return null;
  }

  public String getContentType() {
    return "application/json;charset=" + this.defaultEncoding;
  }

  public String getExtension() {
    return "json";
  }

  @Inject("struts.i18n.encoding")
  public void setDefaultEncoding(String val) {
    this.defaultEncoding = val;
  }
}