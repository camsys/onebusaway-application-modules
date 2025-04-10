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
import org.jdom2.input.SAXBuilder;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class RssDocumentBuilderUtil {

    private static final SAXBuilder builder = new SAXBuilder();


    public static Document buildDocumentFromRssFeed(InputStream feed) throws Exception {
        return builder.build(feed);
    }

    public static List<Element> getDocumentElements(Document doc) {
        return doc.getRootElement().getChild("channel").getChildren("item");
    }

    public static String getDocumentLanguage(Document doc, Locale locale) {
        String language = doc.getRootElement().getChild("channel").getChildText("language");
        if(language == null)
            language = locale.getLanguage();  //they don't send language for this feed currently, perhaps they'll start?
        if(language.equals("en-us")) {
            // java prefers en
            language = locale.getLanguage();
        }
        return language;
    }
}
