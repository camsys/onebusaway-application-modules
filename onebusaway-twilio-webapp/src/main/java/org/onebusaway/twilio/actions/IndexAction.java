package org.onebusaway.twilio.actions;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionContext;


@Results({
  @Result(name="stops-index", location="stops/index", type="redirectAction", params={"From", "${phoneNumber}"})
})
public class IndexAction extends TwilioSupport {

  private static final long serialVersionUID = 1L;
  private static Logger _log = LoggerFactory.getLogger(IndexAction.class);
  
  @Override
  public String execute() throws Exception {
    _log.debug("in execute! with input=" + getInput());
    
    
    if ("3".equals(getInput())) {
      clearNextAction();
      return "stops-index";
    } else {
      setNextAction("index");
    }
    return INPUT;
  }

}