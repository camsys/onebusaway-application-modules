/**
 * Copyright (C) 2025 Cambridge Systematics, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;
import org.onebusaway.admin.service.alerts.GtfsSupportService;
import org.onebusaway.admin.service.alerts.RssServiceAlertType;
import org.onebusaway.admin.service.alerts.RssServiceAlertsMapper;
import org.onebusaway.admin.util.DateTimeUtil;
import org.onebusaway.alerts.service.ServiceAlerts;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.service_alerts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class RssServiceAlertsMapperImpl implements RssServiceAlertsMapper {

    private static Logger _log = LoggerFactory.getLogger(RssServiceAlertsMapperImpl.class);

    private GtfsSupportService _gtfsSupportService;

    @Autowired
    public void setGtfsSupportService(GtfsSupportService gtfsSupportService){
        _gtfsSupportService = gtfsSupportService;
    }

    @Override
    public ServiceAlertBean rssAlertToServiceAlertBean(Element itemElement,
                                                       String language,
                                                       String alertSource,
                                                       RssServiceAlertType alertType){

        String title = itemElement.getChild("title").getValue();
        String link = "";
        if (itemElement.getChild("link") != null)
            link = itemElement.getChild("link").getValue();
        String description = itemElement.getChild("description").getValue();
        String pubDateString = itemElement.getChild("pubDate").getValue();
        String guid = itemElement.getChild("guid").getValue();

        String formattedDescription = getFormattedDescription(description);
        String formattedGuid = getFormattedGuid(guid, title, alertType);
        Date pubDate = DateTimeUtil.parseRFC1123ToDate(pubDateString);
        List<SituationAffectsBean> affectedRouteIds = getRouteIds(title);

        ServiceAlertBean serviceAlertBean = new ServiceAlertBean();
        serviceAlertBean.setSource(alertSource + "_" + alertType.getLabel());
        serviceAlertBean.setAllAffects(affectedRouteIds);
        serviceAlertBean.setSeverity(ESeverity.UNKNOWN);
        serviceAlertBean.setSummaries(Arrays.asList(new NaturalLanguageStringBean[]{new NaturalLanguageStringBean(formattedDescription, language)}));
        serviceAlertBean.setReason(ServiceAlerts.ServiceAlert.Cause.UNKNOWN_CAUSE.name());
        SituationConsequenceBean situationConsequenceBean = new SituationConsequenceBean();
        situationConsequenceBean.setEffect(EEffect.SIGNIFICANT_DELAYS);
        serviceAlertBean.setConsequences(Arrays.asList(new SituationConsequenceBean[]{situationConsequenceBean}));
        serviceAlertBean.setCreationTime(pubDate.getTime());
        serviceAlertBean.setId(new AgencyAndId(_gtfsSupportService.getAgencyId(), formattedGuid).toString());
        if (StringUtils.isNotBlank(link))
            serviceAlertBean.setUrls(Arrays.asList(new NaturalLanguageStringBean[]{new NaturalLanguageStringBean(link, language)}));

        return serviceAlertBean;
    }

    private String getFormattedDescription(String description) {
        String formattedDescription = removeCommas(description);
        return formattedDescription;
    }

    private String removeCommas(String text){
        return text.replace(",", "");
    }

    private String getFormattedGuid(String guid, String title, RssServiceAlertType alertType) {
        if(alertType.equals(RssServiceAlertType.ADVISORY)){
            return guid + "_" + title;
        }
        return guid;
    }

    private List<SituationAffectsBean> getRouteIds(String description){
        String[] routeShortNames = description.split("\\:")[0].split("\\,");
        List<SituationAffectsBean> affectedRoutes = new ArrayList<SituationAffectsBean>();
        for(int i = 0; i < routeShortNames.length; i++) {
            String routeShortName = routeShortNames[i];
            routeShortName = routeShortName.toUpperCase().trim();
            String routeId = _gtfsSupportService.getRouteIdForRouteShortName(routeShortName);
            if(routeId != null){
                SituationAffectsBean situationAffectsBean = new SituationAffectsBean();
                situationAffectsBean.setAgencyId(_gtfsSupportService.getAgencyId());
                situationAffectsBean.setRouteId(routeId);
                affectedRoutes.add(situationAffectsBean);
            }else{
                _log.warn("No route found for route short name " + routeShortName);
            }
        }
        return affectedRoutes;
    }


}
