package org.onebusaway.webapp.actions.admin.vehiclepredictions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.gtfs_realtime.library.GtfsRealtimeConversionLibrary;
import org.onebusaway.gtfs_realtime.model.StopTimeUpdateModel;
import org.onebusaway.gtfs_realtime.model.TripUpdateModel;
import org.onebusaway.util.services.configuration.ConfigurationService;
import org.onebusaway.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.ExtensionRegistry;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtimeOneBusAway;

@Namespace(value="/admin/vehiclepredictions")
@Result(name = "success",
  type = "json",
  params = {
      "root", "jsonString"
  })
public class GtfsRtProxyAction extends OneBusAwayNYCAdminActionSupport {

  private Logger _log = LoggerFactory.getLogger(GtfsRtProxyAction.class);
  
//  private static final ExtensionRegistry _registry = ExtensionRegistry.newInstance();
//
//  static {
//    _registry.add(GtfsRealtimeOneBusAway.obaFeedEntity);
//    _registry.add(GtfsRealtimeOneBusAway.obaTripUpdate);
//  }
  
 // @Autowired
  //private ConfigurationService _configurationService;

  private String type = "text/plain";
  private InputStream stream;
  String jsonString;

  public String execute() {
    CloseableHttpClient httpclient = HttpClients.createDefault();
    try {
      HttpGet request = new HttpGet(getGtfsRtPath());
      CloseableHttpResponse response = httpclient.execute(request);
      InputStream is = response.getEntity().getContent();
      FeedMessage message = FeedMessage.parseFrom(is/*, _registry*/);
      List<TripUpdateModel> updates = GtfsRealtimeConversionLibrary.readTripUpdates(message);
      
      for (TripUpdateModel tu : updates) {
        for (StopTimeUpdateModel stu : tu.getStopTimeUpdates()) {
          stu.setTripUpdateModel(null);
        }
      }
      
      Gson gson = new Gson();
      jsonString = gson.toJson(updates);
     
      
      return SUCCESS;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return ERROR;
  }


  private String getGtfsRtPath() {
    //return _configurationService.getConfigurationValueAsString("admin.link.service.path", "/services/tss_lab/GetOnScheduleTrains?TimeInterval=5");
//    return "http://admin.staging.obast.org:9999/sc/trip-updates";
    return "http://localhost:9998/sc/trip-updates";
  }

  public String getType() {
    return this.type;
  }
  
  public InputStream getStream() {
    return this.stream;
  }
  
  public String getJsonString() {
    return jsonString;
  }
  
 
}
