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
package org.onebusaway.admin.service.alerts.impl;


import com.google.transit.realtime.GtfsRealtime.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.onebusaway.admin.service.alerts.*;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.onebusaway.admin.util.RssDocumentBuilderUtil.*;

@Component("rssServiceAlertsService")
public class RssServiceAlertsServiceImpl implements IntegratingServiceAlertsService {

    private static Logger _log = LoggerFactory.getLogger(RssServiceAlertsServiceImpl.class);

    private String _serviceStatusUrlString = null;
    private String _serviceAdvisoryUrlString = null;
    private String _alertSource = "default";
    private FeedRetrievalService _feedRetrievalService;
    private ScheduledExecutorService _executor;
    private ConsoleServiceAlertsService _serviceAlertsService;
    private GtfsSupportService _gtfsSupportService;
    private RssServiceAlertsMapper _rssServiceAlertsMapper;

    // NOTE!  because we cache alerts here, we may hold on to alerts that have been manually deleted from db
    private Map<String, ServiceAlertBean> _alertCache;
    private boolean _removeAgencyIds = true;
    private FeedMessage _feed = null;
    private Locale _locale = null;

    private int _refreshRate = 2; // minutes


    @Autowired
    public void setConsoleServiceAlertsService(ConsoleServiceAlertsService service) {
        _serviceAlertsService = service;
    }

    @Autowired
    public void setRssServiceAlertsMapper(RssServiceAlertsMapper rssServiceAlertsMapper){
        _rssServiceAlertsMapper = rssServiceAlertsMapper;
    }

    @Autowired
    public void setGtfsSupportService(GtfsSupportService gtfsSupportService){
        _gtfsSupportService = gtfsSupportService;
    }

    @Autowired
    public void setFeedRetrievalService(FeedRetrievalService feedRetrievalService){
        _feedRetrievalService = feedRetrievalService;
    }

    public void setServiceStatusUrlString(String url) {
      _serviceStatusUrlString = url;
    }
    
    public void setServiceAdvisoryUrlString(String url) {
      _serviceAdvisoryUrlString = url;
    }
    
    public void setAlertSource(String source) {
      _alertSource = source;
    }
    
    public void setLocale(Locale locale) {
      _locale = locale;
    }

    public void setRefreshRate(int rateInMinutes) {
        this._refreshRate = rateInMinutes;
    }
    public int getRefreshRate() {
        return _refreshRate;
    }

    public boolean isEnabled() {
      return _serviceStatusUrlString != null && _serviceAdvisoryUrlString != null;
    }


    @PostConstruct
    public void start() throws Exception {
        if (_locale == null)
          _locale = Locale.getDefault();
        
        _executor = Executors.newSingleThreadScheduledExecutor();
        // re-build internal route cache
        _executor.scheduleAtFixedRate(new RefreshDataTask(), 0, 1, TimeUnit.HOURS);
        // poll feed after cache is built above
        _executor.scheduleAtFixedRate(new PollRssTask(), 1, getRefreshRate(), TimeUnit.MINUTES);
    }

    @PreDestroy
    public void stop() throws IOException {
        if (_executor != null)
            _executor.shutdownNow();
    }

    @Override
    public FeedMessage getServiceAlertFeed() {
      return _feed;
    }
    
    public List<ServiceAlertBean> pollServiceAdvisoryRssFeed() throws Exception {

        List<ServiceAlertBean> alerts = new ArrayList<ServiceAlertBean>();
        if (_serviceAdvisoryUrlString == null) return alerts;

        InputStream input = _feedRetrievalService.getFeed("advisory", _serviceAdvisoryUrlString);
        Document doc = buildDocumentFromRssFeed(input);
        List<Element> elements = getDocumentElements(doc);
        String language = getDocumentLanguage(doc, _locale);

        for(Element itemElement : elements){
            ServiceAlertBean serviceAlertBean = _rssServiceAlertsMapper
                    .rssAlertToServiceAlertBean(itemElement, language, _alertSource, RssServiceAlertType.ADVISORY);
            alerts.add(serviceAlertBean);
        }
        return alerts;
    }



    public List<ServiceAlertBean>  pollServiceStatusRssFeed() throws Exception {
        List<ServiceAlertBean> alerts = new ArrayList<ServiceAlertBean>();  
        if (_serviceStatusUrlString == null) return alerts;

        InputStream input = _feedRetrievalService.getFeed("status", _serviceStatusUrlString);
        Document doc = buildDocumentFromRssFeed(input);
        List<Element> elements = getDocumentElements(doc);
        String language = getDocumentLanguage(doc, _locale);

        for(Element itemElement : elements){
            ServiceAlertBean serviceAlertBean = _rssServiceAlertsMapper
                    .rssAlertToServiceAlertBean(itemElement, language, _alertSource, RssServiceAlertType.ALERT);
            alerts.add(serviceAlertBean);
        }
        return alerts;
    }




    private class PollRssTask implements Runnable {
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            List<ServiceAlertBean> toAdd = new ArrayList<>();
            List<AgencyAndId> toRemove = new ArrayList<>();
            List<ServiceAlertBean> toUpdate = new ArrayList<>();

            _log.info("PollRssTask.run enter");
            try {
                if (!isEnabled()) {
                    return;
                }

                if (!_gtfsSupportService.hasRouteShortNameMappings()) {
                    _log.info("empty route map, exiting");
                    return;
                }

                ListBean<ServiceAlertBean> currentObaAlerts = _serviceAlertsService.getAllServiceAlertsForAgencyId(_gtfsSupportService.getAgencyId());
                for (ServiceAlertBean serviceAlertBean : currentObaAlerts.getList()) {
                    String linkText = "NuLl";
                    if (serviceAlertBean.getUrls() != null
                            && !serviceAlertBean.getUrls().isEmpty()
                            && serviceAlertBean.getUrls().get(0) != null) {
                        linkText = serviceAlertBean.getUrls().get(0).getValue();
                    }
                    _log.info("found existing sa=" + serviceAlertBean.getSummaries().get(0).getValue()
                            + " with source=" + serviceAlertBean.getSource()
                            + " and link=" + linkText);
                    if (!_alertCache.keySet().contains(serviceAlertBean.getId())
                            && serviceAlertBean.getSource() != null
                            /* WMATA_alert or WMATA_advisory */
                            && serviceAlertBean.getSource().contains(_alertSource + "_")) {
                        _log.info("new service alert=" + serviceAlertBean.getSummaries().get(0).getValue()
                            + " and link=" + linkText);
                        _alertCache.put(serviceAlertBean.getId(), serviceAlertBean);
                    }
                }

                List<ServiceAlertBean> rssAlerts = new ArrayList<ServiceAlertBean>();
                try {
                    rssAlerts.addAll(pollServiceAdvisoryRssFeed());
                } catch (Exception e) {
                    _log.warn(e.getMessage());
                    e.printStackTrace();
                }

                try {
                    rssAlerts.addAll(pollServiceStatusRssFeed());
                } catch (Exception e) {
                    _log.warn(e.getMessage());
                    e.printStackTrace();
                }

                Map<String, ServiceAlertBean> currentRssAlertMap = new HashMap<String, ServiceAlertBean>();
                for (ServiceAlertBean alert : rssAlerts) {
                    currentRssAlertMap.put(alert.getId(), alert);
                }

                Iterator<String> cachedAlertsGuidIter = _alertCache.keySet().iterator();
                //first, check for expired alerts and existing alerts that have been updated
                while (cachedAlertsGuidIter.hasNext()) {
                    String guid = cachedAlertsGuidIter.next();
                    if (!currentRssAlertMap.keySet().contains(guid)) {
                        _log.info("Removing expired alert with guid " + guid);
                        try {
                            toRemove.add(AgencyAndId.convertFromString(guid));
                        } catch (Exception any) {
                            _log.error("invalid guid=" + guid);
                        }
                        cachedAlertsGuidIter.remove();
                    } else {
                        ServiceAlertBean currentAlert = _alertCache.get(guid);
                        ServiceAlertBean rssAlert = currentRssAlertMap.get(guid);
                        if (rssAlert.getCreationTime() > currentAlert.getCreationTime()) {
                            _log.info("Updating alert with guid " + guid);
                            _alertCache.put(guid, rssAlert);

                            toUpdate.add(rssAlert);
                        }
                    }
                }

                //now create alerts for any new guids on the RSS feed
                for (String currentRssGuid : currentRssAlertMap.keySet()) {
                    if (!_alertCache.keySet().contains(currentRssGuid)) {
                        _log.info("Creating alert with guid " + currentRssGuid);

                        toAdd.add(currentRssAlertMap.get(currentRssGuid));
                        _alertCache.put(currentRssGuid, currentRssAlertMap.get(currentRssGuid));
                    }
                }

                _serviceAlertsService.removeServiceAlerts(toRemove);
                _serviceAlertsService.updateServiceAlerts(_gtfsSupportService.getAgencyId(), toUpdate);
                _serviceAlertsService.createServiceAlerts(_gtfsSupportService.getAgencyId(), toAdd);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                long end = System.currentTimeMillis();
                _log.info("PollRssTask.run exit in " + (end - start) + "ms");
            }
        }
    }
    private class RefreshDataTask implements Runnable {

        @Override
        public void run() {

            if (!isEnabled())
            {
                _log.info("exiting refresh cache, not enabled");
                return;
            }

            while(true){
                try {
                    _gtfsSupportService.refreshRouteShortNameToRouteIdMap();
                    _alertCache = new HashMap<>();
                    break;
                } catch (RemoteConnectFailureException rcfe) {
                    _log.warn("TDS hasn't started yet, will re-attempt to load routes in 30 seconds");
                    try {
                        Thread.sleep((30l * 1000l));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
