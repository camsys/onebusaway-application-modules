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
package org.onebusaway.phone.actions.stops;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.phone.actions.AbstractAction;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.SearchQueryBean.EQueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionChainResult;

public class StopForCodeAction extends AbstractAction {

  private static final long serialVersionUID = 1L;
  private static Logger _log = LoggerFactory.getLogger(StopForCodeAction.class);
  
  private String _stopCode;

  private List<String> _stopIds;

  private List<StopBean> _stops;

  public void setStopCode(String stopCode) {
    _stopCode = stopCode;
  }

  public List<String> getStopIds() {
    return _stopIds;
  }

  public List<StopBean> getStops() {
    return _stops;
  }

  public String execute() throws Exception {

    CoordinateBounds bounds = getDefaultSearchArea();
    if (bounds == null)
      return NEEDS_DEFAULT_SEARCH_LOCATION;

    if (_stopCode == null || _stopCode.length() == 0) {
      _log.error("missing required input stopCode");
      return INPUT;
    }

    _log.debug("searching with stopCode=" + _stopCode);
    SearchQueryBean searchQuery = new SearchQueryBean();
    searchQuery.setBounds(bounds);
    searchQuery.setMaxCount(5);
    searchQuery.setType(EQueryType.BOUNDS_OR_CLOSEST);
    searchQuery.setQuery(_stopCode);

    StopsBean stopsBean = _transitDataService.getStops(searchQuery);

    _stops = stopsBean.getStops();

    logUserInteraction("query", _stopCode);

    if (_stops.size() == 0) {
      _log.debug("no stop found");
      return "noStopsFound";
    } else if (_stops.size() == 1) {
      StopBean stop = _stops.get(0);
      _stopIds = Arrays.asList(stop.getId());
      _log.debug("found one stop = " + _stopIds);
      
      LinkedList<? extends String> chainHistory = ActionChainResult.getChainHistory();
      for (String history : chainHistory) {
        _log.debug("chain: " + history);
      }
      if (chainHistory.contains(makeKey("/", "/stop/arrivalsAndDeparturesForStopId", null))) {
        _log.debug("cannot chain, result already present");
        return null;
      }
      
      return SUCCESS;
    } else {
      _log.error("found multiple stops");
      return "multipleStopsFound";
    }
  }
  
  private String makeKey(String namespace, String actionName, String methodName) {
    if (null == methodName) {
        String key = namespace + "/" + actionName;
        _log.error("makeKey returning " + key);
        return key;
    }

    return namespace + "/" + actionName + "!" + methodName;
}
}
