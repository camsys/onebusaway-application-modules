package org.onebusaway.twilio.actions;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Results({
  @Result(name="stopsIndex", location="stops/index", type="redirectAction")
})
public class IndexAction extends TwilioSupport {

  private static Logger _log = LoggerFactory.getLogger(IndexAction.class);
  
  @Override
  public String execute() throws Exception {
    _log.info("in execute! with input=" + getInput());
    
    
    if ("3".equals(getInput())) {
      _log.error("redirecting");
      return "stopsIndex";
    }
    return INPUT;
  }

}