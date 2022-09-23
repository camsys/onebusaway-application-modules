/**
 * Copyright (C) 2020 Cambridge Systematics, Inc.
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
package org.onebusaway.admin.service.server.impl;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtimeConstants;
import org.onebusaway.admin.service.server.ConsoleServiceAlertsService;
import org.onebusaway.alerts.impl.ServiceAlertBeanHelper;
import org.onebusaway.alerts.impl.ServiceAlertBuilderHelper;
import org.onebusaway.alerts.impl.ServiceAlertRecord;
import org.onebusaway.alerts.service.ServiceAlertsService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertRecordBean;
import org.onebusaway.util.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * by-pass the TDS and perform operations directly on the database.
 */
@Component
public class ConsoleServiceAlertsServiceImpl implements ConsoleServiceAlertsService {

    private static final Logger _log = LoggerFactory.getLogger(ConsoleServiceAlertsServiceImpl.class);


    @Autowired
    private ServiceAlertsService _service;

    @Override
    public GtfsRealtime.FeedMessage getAll() {
        List<ServiceAlertBean> allServiceAlerts = ServiceAlertBeanHelper.list(_service.getAllServiceAlerts());
        GtfsRealtime.FeedMessage.Builder feed = GtfsRealtime.FeedMessage.newBuilder();
        GtfsRealtime.FeedHeader.Builder header = feed.getHeaderBuilder();
        header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
        long time = SystemTime.currentTimeMillis();
        header.setTimestamp(time / 1000);
        ListBean<ServiceAlertBean> listBean = new ListBean<>();
        listBean.setList(allServiceAlerts);
        listBean.setLimitExceeded(false);

        ServiceAlertBuilderHelper.fillFeedMessage(feed, listBean, "1", time);
        return feed.build();
    }
    @Override
    public GtfsRealtime.FeedMessage getAlerts(String agencyId) {
        ListBean<ServiceAlertBean> listBean = getAllServiceAlertsForAgencyId(agencyId);

        GtfsRealtime.FeedMessage.Builder feed = GtfsRealtime.FeedMessage.newBuilder();
        GtfsRealtime.FeedHeader.Builder header = feed.getHeaderBuilder();
        header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
        long time = SystemTime.currentTimeMillis();
        header.setTimestamp(time / 1000);


        ServiceAlertBuilderHelper.fillFeedMessage(feed, listBean, agencyId, time);

        return feed.build();
    }

    @Override
    public void removeServiceAlert(AgencyAndId id) {
        _log.info("removing alert " + id.getId());
        _service.removeServiceAlert(id);
    }

    @Override
    public void updateServiceAlert(String agencyId, ServiceAlertBean bean) {
        updateServiceAlert(agencyId, bean, false);
    }

    @Override
    public void updateServiceAlert(String agencyId, ServiceAlertBean bean, Boolean isCopy) {
        _log.info("updating alert " + bean.getId());
        ServiceAlertRecord record = ServiceAlertBeanHelper.getServiceAlertRecordFromServiceAlertBean(bean, agencyId);
        if (Boolean.TRUE.equals(isCopy))
            record.setCopy(isCopy);
        else
            record.setCopy(null);
        _service.createOrUpdateServiceAlert(record);
    }

    @Override
    public ServiceAlertBean createServiceAlert(String agencyId, ServiceAlertBean serviceAlertBean) {
        _log.info("creating alert " + serviceAlertBean.getId());
        return ServiceAlertBeanHelper.getServiceAlertAsBean(
                _service.createOrUpdateServiceAlert(
                        ServiceAlertBeanHelper.getServiceAlertRecordFromServiceAlertBean(serviceAlertBean, agencyId.toString())));
    }

    @Override
    public ServiceAlertBean getServiceAlertForId(String alertId) {
        return ServiceAlertBeanHelper.getServiceAlertAsBean(_service.getServiceAlertForId(AgencyAndId.convertFromString(alertId)));
    }

    @Override
    public ServiceAlertRecord copyServiceAlert(String agencyId, ServiceAlertBean model) {
        return _service.copyServiceAlert(ServiceAlertBeanHelper.getServiceAlertRecordFromServiceAlertBean(model, agencyId));
    }

    @Override
    /**
     * retrieve service alerts OWNED by the agency -- not agency level service alerts
     */
    public ListBean<ServiceAlertRecordBean> getAllServiceAlertRecordsForAgencyId(String agencyId) {
        List<ServiceAlertRecord> serviceAlertsForAgencyId = _service.getAllServiceAlerts();
        List<ServiceAlertRecord> filtered = new ArrayList<>();
        if (serviceAlertsForAgencyId != null && !serviceAlertsForAgencyId.isEmpty()) {
            for (ServiceAlertRecord record : serviceAlertsForAgencyId) {
                if (record.getAgencyId().equals(agencyId)) {
                    filtered.add(record);
                }
            }
        }
        ListBean<ServiceAlertRecordBean> listBean = new ListBean<>();
        listBean.setList(ServiceAlertBeanHelper.listRecordBeans(filtered));
        return listBean;
    }

    @Override
    public void removeAllServiceAlertsForAgencyId(String agencyId) {
        _service.removeAllServiceAlertsForFederatedAgencyId(agencyId);
    }

    @Override
    public void removeServiceAlerts(List<AgencyAndId> toRemove) {
        _service.removeServiceAlerts(toRemove);
    }

    @Override
    public void updateServiceAlerts(String agencyId, List<ServiceAlertBean> toUpdate) {
        if (toUpdate == null || toUpdate.isEmpty()) return;
        ArrayList<ServiceAlertRecord> records = new ArrayList<>(toUpdate.size());
        for (ServiceAlertBean bean : toUpdate) {
            records.add(ServiceAlertBeanHelper.getServiceAlertRecordFromServiceAlertBean(bean, agencyId));
        }
        _service.createOrUpdateServiceAlerts(agencyId, records);
    }

    @Override
    public void createServiceAlerts(String agencyId, List<ServiceAlertBean> toAdd) {
        updateServiceAlerts(agencyId, toAdd);
    }

    @Override
    /**
     * retrieve service alerts as beans OWNED by the agency -- not agency level service alerts
     */
    public ListBean<ServiceAlertBean> getAllServiceAlertsForAgencyId(String agencyId) {
        List<ServiceAlertRecord> serviceAlertsForAgencyId = _service.getAllServiceAlerts();
        List<ServiceAlertRecord> filtered = new ArrayList<>();
        if (serviceAlertsForAgencyId != null) {
            for (ServiceAlertRecord record : serviceAlertsForAgencyId) {
                if (record == null) continue;
                // don't pass on favorites
                if (agencyId.equals(record.getAgencyId()) && !Boolean.TRUE.equals(record.isCopy())) {
                    filtered.add(record);
                }
            }
        }
        List<ServiceAlertBean> alertBeans = ServiceAlertBeanHelper.list(filtered);
        ListBean<ServiceAlertBean> listBean = new ListBean<>();
        listBean.setList(alertBeans);
        listBean.setLimitExceeded(false);
        return listBean;
    }

}