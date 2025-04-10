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
package org.onebusaway.admin.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class RssDocumentBuilderUtilTest {

    private static InputStream rssFeed = null;

    @BeforeClass
    public static void setup(){
        rssFeed = RssDocumentBuilderUtilTest.class.getResourceAsStream("/org/onebusaway/admin/util/rss.xml");
    }

    @Test
    public void testBuildDocumentFromRssFeed() throws Exception {
        Document doc = RssDocumentBuilderUtil.buildDocumentFromRssFeed(rssFeed);
        Assert.assertNotNull(doc);
    }

    @Test
    public void testGetDocumentElements() throws Exception {
        Document doc = RssDocumentBuilderUtil.buildDocumentFromRssFeed(rssFeed);
        List<Element> elements = RssDocumentBuilderUtil.getDocumentElements(doc);
        Assert.assertEquals(12, elements.size());
    }

    @Test
    public void testGetDocumentLanguage() throws Exception {
        Document doc = RssDocumentBuilderUtil.buildDocumentFromRssFeed(rssFeed);
        String language = RssDocumentBuilderUtil.getDocumentLanguage(doc, Locale.ENGLISH.stripExtensions());
        Assert.assertEquals("en", language);
    }
}
