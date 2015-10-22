/**
 * Copyright (C) 2015 Cambridge Systematics
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
package org.onebusaway.nextbus.actions.api;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nextbus.model.nextbus.Body;
import org.onebusaway.nextbus.model.nextbus.BodyError;
import org.onebusaway.nextbus.model.transiTime.Prediction;
import org.onebusaway.nextbus.model.transiTime.Predictions;
import org.onebusaway.nextbus.model.transiTime.PredictionsDirection;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.opensymphony.xwork2.ModelDriven;

public class PredictionsAction extends NextBusApiBase implements
    ModelDriven<Body<Predictions>> {

  private static Logger _log = LoggerFactory.getLogger(PredictionsAction.class);

  private String agencyId;

  private String stopId;
  
  private String rTag;

  private String stopTag;

  private String routeTag;

  public String getA() {
    return agencyId;
  }

  public void setA(String agencyId) {
    this.agencyId = getMappedAgency(agencyId);
  }

  public String getStopId() {
    return stopId;
  }

  public void setStopId(String stopId) {
    this.stopId = _tdsMappingService.getStopIdFromStopCode(stopId);
  }

  public String getS() {
    return stopTag;
  }

  public void setS(String stopTag) {
    this.stopTag = stopTag;
  }
  
  public String getR() {
    return stopTag;
  }

  public void setR(String rTag) {
    this.rTag = rTag;
  }

  public String getRouteTag() {
    return routeTag;
  }

  public void setRouteTag(String routeTag) {
    this.routeTag = _tdsMappingService.getRouteIdFromShortName(routeTag);
  }

  public DefaultHttpHeaders index() {
    return new DefaultHttpHeaders("success");
  }

  public Body<Predictions> getModel() {

    Body<Predictions> body = new Body<Predictions>();
    List<AgencyAndId> stopIds = new ArrayList<AgencyAndId>();
    List<AgencyAndId> routeIds = new ArrayList<AgencyAndId>();

    if (isValid(body, stopIds, routeIds)) {

      String serviceUrl = getServiceUrl() + agencyId + PREDICTIONS_COMMAND
          + "?";

      String routeStop = "";

      for (AgencyAndId routeId : routeIds) {
        routeStop += "rs=" + getIdNoAgency(routeId.toString()) + "|"
            + getIdNoAgency(stopId) + "&";
      }
      String uri = serviceUrl + routeStop + "format=" + REQUEST_TYPE;

      try {

        JsonArray predictionsJson = getJsonObject(uri).getAsJsonArray(
            "predictions");
        Type listType = new TypeToken<List<Predictions>>() {
        }.getType();

        List<Predictions> predictions = new Gson().fromJson(predictionsJson,
            listType);

        modifyJSONObject(predictions);

        body.getResponse().addAll(predictions);

      } catch (Exception e) {
        body.getErrors().add(new BodyError("No valid results found."));
        _log.error(e.getMessage());
      }
    }

    return body;

  }

  private void modifyJSONObject(List<Predictions> predictions) {
    AgencyBean agencyBean = _transitDataService.getAgency(agencyId);
    for (Predictions prediction : predictions) {
      prediction.setAgencyTitle(agencyBean.getName());
      for (PredictionsDirection direction : prediction.getDest()) {
        for (Prediction dirPrediction : direction.getPred()) {
          dirPrediction.setDirTag(direction.getDir());
        }
      }
    }
  }

  private boolean isValid(Body body, List<AgencyAndId> stopIds,
      List<AgencyAndId> routeIds) {
    if (!isValidAgency(body, agencyId))
      return false;

    List<String> agencies = new ArrayList<String>();
    agencies.add(agencyId);
    
    boolean usingStopId = false;
    boolean usingStopTag = false;
    
    if (StringUtils.isNotBlank(stopId)) {
      if (!processStopIds(stopId, stopIds, agencies, body))
        return false;
      else
        usingStopId = true;
    }
    else if(StringUtils.isNotBlank(stopTag)){
      if (!processStopIds(stopTag, stopIds, agencies, body))
        return false;
      else
        usingStopTag = true;
    }
    else{
      body.getErrors().add(new BodyError("route parameter \"r\" must be specified in query string"));
      return false;
    }

    StopBean stopBean = _transitDataService.getStop(stopIds.get(0).toString());

    if (routeTag == null && usingStopId) {
      for (RouteBean routeBean : stopBean.getRoutes()) {
        routeIds.add(AgencyAndId.convertFromString(routeBean.getId()));
      }
    } else {
      if (usingStopId && !processRouteIds(routeTag, routeIds, agencies, body))
        return false;
      
      if(usingStopTag && !processRouteIds(rTag, routeIds, agencies, body))
        return false;

      boolean stopServesRoute = false;
      for (RouteBean routeBean : stopBean.getRoutes()) {
        if (routeIds.contains(AgencyAndId.convertFromString(routeBean.getId())))
          stopServesRoute = true;
      }
      if (!stopServesRoute) {
        body.getErrors().add(
            new BodyError(
                "For agency="
                    + agencyId
                    + " route r="
                    + routeTag
                    + " is not currently available. It might be initializing still."));
        return false;
      }

    }

    return true;
  }
}
