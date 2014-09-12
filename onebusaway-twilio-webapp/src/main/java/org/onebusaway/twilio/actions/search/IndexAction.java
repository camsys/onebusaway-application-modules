package org.onebusaway.twilio.actions.search;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.onebusaway.twilio.actions.search.IndexAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Results({
	  @Result(name="back", location="index", type="redirectAction"),
	  @Result(name="route-for-name", location="route-for-name", type="chain"),
	  @Result(name="search-for-code", location="stop-for-code", type="chain")
	  })
public class IndexAction extends TwilioSupport {

	  private static final long serialVersionUID = 1L;
	  private static Logger _log = LoggerFactory.getLogger(IndexAction.class);
	  private String _searchCode;
	  
	  public String getSearchCode() {
	    return _searchCode;
	  }
	  
	  public void setSearchCode(String searchCode) {
		  _searchCode = searchCode;
	  }
	  
	  @Override
	  public String execute() throws Exception {
	    _log.debug("in search index with input=" + getInput());
	    
	    if (getInput() != null) {
	      if ("8".equals(getInput())) {
	      	clearInput();
	        return "back";
	      }
	      
	      //addActionWithParameterFromMatch("([1-9][0-9]*)#", "/search/route", "routeName", 1);
	      setSearchCode(getInput());
	      clearInput();
	      _log.debug("search.IndexAction:searchCode: " + _searchCode);
	      if (_searchCode.matches("([1-9][0-9]*)")) {
	    	  return "route-for-name";
	      }
	      
	      //_log.debug("forwarding to search");
	      return "search-for-code";
	    } else {
	      setNextAction("search/index");
	    }
	    
	    return INPUT;
	  }
}
