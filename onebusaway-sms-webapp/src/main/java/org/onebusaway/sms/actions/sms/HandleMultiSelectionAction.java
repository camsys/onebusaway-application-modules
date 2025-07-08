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

import org.onebusaway.transit_data.model.StopBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandleMultiSelectionAction extends AbstractTextmarksAction {

  private static final long serialVersionUID = 1L;

  private int _selectedIndex;

  private String _stopId;

  private int _numberOfStops = Integer.MAX_VALUE;

  public int getSelectedIndex() {
    return _selectedIndex;
  }

  private List<StopBean> _stops;

  @Override
  public String execute() {

    if (_text == null || _text.length() == 0)
      return INPUT;

    if (_text.startsWith("#")) {
      clearNextActions();
      return "command";
    }

    if(_session.get("numberOfStops") != null) {
      _numberOfStops = (Integer) _session.get("numberOfStops");
    }

    try {
      _selectedIndex = Integer.parseInt(_text) - 1;
    } catch (NumberFormatException ex) {
      return INPUT;
    }

    if (_selectedIndex == -1)
      return "cancel";

    if(_selectedIndex >= getNumberOfStops()){
      return "cancel";
    }


    Map<String, String[]> params = new HashMap<>();
    _session.put("selectedIndex", new String[]{String.valueOf(_selectedIndex)});
    pushNextAction("stop-by-number");


    return getNextActionOrSuccess().getAction();
  }

  public int getNumberOfStops() {
    return _numberOfStops;
  }

  public void setNumberOfStops(int numberOfStops) {
    _numberOfStops = numberOfStops;
  }

  public void setStopId(String stopId) {
    _stopId = stopId;
  }

  private String getStopId() {
    return _stopId;
  }

}
