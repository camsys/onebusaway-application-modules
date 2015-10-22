/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google, Inc.
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
package org.onebusaway.transit_data_federation.impl.beans;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.service_alerts.*;
import org.onebusaway.transit_data_federation.impl.service_alerts.*;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.beans.ServiceAlertsBeanService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripInstance;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlertsService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
class ServiceAlertsBeanServiceImpl implements ServiceAlertsBeanService {

  private ServiceAlertsService _serviceAlertsService;

  @Autowired
  public void setServiceAlertsService(ServiceAlertsService serviceAlertsService) {
    _serviceAlertsService = serviceAlertsService;
  }

  @Override
  public ServiceAlertBean createServiceAlert(String agencyId,
      ServiceAlertBean situationBean) {
    ServiceAlertRecord serviceAlertRecord = getServiceAlertRecordFromServiceAlertBean(
        situationBean, agencyId);
    serviceAlertRecord = _serviceAlertsService.createOrUpdateServiceAlert(
        serviceAlertRecord);
    return getServiceAlertAsBean(serviceAlertRecord);
  }

  @Override
  public void updateServiceAlert(ServiceAlertBean situationBean) {
    AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationBean.getId());
    ServiceAlertRecord serviceAlertRecord = getServiceAlertRecordFromServiceAlertBean(
        situationBean, id.getAgencyId());
    _serviceAlertsService.createOrUpdateServiceAlert(serviceAlertRecord);
  }

  @Override
  public void removeServiceAlert(AgencyAndId situationId) {
    _serviceAlertsService.removeServiceAlert(situationId);
  }

  @Override
  public ServiceAlertBean getServiceAlertForId(AgencyAndId situationId) {
    ServiceAlertRecord serviceAlert = _serviceAlertsService.getServiceAlertForId(situationId);
    if (serviceAlert == null)
      return null;
    return getServiceAlertAsBean(serviceAlert);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForFederatedAgencyId(
      String agencyId) {
    List<ServiceAlertRecord> serviceAlerts = _serviceAlertsService.getServiceAlertsForFederatedAgencyId(agencyId);
    return list(serviceAlerts);
  }

  @Override
  public void removeAllServiceAlertsForFederatedAgencyId(String agencyId) {
    _serviceAlertsService.removeAllServiceAlertsForFederatedAgencyId(agencyId);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForStopId(long time,
      AgencyAndId stopId) {
    List<ServiceAlertRecord> serviceAlerts = _serviceAlertsService.getServiceAlertsForStopId(
        time, stopId);
    return list(serviceAlerts);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForStopCall(long time,
      BlockInstance blockInstance, BlockStopTimeEntry blockStopTime,
      AgencyAndId vehicleId) {

    List<ServiceAlertRecord> serviceAlerts = _serviceAlertsService.getServiceAlertsForStopCall(
        time, blockInstance, blockStopTime, vehicleId);

    return list(serviceAlerts);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlertsForVehicleJourney(long time,
      BlockTripInstance blockTripInstance, AgencyAndId vehicleId) {

    List<ServiceAlertRecord> serviceAlerts = _serviceAlertsService.getServiceAlertsForVehicleJourney(
        time, blockTripInstance, vehicleId);

    return list(serviceAlerts);
  }

  @Override
  public List<ServiceAlertBean> getServiceAlerts(SituationQueryBean query) {
    List<ServiceAlertRecord> serviceAlerts = _serviceAlertsService.getServiceAlerts(query);
    return list(serviceAlerts);
  }

  /****
   * Private Methods
   ****/

  private List<ServiceAlertBean> list(List<ServiceAlertRecord> serviceAlerts) {
    List<ServiceAlertBean> beans = new ArrayList<ServiceAlertBean>();
    for (ServiceAlertRecord serviceAlert : serviceAlerts)
      beans.add(getServiceAlertAsBean(serviceAlert));
    return beans;
  }

  private ServiceAlertBean getServiceAlertAsBean(ServiceAlertRecord serviceAlert) {

    ServiceAlertBean bean = new ServiceAlertBean();

    AgencyAndId id = ServiceAlertLibrary.agencyAndId(serviceAlert.getAgencyId(), serviceAlert.getServiceAlertId());
    bean.setId(AgencyAndIdLibrary.convertToString(id));
    bean.setCreationTime(serviceAlert.getCreationTime());

    bean.setActiveWindows(getRangesAsBeans(serviceAlert.getActiveWindows()));
    bean.setPublicationWindows(getRangesAsBeans(serviceAlert.getPublicationWindows()));

    /**
     * Reasons
     */
    if (serviceAlert.getCause() != null)
      bean.setReason(getCauseAsReason(serviceAlert.getCause()));

    /**
     * Text descriptions
     */
    bean.setSummaries(getTranslatedStringsAsNLSBeans(serviceAlert.getSummaries()));
    bean.setDescriptions(getTranslatedStringsAsNLSBeans(serviceAlert.getDescriptions()));
    bean.setUrls(getTranslatedStringsAsNLSBeans(serviceAlert.getUrls()));

    if (serviceAlert.getSeverity() != null)
      bean.setSeverity(serviceAlert.getSeverity());

    bean.setAllAffects(getAffectsAsBeans(serviceAlert));
    bean.setConsequences(getConsequencesAsBeans(serviceAlert));

    return bean;
  }

  private ServiceAlertRecord getServiceAlertRecordFromServiceAlertBean(
      ServiceAlertBean bean, String agencyId) {

    ServiceAlertRecord situation = new ServiceAlertRecord();
    situation.setAgencyId(agencyId);
    if (bean.getId() != null && !bean.getId().isEmpty()) {
      AgencyAndId id = AgencyAndIdLibrary.convertFromString(bean.getId());
      situation.setServiceAlertId(bean.getId());
      situation.setAgencyId(id.getAgencyId());
    }
    situation.setCreationTime(bean.getCreationTime());

    situation.setActiveWindows(getBeansAsRanges(bean.getActiveWindows()));
    situation.setPublicationWindows(
        getBeansAsRanges(bean.getPublicationWindows()));

    /**
     * Reasons
     */
    situation.setCause(getReasonAsCause(bean.getReason()));

    /**
     * Text descriptions
     */
    situation.setSummaries(new ArrayList<ServiceAlertLocalizedString>());
    for(NaturalLanguageStringBean summary : bean.getSummaries()){
      ServiceAlertLocalizedString string = new ServiceAlertLocalizedString();
      string.setLanguage(summary.getLang());
      string.setValue(summary.getValue());
      situation.getSummaries().add(string);
    }

    situation.setDescriptions(new ArrayList<ServiceAlertLocalizedString>());
    for(NaturalLanguageStringBean summary : bean.getDescriptions()){
      ServiceAlertLocalizedString string = new ServiceAlertLocalizedString();
      string.setLanguage(summary.getLang());
      string.setValue(summary.getValue());
      situation.getDescriptions().add(string);
    }

    if (bean.getSeverity() != null)
      situation.setSeverity(bean.getSeverity());

    situation.setAllAffects(getBeanAsAffects(bean));
    situation.setConsequences(getBeanAsConsequences(bean));

    situation.setSource(bean.getSource());

    return situation;
  }

  /****
   * Situations Affects
   ****/

  private List<SituationAffectsBean> getAffectsAsBeans(ServiceAlertRecord serviceAlert) {

    if (serviceAlert.getAllAffects().size() == 0)
      return null;

    List<SituationAffectsBean> beans = new ArrayList<SituationAffectsBean>();

    for (ServiceAlertsSituationAffectsClause affects : serviceAlert.getAllAffects()) {
      SituationAffectsBean bean = new SituationAffectsBean();
      if (affects.getAgencyId() != null)
        bean.setAgencyId(affects.getAgencyId());
      if (affects.getApplicationId() != null)
        bean.setApplicationId(affects.getApplicationId());
      if (affects.getRouteId() != null) {
        AgencyAndId routeId = ServiceAlertLibrary.agencyAndId(serviceAlert.getAgencyId(), affects.getRouteId());
        bean.setRouteId(AgencyAndId.convertToString(routeId));
      }
      if (affects.getDirectionId() != null)
        bean.setDirectionId(affects.getDirectionId());
      if (affects.getTripId() != null) {
        AgencyAndId tripId = ServiceAlertLibrary.agencyAndId(serviceAlert.getAgencyId(), affects.getTripId());
        bean.setTripId(AgencyAndId.convertToString(tripId));
      }
      if (affects.getStopId() != null) {
        AgencyAndId stopId = ServiceAlertLibrary.agencyAndId(serviceAlert.getAgencyId(), affects.getStopId());
        bean.setStopId(AgencyAndId.convertToString(stopId));
      }
      if (affects.getApplicationId()  != null)
        bean.setApplicationId(affects.getApplicationId());
      beans.add(bean);
    }
    return beans;
  }

  private List<ServiceAlertsSituationAffectsClause> getBeanAsAffects(ServiceAlertBean bean) {

    List<ServiceAlertsSituationAffectsClause> affectsList = new ArrayList<ServiceAlertsSituationAffectsClause>();

    if (!CollectionsLibrary.isEmpty(bean.getAllAffects())) {
      for (SituationAffectsBean affectsBean : bean.getAllAffects()) {
        ServiceAlertsSituationAffectsClause affects = new ServiceAlertsSituationAffectsClause();
        if (affectsBean.getAgencyId() != null)
          affects.setAgencyId(affectsBean.getAgencyId());
        if (affectsBean.getApplicationId() != null)
          affects.setApplicationId(affectsBean.getApplicationId());
        if (affectsBean.getRouteId() != null) {
          affects.setRouteId(affectsBean.getRouteId());
        }
        if (affectsBean.getDirectionId() != null)
          affects.setDirectionId(affectsBean.getDirectionId());
        if (affectsBean.getTripId() != null) {
          affects.setTripId(affectsBean.getTripId());
        }
        if (affectsBean.getStopId() != null) {
          affects.setStopId(affectsBean.getStopId());
        }
        affectsList.add(affects);
      }
    }

    return affectsList;
  }

  /****
   * Consequence
   ****/

  private List<SituationConsequenceBean> getConsequencesAsBeans(
      ServiceAlertRecord serviceAlert) {
    if (serviceAlert.getConsequences().size() == 0)
      return null;
    List<SituationConsequenceBean> beans = new ArrayList<SituationConsequenceBean>();
    for (ServiceAlertSituationConsequenceClause consequence : serviceAlert.getConsequences()) {
      SituationConsequenceBean bean = new SituationConsequenceBean();
      if (consequence.getEffect() != null)
        bean.setEffect(consequence.getEffect());
      if (consequence.getDetourPath() != null)
        bean.setDetourPath(consequence.getDetourPath());
      if (consequence.getDetourStopIds().size() != 0) {
        List<String> stopIds = new ArrayList<String>();
        for (String stopId : consequence.getDetourStopIds()) {
          AgencyAndId id = ServiceAlertLibrary.agencyAndId(serviceAlert.getAgencyId(), stopId);
          stopIds.add(AgencyAndId.convertToString(id));
        }
        bean.setDetourStopIds(stopIds);
      }
      beans.add(bean);
    }
    return beans;
  }

  private List<ServiceAlertSituationConsequenceClause> getBeanAsConsequences(ServiceAlertBean bean) {

    List<ServiceAlertSituationConsequenceClause> consequences = new ArrayList<ServiceAlertSituationConsequenceClause>();

    if (!CollectionsLibrary.isEmpty(bean.getConsequences())) {
      for (SituationConsequenceBean consequence : bean.getConsequences()) {
        ServiceAlertSituationConsequenceClause consequenceClause = new ServiceAlertSituationConsequenceClause();
        if (consequence.getEffect() != null)
          consequenceClause.setEffect(consequence.getEffect());
        if (consequence.getDetourPath() != null)
          consequenceClause.setDetourPath(consequence.getDetourPath());
        if (!CollectionsLibrary.isEmpty(consequence.getDetourStopIds())) {
          List<String> detourStopIds = new ArrayList<String>();
          for (String detourStopId : consequence.getDetourStopIds()) {
            detourStopIds.add(detourStopId);
          }
          consequenceClause.setDetourStopIds(detourStopIds);
        }
        consequences.add(consequenceClause);
      }
    }

    return consequences;
  }

  /****
   * 
   ****/

  private ECause getReasonAsCause(String reason) {
    if (reason == null)
      return ECause.UNKNOWN_CAUSE;
    return ECause.valueOf(reason);
  }

  private String getCauseAsReason(ECause cause) {
    return cause.toString();
  }

  /****
   * 
   ****/

  private List<TimeRangeBean> getRangesAsBeans(List<ServiceAlertTimeRange> ranges) {
    if (ranges == null || ranges.isEmpty())
      return null;
    List<TimeRangeBean> beans = new ArrayList<TimeRangeBean>();
    for (ServiceAlertTimeRange range : ranges) {
      TimeRangeBean bean = new TimeRangeBean();
      if (range.getFrom() != null)
        bean.setFrom(range.getFrom());
      if (range.getTo() != null)
        bean.setTo(range.getTo());
      beans.add(bean);
    }
    return beans;
  }

  private List<ServiceAlertTimeRange> getBeansAsRanges(List<TimeRangeBean> beans) {
    if (beans == null)
      return Collections.emptyList();
    List<ServiceAlertTimeRange> ranges = new ArrayList<ServiceAlertTimeRange>();
    for (TimeRangeBean bean : beans) {
      ServiceAlertTimeRange range = new ServiceAlertTimeRange();
      if (bean.getFrom() > 0)
        range.setFrom(bean.getFrom());
      if (bean.getTo() > 0)
        range.setTo(bean.getTo());
      if (range.getFrom() != null || range.getTo() != null)
        ranges.add(range);
    }
    return ranges;
  }

  private List<NaturalLanguageStringBean> getTranslatedStringsAsNLSBeans(
      List<ServiceAlertLocalizedString> strings) {

    if (strings == null || strings.size() == 0)
      return null;

    List<NaturalLanguageStringBean> nlsBeans = new ArrayList<NaturalLanguageStringBean>();
    for (ServiceAlertLocalizedString translation : strings) {
      NaturalLanguageStringBean nls = new NaturalLanguageStringBean();
      nls.setValue(translation.getValue());
      nls.setLang(translation.getLanguage());
      nlsBeans.add(nls);
    }

    return nlsBeans;
  }

}
