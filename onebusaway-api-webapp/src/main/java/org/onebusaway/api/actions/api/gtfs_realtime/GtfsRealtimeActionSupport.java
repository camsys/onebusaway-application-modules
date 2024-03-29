/**
 * Copyright (C) 2013 Google, Inc.
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
package org.onebusaway.api.actions.api.gtfs_realtime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.realtime.GtfsRealtimeConstantsV2;
import org.onebusaway.api.services.AgencyAndIdModificationStrategy;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.util.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.opensymphony.xwork2.conversion.annotations.TypeConversion;
import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;

import javax.servlet.http.HttpServletResponse;

public abstract class GtfsRealtimeActionSupport extends ApiActionSupport {

  private static final long serialVersionUID = 1L;
  private static Logger _log = LoggerFactory.getLogger(GtfsRealtimeActionSupport.class);

  enum FILTER_TYPE {
    UNFILTERED,
    ROUTE_ID
  }

  private static final int V2 = 2;

  @Autowired
  protected TransitDataService _service;

  @Autowired(required = false)
  protected AgencyAndIdModificationStrategy _stopIdModificationStrategy;

  private String _agencyId;

  private long _time;

  private String _routeFilterId = null;

  private boolean _removeAgencyIds = true;

  private static final SimpleDateFormat _sdf;

  static {
    _sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT' ", Locale.US);
    _sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public GtfsRealtimeActionSupport() {
    super(V2);
  }
  
  public void setTransitDataService(TransitDataService service) {
    _service = service;
  }

  @RequiredFieldValidator
  public void setId(String id) {
    _agencyId = id;
  }

  public String getId() {
    return _agencyId;
  }

  public void setRouteFilterId(String routeId) { _routeFilterId = routeId; }

  public String getRouteFilterId() { return _routeFilterId; }

  @TypeConversion(converter = "org.onebusaway.presentation.impl.conversion.DateTimeConverter")
  public void setTime(Date time) {
    _time = time.getTime();
  }

  public void setRemoveAgencyIds(boolean removeAgencyIds) {
    _removeAgencyIds = removeAgencyIds;
  }

  public DefaultHttpHeaders show() throws ServiceException {
    if (!isVersion(V2))
      return setUnknownVersionResponse();

    if (hasErrors())
      return setValidationErrorsResponse();

    long time = SystemTime.currentTimeMillis();
    if (_time != 0)
      time = _time;

    FeedMessage.Builder feed = FeedMessage.newBuilder();
    FeedHeader.Builder header = feed.getHeaderBuilder();
    header.setGtfsRealtimeVersion(GtfsRealtimeConstantsV2.VERSION);
    header.setTimestamp(time / 1000);
    if (getRouteFilterId() != null) {
      fillFeedMessage(feed, _agencyId, time, FILTER_TYPE.ROUTE_ID, getRouteFilterId());
    } else {
      fillFeedMessage(feed, _agencyId, time, FILTER_TYPE.UNFILTERED, null);
    }

    try {
      return setOkResponse(feed.build());
    } catch (Throwable t) {
      _log.error("exception constructing GTFS-RT:", t, t);
      return setExceptionResponse();
    }
  }

  protected abstract void fillFeedMessage(FeedMessage.Builder feed,
      String agencyId, long timestamp, FILTER_TYPE filterType, String filterValue);

  protected void setLastModifiedHeader(long feedTimestamp){
    if(feedTimestamp != 0){
      long lastModifiedMills = feedTimestamp * 1000L;
      String lastModifiedHeader = _sdf.format(new Date(lastModifiedMills));
      HttpServletResponse response = ServletActionContext.getResponse();
      response.setHeader("Last-Modified", lastModifiedHeader);
    }

  }

  protected String normalizeId(String id) {
    if (_removeAgencyIds) {
      int index = id.indexOf('_');
      if (index != -1) {
        id = id.substring(index + 1);
      }
    }
    return id;
  }

  protected AgencyAndId modifiedStopId(String agency, String stopId) {
    AgencyAndId id = AgencyAndId.convertFromString(stopId);
    if (_stopIdModificationStrategy == null)
      return id;
    AgencyAndId newId = _stopIdModificationStrategy.convertId(agency, id);
    if (newId != null)
      id = newId;
    return id;
  }
}
