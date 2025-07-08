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
package org.onebusaway.sms.actions.sms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.SearchQueryBean.EQueryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class StopByNumberAction extends AbstractTextmarksAction {

  private static final long serialVersionUID = 1L;

  private ServiceAreaService _serviceAreaService;

  private String _stopQuery;

  private List<StopBean> _stops;

  private int _selectedIndex = -1;

  private String[] _args;

  private String _stopId;

  @Qualifier("BasicServiceAreaService")
  @Autowired
  public void setServiceAreaService(ServiceAreaService serviceAreaService) {
    _serviceAreaService = serviceAreaService;
  }

  public String getStopQuery() {
    return _stopQuery;
  }

  public List<StopBean> getStops() {
    return _stops;
  }

  public void setSelectedIndex(int selectedIndex) {
    _selectedIndex = selectedIndex;
  }

  public String getStopId() {
    return _stopId;
  }

  public void setStopId(String stopId) {
    _stopId = stopId;
  }


  public String[] getArgs() {
    return _args;
  }

  @Override
  public String execute() throws ServiceException {
    String input = cleanUpInput(_text);
    boolean invalidInput = isInputInvalid(input);

    if (invalidInput) {
      return INPUT;
    }

    // Check Service Area
    CoordinateBounds serviceArea = _serviceAreaService.getServiceArea();
    if (serviceArea == null) {
      pushNextAction("stop-by-number", "text", _text);
      return "query-default-search-location";
    }

    String[] inputs = getAllInputs(input);

    String stopId = processStopId(inputs);

    _stopQuery = stopId;
    SearchQueryBean searchQuery = new SearchQueryBean();
    searchQuery.setBounds(serviceArea);
    searchQuery.setMaxCount(5);
    searchQuery.setType(EQueryType.BOUNDS_OR_CLOSEST);
    searchQuery.setQuery(_stopQuery);

    StopsBean results = _transitDataService.getStops(searchQuery);

    _stops = results.getStops();

    int stopIndex = 0;

    if (_stops.isEmpty()) {
      return "noStopsFound";
    } else if (_stops.size() > 1) {
      if (0 <= _selectedIndex && _selectedIndex < _stops.size()) {
        stopIndex = _selectedIndex;
      } else {
        Map<String, String[]> params = new HashMap<>();
        _session.put("numberOfStops",_stops.size());
        _session.put("stopId", stopId);
        pushNextAction("handle-multi-selection");
        return "multipleStopsFound";
      }
    }

    _session.remove("numberOfStops");
    _session.remove("stopId");


    StopBean stop = _stops.get(stopIndex);
    _stopId = stop.getId();


    _args = new String[inputs.length - 1];
    System.arraycopy(inputs, 1, _args, 0, _args.length);


    return "arrivals-and-departures";
  }

  private String processStopId(String[] inputs) {
    Object sessionStopId = _session.get("stopId");
    if(sessionStopId != null) {
      String stopId = (String) sessionStopId;
      _selectedIndex = Math.max(Integer.parseInt(inputs[0]) - 1, 0);
      return stopId;
    }
    return inputs[0];
  }

  private String[] getAllInputs(String input) {
    return input.trim().split("\\s+");
  }

  private boolean isInputInvalid(String input) {
    return _text == null || _text.length() == 0 || input.isEmpty();
  }

  private String cleanUpInput(String text) {
    if (text != null)
      return text.trim();
    return text;
  }
}
