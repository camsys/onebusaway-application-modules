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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.onebusaway.admin.service.alerts.FeedRetrievalService;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class HttpFeedRetrievalServiceImpl implements FeedRetrievalService {

    private final HttpClient _httpClient = new HttpClient();

    @Override
    public InputStream getFeed(String name, String url) throws Exception {
        HttpMethod httpget = new GetMethod(url);
        int response = _httpClient.executeMethod(httpget);
        if (response != HttpStatus.SC_OK) {
            throw new Exception("service " + name + " poll failed, returned status code: " + response);
        }
        return httpget.getResponseBodyAsStream();
    }
}
