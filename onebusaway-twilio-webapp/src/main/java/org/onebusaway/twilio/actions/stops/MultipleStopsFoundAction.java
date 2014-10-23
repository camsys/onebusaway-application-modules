package org.onebusaway.twilio.actions.stops;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.presentation.services.text.TextModification;
import org.onebusaway.transit_data.model.StopBean;
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
import org.onebusaway.twilio.actions.Messages;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.ValueStack;

@Results({
	  @Result(name="back", location="index", type="chain"),
	  @Result(name="stop-selected",  location="arrivals-and-departures-for-stop-id", type="chain"),
    @Result(name="repeat", location="multiple-stops-found", type="chain")
})
public class MultipleStopsFoundAction extends TwilioSupport {

  private TextModification _destinationPronunciation;
	private static Logger _log = LoggerFactory.getLogger(MultipleStopsFoundAction.class);
  private List<String> _stopIds;
	private List<StopBean> stops;
	private StopBean stop;

  @Autowired
  public void setDestinationPronunciation(
      @Qualifier("destinationPronunciation") TextModification destinationPronunciation) {
    _destinationPronunciation = destinationPronunciation;
  }

  public List<String> getStopIds() {
    return _stopIds;
  }

  @Override
  public String execute() throws Exception {

	ActionContext context = ActionContext.getContext();
		Integer navState = (Integer)sessionMap.get("navState");
		if (navState == null) {
			navState = DISPLAY_DATA;
		}
		_log.debug("MultipleStopsFound, navState: " + navState);
		
		if (navState == DISPLAY_DATA) {
		
		  stops = (List<StopBean>) sessionMap.get("stops");
      _log.debug("stops: " + stops);
      
      int index = 1;
      
      addMessage(Messages.MULTIPLE_STOPS_WERE_FOUND);
      
			// Keep a map of key options and their corresponding stop beans
			Map<Integer, StopBean> keyMapping = new HashMap<Integer, StopBean>();
      for( StopBean stop : stops) {
        
        addMessage(Messages.FOR);
        
        String destination = _destinationPronunciation.modify(stop.getName());
        destination = destination.replaceAll("\\&", "and");      
        addText(destination);
        addText(", ");
        
        addMessage(Messages.PLEASE_PRESS);
        
        String key = Integer.toString(index++);
        addText(key);
        addText(". ");
        
        keyMapping.put(new Integer(index-1), stop);
      }
  
      addMessage(Messages.HOW_TO_GO_BACK);
      //addAction("\\*", "/back");
  
      addMessage(Messages.TO_REPEAT);
			sessionMap.put("keyMapping", keyMapping);
			sessionMap.put("stops", stops);
			
			navState = DO_ROUTING;
			sessionMap.put("navState", navState);
    }  else {	// Do the routing, matching the key pressed with the correct stop bean.
			_log.debug("Handling selection of choice of stops.");
			
			navState = DISPLAY_DATA;
			sessionMap.put("navState", navState);
			sessionMap.put("stop", stop);
			//setNextAction("search/multiple-stops-found");
			
			// Handle "back" request ('*' key pressed)
			if (PREVIOUS_MENU_ITEM.equals(getInput())) {
				return "back";
			}
			if (REPEAT_MENU_ITEM.equals(getInput())) {
        return "repeat";
      }

			int key = Integer.parseInt(getInput());
			Map<Integer, StopBean> keyMapping = (Map<Integer, StopBean>)sessionMap.get("keyMapping");
			stop = (StopBean)keyMapping.get(key);
			sessionMap.put("stop", stop);

			//_log.debug("Key " + key + " entered for stop: " + stop.getId());
      _stopIds = Arrays.asList(stop.getId());
			return "stop-selected";
		}

    
    return SUCCESS;
  }}
