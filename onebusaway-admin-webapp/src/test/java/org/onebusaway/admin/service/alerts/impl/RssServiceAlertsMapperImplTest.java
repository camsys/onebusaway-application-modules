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

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onebusaway.admin.service.alerts.GtfsSupportService;
import org.onebusaway.admin.service.alerts.RssServiceAlertType;
import org.onebusaway.admin.util.RssDocumentBuilderUtil;
import org.onebusaway.admin.util.RssDocumentBuilderUtilTest;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RssServiceAlertsMapperImplTest {

    private static InputStream rssFeed = null;

    @BeforeClass
    public static void setup(){
        rssFeed = RssDocumentBuilderUtilTest.class.getResourceAsStream("/org/onebusaway/admin/service/alerts/impl/rss_with_comma.xml");
    }

    @Test
    public void testRssAlertToServiceAlertBean() throws Exception {
        GtfsSupportService gtfsSupportService = mock(GtfsSupportServiceImpl.class);
        when(gtfsSupportService.getRouteIdForRouteShortName(anyString())).thenReturn("1");
        when(gtfsSupportService.getAgencyId()).thenReturn("1");

        RssServiceAlertsMapperImpl rssServiceAlertsMapper = new RssServiceAlertsMapperImpl();
        rssServiceAlertsMapper.setGtfsSupportService(gtfsSupportService);

        Document document = RssDocumentBuilderUtil.buildDocumentFromRssFeed(rssFeed);
        String language = RssDocumentBuilderUtil.getDocumentLanguage(document, Locale.ENGLISH);
        List<Element> elements = RssDocumentBuilderUtil.getDocumentElements(document);
        Element element = elements.get(0);


        ServiceAlertBean serviceAlertBean = rssServiceAlertsMapper.rssAlertToServiceAlertBean(element,language,
                "rss.xml", RssServiceAlertType.ALERT);

        String expectedSummary = "K6 Service Alert: Northbound buses are on detour until 11pm on Wednesday April 23 " +
                "at New Hampshire Ave & University Blvd because of " +
                "the Purple Line construction. For detailed routing information visit: " +
                "https://wmata.com/service/status/details/temporary-detour-construction-on-new-hampshire-ave.cfm";

        Assert.assertEquals("1_b4412c7a-cc0b-f011-bae2-002248279643", serviceAlertBean.getId());
        Assert.assertEquals(expectedSummary, serviceAlertBean.getSummaries().get(0).getValue());
    }
}
