package org.onebusaway.twilio.actions.stops;

import java.util.Arrays;
import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.SearchQueryBean.EQueryType;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Results({
  @Result(name="success", location="arrivals-and-departures-for-stop-id", type="chain")
})
public class StopForCodeAction extends TwilioSupport {
  
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
    _log.info("in stop for code");
    CoordinateBounds bounds = getDefaultSearchArea();
    if (bounds == null)
      return NEEDS_DEFAULT_SEARCH_LOCATION;

    if (_stopCode == null || _stopCode.length() == 0)
      return INPUT;

    _log.info("searching on stopCode=" + _stopCode);
    SearchQueryBean searchQuery = new SearchQueryBean();
    searchQuery.setBounds(bounds);
    searchQuery.setMaxCount(5);
    searchQuery.setType(EQueryType.BOUNDS_OR_CLOSEST);
    searchQuery.setQuery(_stopCode);

    StopsBean stopsBean = _transitDataService.getStops(searchQuery);

    _stops = stopsBean.getStops();

    logUserInteraction("query", _stopCode);

    if (_stops.size() == 0) {
      return "noStopsFound";
    } else if (_stops.size() == 1) {
      StopBean stop = _stops.get(0);
      _stopIds = Arrays.asList(stop.getId());
      return SUCCESS;
    } else {
      return "multipleStopsFound";
    }
  }
}
