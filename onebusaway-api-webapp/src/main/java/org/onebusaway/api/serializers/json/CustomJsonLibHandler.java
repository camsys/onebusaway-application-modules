package org.onebusaway.api.serializers.json;

import com.fasterxml.jackson.databind.*;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.rest.handler.AbstractContentTypeHandler;
import org.apache.struts2.rest.handler.ContentTypeHandler;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;


public class CustomJsonLibHandler implements ContentTypeHandler {

  @Deprecated
  @Override
  public void toObject(Reader in, Object target) throws IOException {
    return;
  }

  @Override
  public void toObject(ActionInvocation actionInvocation, Reader in, Object target) throws IOException {
    StringBuilder sb = new StringBuilder();
    char[] buffer = new char[1024];
    int len = 0;
    while ((len = in.read(buffer)) > 0) {
      sb.append(buffer, 0, len);
    }
    if (target != null && sb.length() > 0 && sb.charAt(0) == '[') {
      JSONArray jsonArray = JSONArray.fromObject(sb.toString());
      if (target.getClass().isArray()) {
        JSONArray.toArray(jsonArray, target, new JsonConfig());
      } else {
        JSONArray.toList(jsonArray, target, new JsonConfig());
      }

    } else {
      JSONObject jsonObject = JSONObject.fromObject(sb.toString());
      JSONObject.toBean(jsonObject, target, new JsonConfig());
    }
  }


  public String fromObject(Object obj, String resultCode, Writer stream, String callback)
          throws IOException {

    String value = null;

    if (obj != null) {
      if (isArray(obj)) {
        JSONArray jsonArray = JSONArray.fromObject(obj);
        value = jsonArray.toString();
      } else {
        JSONObject jsonObject = JSONObject.fromObject(obj);
        value = jsonObject.toString();
      }
    }

    if (value != null) {
      if (callback != null)
        stream.write(callback + "(" + value + ")");
      else
        stream.write(value);
    }

    return null;
  }

  @Deprecated
  @Override
  public String fromObject(Object obj, String resultCode, Writer stream)
          throws IOException {
    return null;

  }

  @Override
  public String fromObject(ActionInvocation actionInvocation, Object obj, String resultCode, Writer stream) throws IOException {
    String callback = getCallback();
    return fromObject(obj, resultCode, stream, callback);
  }

  private boolean isArray(Object obj) {
    return obj instanceof Collection<?> || obj.getClass().isArray();
  }

  public String getContentType() {
    String callback = getCallback();
    if(callback != null){
      return ("application/javascript");
    }
    return "application/json";
  }

  private String getCallback(){
    String callback = null;
    HttpServletRequest req = ServletActionContext.getRequest();
    if (req != null) {
      callback = req.getParameter("callback");
    }
    return callback;
  }

  public String getExtension() {
    return "json";
  }
}