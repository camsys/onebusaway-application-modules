/**
 * Copyright (C) 2015 Cambridge Systematics, Inc.
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
package org.onebusaway.realtime.soundtransit.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeMutableProvider;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeProviderImpl;
//import org.onebusaway.realtime.soundtransit.model.AVLRecord;
//import org.onebusaway.realtime.soundtransit.model.VehicleRecord;
//import org.onebusaway.realtime.soundtransit.sql.ResultSetMapper;
//import org.onebusaway.realtime.soundtransit.tds.AVLTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.ServletContextAware;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class SoundAvlToGtfsRealtimeService implements ServletContextAware {
  public static final String DB_URL = "url";
  public static final String QUERY_STRING = "select ID as Id, BusID as busId, RptTime as reportTime, "
      + "LatDD as lat, LonDD as lon, LogonRoute as logonRoute, LogonTrip as logonTrip, "
      + "BusNum as busNumber, RptDate as reportDate from tblbuses;";

  private static final Logger _log = LoggerFactory.getLogger(SoundAvlToGtfsRealtimeService.class);
  private GtfsRealtimeMutableProvider _gtfsRealtimeProvider;
  private ScheduledExecutorService _refreshExecutor;
  private ScheduledExecutorService _delayExecutor;
  private FeedService _feedService;

  private String _url = null;
  private int _refreshInterval = 60;

  public void setRefreshInterval(int interval) {
    _refreshInterval = interval;
  }

  public void setGtfsRealtimeProvider(GtfsRealtimeMutableProvider p) {
    _gtfsRealtimeProvider = p;
  }

  public void setConnectionUrl(String jdbcConnectionString) {
    _url = jdbcConnectionString;
  }

  @Autowired
  public void setFeedService(FeedService feedService) {
    _feedService = feedService;
  }

  @PostConstruct
  public void start() throws Exception {
    _log.info("starting GTFS-realtime service");
    if (_gtfsRealtimeProvider != null) {
      _refreshExecutor = Executors.newSingleThreadScheduledExecutor();
      _refreshExecutor.scheduleAtFixedRate(new RefreshTransitData(), 0,
          _refreshInterval, TimeUnit.SECONDS);

      _delayExecutor = Executors.newSingleThreadScheduledExecutor();
      _delayExecutor.scheduleAtFixedRate(new DelayThread(), _refreshInterval,
          _refreshInterval / 4, TimeUnit.SECONDS);
    } else {
      _log.error("Testing mode.  RefreshInterval ignored!");

      new Thread(new RefreshTransitData()).run();

    }
  }

  @PreDestroy
  public void stop() {
    _log.info("stopping GTFS-realtime service");
    if (_refreshExecutor != null) {
      _refreshExecutor.shutdownNow();
    }
  }

  // package private for unit tests
  Map<String, String> getConnectionProperties() {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(DB_URL, _url);
    return properties;
  }

  // package private for unit tests
  Connection getConnection(Map<String, String> properties) throws Exception {
    return DriverManager.getConnection(properties.get(DB_URL));
  }

  // void writeGtfsRealtimeOutput(List<VehicleRecord> records) {
  void writeGtfsRealtimeOutput(String dataFromAvl) {
    Map<String, Object> parsedAvlUpdates = _feedService.feedToJson(dataFromAvl);
    FeedMessage vehiclePositionsFM = _feedService.buildVPMessage(parsedAvlUpdates);

    FeedMessage tripUpdatesFM = _feedService.buildTUMessage(parsedAvlUpdates);

    if (_gtfsRealtimeProvider != null) {
      _gtfsRealtimeProvider.setTripUpdates(tripUpdatesFM);
      // update metrics
      _gtfsRealtimeProvider.setLastUpdateTimestamp(System.currentTimeMillis());
    }
    if (_gtfsRealtimeProvider != null)
      _gtfsRealtimeProvider.setVehiclePositions(vehiclePositionsFM);
  }

  public void writeGtfsRealtimeOutput() throws Exception {
    //TODO: move url to config file
    URL avlUpdatesUrl = new URL(
        "http://localhost:9764/services/tss_lab/GetOnScheduleTrains?TimeInterval=5");
    String dataFromAvl = readAvlUpdatesFromUrl(avlUpdatesUrl);
    _log.info("AVL: " + dataFromAvl);
    writeGtfsRealtimeOutput(dataFromAvl);
  }

  public void setServletContext(ServletContext context) {
    if (context != null) {
      String url = context.getInitParameter("soundtransit.jdbc");
      if (url != null) {
        _log.info("init with connection info: " + url);
        this.setConnectionUrl(url);
      } else {
        _log.warn("missing expected init param: soundtransit.jdbc");
      }
    }
  }

  private class RefreshTransitData implements Runnable {
    public void run() {
      try {
        _log.info("refreshing vehicles");
        writeGtfsRealtimeOutput();
      } catch (Exception ex) {
        _log.error("Failed to refresh TransitData: " + ex.getMessage());
        _log.error(ex.toString(), ex);
      }
    }
  }
  private class DelayThread implements Runnable {
    public void run() {
      long hangTime = (System.currentTimeMillis() - _gtfsRealtimeProvider.getLastUpdateTimestamp()) / 1000;
      if (hangTime > (_refreshInterval * 6)) {
        // if we've reached here, the connection to the database has hung
        // we assume a service-based configuration and simply exit
        // TODO adjust network/driver timeouts instead!
        _log.error("Connection hung with delay of " + hangTime + ".  Exiting!");
        //System.exit(1);  Commented out for now to prevent shutting down while AVL feed is refreshing.
      } else {
        _log.info("hangTime:" + hangTime);
      }
    }
  }

  private String readAvlUpdatesFromUrl(URL url) throws IOException {
    String result = "";
    HttpURLConnection avlConnection = (HttpURLConnection) url.openConnection();
    avlConnection.setRequestProperty(
        "Accept",
        "text/html,application/xhtml+xml,application/json;q=0.9,application/xml;q=0.9,*/*;q=0.8");
    Map<String, List<String>> properties = avlConnection.getRequestProperties();
    if (properties.size() == 0) {
      _log.info("no request properties present.");
    }
    for (String propertyKey : properties.keySet()) {
      _log.info("property key: " + propertyKey);
    }
    _log.info("Accept header: " + avlConnection.getHeaderField("Accept"));
    _log.info("Accept property: " + avlConnection.getRequestProperty("Accept"));
    _log.info("content type: " + avlConnection.getContentType());
    _log.info("accept header: " + avlConnection.getHeaderField("Accept"));
    InputStream in = avlConnection.getInputStream();
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String nextLine = "";
      while (null != (nextLine = br.readLine())) {
        result = nextLine;
      }
    } finally {
      try {
        in.close();
      } catch (IOException ex) {
        _log.error("error closing url stream " + url);
      }
    }
    _log.info("result: " + result);
    return result;
  }
}
