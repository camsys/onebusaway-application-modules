package org.onebusaway.transit_data_federation.impl.beans;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.service_alerts.*;
import org.onebusaway.transit_data_federation.impl.service_alerts.ServiceAlertLibrary;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.beans.ServiceAlertsBeanService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripInstance;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlertsService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ServiceAlertsBeanServiceImpl implements ServiceAlertsBeanService {

    private ServiceAlertsService _serviceAlertsService;

    @Autowired
    public void setServiceAlertsService(ServiceAlertsService serviceAlertsService) {
        _serviceAlertsService = serviceAlertsService;
    }

    @Override
    public ServiceAlertBean createServiceAlert(String agencyId,
                                               ServiceAlertBean situationBean) {
        ServiceAlerts.ServiceAlert.Builder serviceAlertBuilder = getBeanAsServiceAlertBuilder(situationBean);
        ServiceAlerts.ServiceAlert serviceAlert = _serviceAlertsService.createOrUpdateServiceAlert(
                serviceAlertBuilder, agencyId);
        return getServiceAlertAsBean(serviceAlert);
    }

    @Override
    public void updateServiceAlert(ServiceAlertBean situationBean) {
        ServiceAlerts.ServiceAlert.Builder serviceAlertBuilder = getBeanAsServiceAlertBuilder(situationBean);
        _serviceAlertsService.createOrUpdateServiceAlert(serviceAlertBuilder, null);
    }

    @Override
    public void removeServiceAlert(AgencyAndId situationId) {
        _serviceAlertsService.removeServiceAlert(situationId);
    }

    @Override
    public ServiceAlertBean getServiceAlertForId(AgencyAndId situationId) {
        ServiceAlerts.ServiceAlert serviceAlert = _serviceAlertsService.getServiceAlertForId(situationId);
        if (serviceAlert == null)
            return null;
        return getServiceAlertAsBean(serviceAlert);
    }

    @Override
    public List<ServiceAlertBean> getServiceAlertsForFederatedAgencyId(
            String agencyId) {
        List<ServiceAlerts.ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForFederatedAgencyId(agencyId);
        return list(serviceAlerts);
    }

    @Override
    public void removeAllServiceAlertsForFederatedAgencyId(String agencyId) {
        _serviceAlertsService.removeAllServiceAlertsForFederatedAgencyId(agencyId);
    }

    @Override
    public List<ServiceAlertBean> getServiceAlertsForStopId(long time,
                                                            AgencyAndId stopId) {
        List<ServiceAlerts.ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForStopId(
                time, stopId);
        return list(serviceAlerts);
    }

    @Override
    public List<ServiceAlertBean> getServiceAlertsForStopCall(long time,
                                                              BlockInstance blockInstance, BlockStopTimeEntry blockStopTime,
                                                              AgencyAndId vehicleId) {

        List<ServiceAlerts.ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForStopCall(
                time, blockInstance, blockStopTime, vehicleId);

        return list(serviceAlerts);
    }

    @Override
    public List<ServiceAlertBean> getServiceAlertsForVehicleJourney(long time,
                                                                    BlockTripInstance blockTripInstance, AgencyAndId vehicleId) {

        List<ServiceAlerts.ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlertsForVehicleJourney(
                time, blockTripInstance, vehicleId);

        return list(serviceAlerts);
    }

    @Override
    public List<ServiceAlertBean> getServiceAlerts(SituationQueryBean query) {
        List<ServiceAlerts.ServiceAlert> serviceAlerts = _serviceAlertsService.getServiceAlerts(query);
        return list(serviceAlerts);
    }

    @Override
    public List<ServiceAlertRecordBean> getServiceAlertRecordsForFederatedAgencyId(String agencyId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceAlertBean copyServiceAlert(String agencyId, ServiceAlertBean situationBean) {
        throw new UnsupportedOperationException();
    }


    /****
     * Private Methods
     ****/

    private List<ServiceAlertBean> list(List<ServiceAlerts.ServiceAlert> serviceAlerts) {
        List<ServiceAlertBean> beans = new ArrayList<ServiceAlertBean>();
        for (ServiceAlerts.ServiceAlert serviceAlert : serviceAlerts)
            beans.add(getServiceAlertAsBean(serviceAlert));
        return beans;
    }

    private ServiceAlertBean getServiceAlertAsBean(ServiceAlerts.ServiceAlert serviceAlert) {

        ServiceAlertBean bean = new ServiceAlertBean();

        AgencyAndId id = ServiceAlertLibrary.agencyAndId(serviceAlert.getId().getAgencyId(), serviceAlert.getId().getId());
        bean.setId(AgencyAndIdLibrary.convertToString(id));
        bean.setCreationTime(serviceAlert.getCreationTime());

        bean.setActiveWindows(getRangesAsBeans(serviceAlert.getActiveWindowList()));
        bean.setPublicationWindows(getRangesAsBeans(serviceAlert.getPublicationWindowList()));

        /**
         * Reasons
         */
        if (serviceAlert.hasCause())
            bean.setReason(getCauseAsReason(serviceAlert.getCause()));

        /**
         * Text descriptions
         */
        bean.setSummaries(getTranslatedStringsAsNLSBeans(serviceAlert.getSummary()));
        bean.setDescriptions(getTranslatedStringsAsNLSBeans(serviceAlert.getDescription()));
        bean.setUrls(getTranslatedStringsAsNLSBeans(serviceAlert.getUrl()));

        if (serviceAlert.hasSeverity())
            bean.setSeverity(ServiceAlertLibrary.convertSeverity(serviceAlert.getSeverity()));

        bean.setAllAffects(getAffectsAsBeans(serviceAlert));
        bean.setConsequences(getConsequencesAsBeans(serviceAlert));

        return bean;
    }

    private ServiceAlerts.ServiceAlert.Builder getBeanAsServiceAlertBuilder(
            ServiceAlertBean bean) {

        ServiceAlerts.ServiceAlert.Builder situation = ServiceAlerts.ServiceAlert.newBuilder();

        if (bean.getId() != null && !bean.getId().isEmpty()) {
            AgencyAndId id = AgencyAndIdLibrary.convertFromString(bean.getId());
            situation.setId(ServiceAlertLibrary.id(id));
        }
        situation.setCreationTime(bean.getCreationTime());

        situation.addAllActiveWindow(getBeansAsRanges(bean.getActiveWindows()));
        situation.addAllPublicationWindow(getBeansAsRanges(bean.getPublicationWindows()));

        /**
         * Reasons
         */
        situation.setCause(getReasonAsCause(bean.getReason()));

        /**
         * Text descriptions
         */
        situation.setSummary(getNLSBeansAsTranslatedString(bean.getSummaries()));
        situation.setDescription(getNLSBeansAsTranslatedString(bean.getDescriptions()));
        situation.setUrl(getNLSBeansAsTranslatedString(bean.getUrls()));

        if (bean.getSeverity() != null)
            situation.setSeverity(ServiceAlertLibrary.convertSeverity(bean.getSeverity()));

        situation.addAllAffects(getBeanAsAffects(bean));
        situation.addAllConsequence(getBeanAsConsequences(bean));

        return situation;
    }

    /****
     * Situations Affects
     ****/

    private List<SituationAffectsBean> getAffectsAsBeans(ServiceAlerts.ServiceAlert serviceAlert) {

        if (serviceAlert.getAffectsCount() == 0)
            return null;

        List<SituationAffectsBean> beans = new ArrayList<SituationAffectsBean>();

        for (ServiceAlerts.Affects affects : serviceAlert.getAffectsList()) {
            SituationAffectsBean bean = new SituationAffectsBean();
            if (affects.hasAgencyId())
                bean.setAgencyId(affects.getAgencyId());
            if (affects.hasApplicationId())
                bean.setApplicationId(affects.getApplicationId());
            if (affects.hasRouteId()) {
                AgencyAndId routeId = ServiceAlertLibrary.agencyAndId(affects.getRouteId().getAgencyId(), affects.getRouteId().getId());
                bean.setRouteId(AgencyAndId.convertToString(routeId));
            }
            if (affects.hasDirectionId())
                bean.setDirectionId(affects.getDirectionId());
            if (affects.hasTripId()) {
                AgencyAndId tripId = ServiceAlertLibrary.agencyAndId(affects.getTripId().getAgencyId(), affects.getTripId().getId());
                bean.setTripId(AgencyAndId.convertToString(tripId));
            }
            if (affects.hasStopId()) {
                AgencyAndId stopId = ServiceAlertLibrary.agencyAndId(affects.getStopId().getAgencyId(), affects.getStopId().getId());
                bean.setStopId(AgencyAndId.convertToString(stopId));
            }
            if (affects.hasApplicationId())
                bean.setApplicationId(affects.getApplicationId());
            beans.add(bean);
        }
        return beans;
    }

    private List<ServiceAlerts.Affects> getBeanAsAffects(ServiceAlertBean bean) {

        List<ServiceAlerts.Affects> affects = new ArrayList<ServiceAlerts.Affects>();

        if (!CollectionsLibrary.isEmpty(bean.getAllAffects())) {
            for (SituationAffectsBean affectsBean : bean.getAllAffects()) {
                ServiceAlerts.Affects.Builder builder = ServiceAlerts.Affects.newBuilder();
                if (affectsBean.getAgencyId() != null)
                    builder.setAgencyId(affectsBean.getAgencyId());
                if (affectsBean.getApplicationId() != null)
                    builder.setApplicationId(affectsBean.getApplicationId());
                if (affectsBean.getRouteId() != null) {
                    AgencyAndId routeId = AgencyAndId.convertFromString(affectsBean.getRouteId());
                    builder.setRouteId(ServiceAlertLibrary.id(routeId));
                }
                if (affectsBean.getDirectionId() != null)
                    builder.setDirectionId(affectsBean.getDirectionId());
                if (affectsBean.getTripId() != null) {
                    AgencyAndId tripId = AgencyAndId.convertFromString(affectsBean.getTripId());
                    builder.setTripId(ServiceAlertLibrary.id(tripId));
                }
                if (affectsBean.getStopId() != null) {
                    AgencyAndId stopId = AgencyAndId.convertFromString(affectsBean.getStopId());
                    builder.setStopId(ServiceAlertLibrary.id(stopId));
                }
                affects.add(builder.build());
            }
        }

        return affects;
    }

    /****
     * Consequence
     ****/

    private List<SituationConsequenceBean> getConsequencesAsBeans(
            ServiceAlerts.ServiceAlert serviceAlert) {
        if (serviceAlert.getConsequenceCount() == 0)
            return null;
        List<SituationConsequenceBean> beans = new ArrayList<SituationConsequenceBean>();
        for (ServiceAlerts.Consequence consequence : serviceAlert.getConsequenceList()) {
            SituationConsequenceBean bean = new SituationConsequenceBean();
            if (consequence.hasEffect())
                bean.setEffect(ServiceAlertLibrary.convertEffect(consequence.getEffect()));
            if (consequence.hasDetourPath())
                bean.setDetourPath(consequence.getDetourPath());
            if (consequence.getDetourStopIdsCount() != 0) {
                List<String> stopIds = new ArrayList<String>();
                for (ServiceAlerts.Id stopId : consequence.getDetourStopIdsList()) {
                    AgencyAndId id = ServiceAlertLibrary.agencyAndId(stopId.getAgencyId(), stopId.getId());
                    stopIds.add(AgencyAndId.convertToString(id));
                }
                bean.setDetourStopIds(stopIds);
            }
            beans.add(bean);
        }
        return beans;
    }

    private List<ServiceAlerts.Consequence> getBeanAsConsequences(ServiceAlertBean bean) {

        List<ServiceAlerts.Consequence> consequences = new ArrayList<ServiceAlerts.Consequence>();

        if (!CollectionsLibrary.isEmpty(bean.getConsequences())) {
            for (SituationConsequenceBean consequence : bean.getConsequences()) {
                ServiceAlerts.Consequence.Builder builder = ServiceAlerts.Consequence.newBuilder();
                if (consequence.getEffect() != null)
                    builder.setEffect(ServiceAlertLibrary.convertEffect(consequence.getEffect()));
                if (consequence.getDetourPath() != null)
                    builder.setDetourPath(consequence.getDetourPath());
                if (!CollectionsLibrary.isEmpty(consequence.getDetourStopIds())) {
                    List<ServiceAlerts.Id> detourStopIds = new ArrayList<ServiceAlerts.Id>();
                    for (String detourStopId : consequence.getDetourStopIds()) {
                        ServiceAlerts.Id id = ServiceAlertLibrary.id(AgencyAndId.convertFromString(detourStopId));
                        detourStopIds.add(id);
                    }
                    builder.addAllDetourStopIds(detourStopIds);
                }
                consequences.add(builder.build());
            }
        }

        return consequences;
    }

    /****
     *
     ****/

    private ServiceAlerts.ServiceAlert.Cause getReasonAsCause(String reason) {
        if (reason == null)
            return ServiceAlerts.ServiceAlert.Cause.UNKNOWN_CAUSE;
        return ServiceAlerts.ServiceAlert.Cause.valueOf(reason);
    }

    private String getCauseAsReason(ServiceAlerts.ServiceAlert.Cause cause) {
        return cause.toString();
    }

    /****
     *
     ****/

    private List<TimeRangeBean> getRangesAsBeans(List<ServiceAlerts.TimeRange> ranges) {
        if (ranges == null || ranges.isEmpty())
            return null;
        List<TimeRangeBean> beans = new ArrayList<TimeRangeBean>();
        for (ServiceAlerts.TimeRange range : ranges) {
            TimeRangeBean bean = new TimeRangeBean();
            if (range.hasStart())
                bean.setFrom(range.getStart());
            if (range.hasEnd())
                bean.setTo(range.getEnd());
            beans.add(bean);
        }
        return beans;
    }

    private List<ServiceAlerts.TimeRange> getBeansAsRanges(List<TimeRangeBean> beans) {
        if (beans == null)
            return Collections.emptyList();
        List<ServiceAlerts.TimeRange> ranges = new ArrayList<ServiceAlerts.TimeRange>();
        for (TimeRangeBean bean : beans) {
            ServiceAlerts.TimeRange.Builder range = ServiceAlerts.TimeRange.newBuilder();
            if (bean.getFrom() > 0)
                range.setStart(bean.getFrom());
            if (bean.getTo() > 0)
                range.setEnd(bean.getTo());
            if (range.hasStart() || range.hasEnd())
                ranges.add(range.build());
        }
        return ranges;
    }

    private ServiceAlerts.TranslatedString getNLSBeansAsTranslatedString(
            List<NaturalLanguageStringBean> nlsBeans) {
        ServiceAlerts.TranslatedString.Builder builder = ServiceAlerts.TranslatedString.newBuilder();
        if (!CollectionsLibrary.isEmpty(nlsBeans)) {
            for (NaturalLanguageStringBean nlsBean : nlsBeans) {
                if (nlsBean.getValue() == null)
                    continue;
                ServiceAlerts.TranslatedString.Translation.Builder translation = ServiceAlerts.TranslatedString.Translation.newBuilder();
                translation.setText(nlsBean.getValue());
                translation.setLanguage(nlsBean.getLang());
                builder.addTranslation(translation);
            }
        }
        return builder.build();
    }

    private List<NaturalLanguageStringBean> getTranslatedStringsAsNLSBeans(
            ServiceAlerts.TranslatedString strings) {

        if (strings == null || strings.getTranslationCount() == 0)
            return null;

        List<NaturalLanguageStringBean> nlsBeans = new ArrayList<NaturalLanguageStringBean>();
        for (ServiceAlerts.TranslatedString.Translation translation : strings.getTranslationList()) {
            NaturalLanguageStringBean nls = new NaturalLanguageStringBean();
            nls.setValue(translation.getText());
            nls.setLang(translation.getLanguage());
            nlsBeans.add(nls);
        }

        return nlsBeans;
    }

}
