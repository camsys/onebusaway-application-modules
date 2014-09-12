package org.onebusaway.twilio.actions.search;

import java.util.Arrays;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.presentation.services.text.TextModification;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.twilio.actions.Messages;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.ValueStack;

//@Results({
//	  @Result(name="success", location="stops-for-route-navigation", type="chain")
//})
public class StopFoundAction extends TwilioSupport {
	private static final long serialVersionUID = 1L;
	private static Logger _log = LoggerFactory.getLogger(IndexAction.class);
	
	private TextModification _destinationPronunciation;

	private TextModification _directionPronunciation;

	@Autowired
	public void setDestinationPronunciation(
		@Qualifier("destinationPronunciation") TextModification destinationPronunciation) {
		_destinationPronunciation = destinationPronunciation;
	}

	@Autowired
	public void setDirectionPronunciation(
		@Qualifier("directionPronunciation") TextModification directionPronunciation) {
		_directionPronunciation = directionPronunciation;
	}

	public String execute() throws Exception {

		ActionContext context = ActionContext.getContext();
	    ValueStack vs = context.getValueStack();

	    StopBean stop = (StopBean) vs.findValue("stop");

	    addMessage(Messages.THE_STOP_NUMBER_FOR);

	    addText(_destinationPronunciation.modify(stop.getName()));

	    String direction = _directionPronunciation.modify(stop.getDirection());
	    addMessage(Messages.DIRECTION_BOUND, direction);

	    addText(Messages.IS);
	    addText(stop.getCode());

	    addMessage(Messages.STOP_FOUND_ARRIVAL_INFO);
	    //AgiActionName arrivalInfoAction = addAction("1", "/stop/arrivalsAndDeparturesForStopId");
	    //arrivalInfoAction.putParam("stopIds", Arrays.asList(stop.getId()));

	    addMessage(Messages.STOP_FOUND_BOOKMARK_THIS_LOCATION);
	    //AgiActionName bookmarkAction = addAction("2", "/stop/bookmark");
	    //bookmarkAction.putParam("stop", stop);

	    addMessage(Messages.STOP_FOUND_RETURN_TO_MAIN_MENU);
	    //addAction("3", "/index");

	    //addAction("[04-9]", "/repeat");

	    addMessage(Messages.HOW_TO_GO_BACK);
	    //addAction("\\*", "/back");

	    addMessage(Messages.TO_REPEAT);
		  
		return SUCCESS;
	}
}
