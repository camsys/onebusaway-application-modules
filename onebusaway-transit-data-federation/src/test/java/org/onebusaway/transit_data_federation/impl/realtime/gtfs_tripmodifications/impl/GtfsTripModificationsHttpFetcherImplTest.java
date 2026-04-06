/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.TripModificationsFormat;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GtfsTripModificationsHttpFetcherImplTest {
    private HttpClient jsonHttpClient;
    private String httpPath;
    private String jsonHttpPath;
    private HttpClient protobufHttpClient;

    @Before
    public void setUp() throws Exception {
        Path jsonPath = Paths.get(getClass().getResource("trip-modifications.json").toURI());
        Path pbPath = Paths.get(getClass().getResource("trip-modifications.pb").toURI());

        jsonHttpClient = createHttpClient(jsonPath);
        protobufHttpClient = createHttpClient(pbPath);

        httpPath = "http://local/trip-modifications";
        jsonHttpPath = httpPath + "?format=json";
    }

    private HttpClient createHttpClient(Path path) throws IOException, InterruptedException {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<byte[]> response = mock(HttpResponse.class);

        byte[] outputAsBytes = Files.readAllBytes(path);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(outputAsBytes);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
        return httpClient;
    }

    @Test
    public void testJsonFormatDetection() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher =
                new GtfsTripModificationsFetcherImpl(jsonHttpPath, jsonHttpClient);

        assertEquals("Should default to JSON format with query parameter",
                TripModificationsFormat.JSON,
                fetcher.getTripModificationsFormat());
    }

    @Test
    public void testProtobufFormatDetectionDefault() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(httpPath, protobufHttpClient);

        assertEquals("Should default to PROTOBUF format without query parameter",
                TripModificationsFormat.PROTOBUF,
                fetcher.getTripModificationsFormat());
    }

    @Test
    public void testFetchJsonHttp() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(jsonHttpPath, jsonHttpClient);

        byte[] data = fetcher.fetchFeed();

        assertNotNull("Fetched data should not be null", data);
        assertTrue("Fetched data should not be empty", data.length > 0);

        String jsonContent = new String(data, StandardCharsets.UTF_8);
        assertTrue("Should contain header element", jsonContent.contains("\"header\""));
        assertTrue("Should contain entity element", jsonContent.contains("\"entity\""));
        assertTrue("Should contain gtfsRealtimeVersion", jsonContent.contains("\"gtfsRealtimeVersion\""));
    }

    @Test
    public void testFetchProtobufHttp() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(httpPath, protobufHttpClient);

        byte[] data = fetcher.fetchFeed();

        assertNotNull("Fetched data should not be null", data);
        assertTrue("Fetched data should not be empty", data.length > 0);
        assertTrue("Protobuf data should be binary", data.length > 10);
    }

    @Test
    public void testJsonContentStructure() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(jsonHttpPath, jsonHttpClient);

        byte[] data = fetcher.fetchFeed();
        String jsonContent = new String(data, StandardCharsets.UTF_8);

        assertTrue("Should contain alert data", jsonContent.contains("\"alert\""));
        assertTrue("Should contain trip modifications", jsonContent.contains("\"tripModifications\""));
        assertTrue("Should contain shape data", jsonContent.contains("\"shape\""));
        assertTrue("Should contain encoded polyline", jsonContent.contains("\"encodedPolyline\""));
        assertTrue("Should contain route S40", jsonContent.contains("\"S40\""));
        assertTrue("Should contain route S48", jsonContent.contains("\"S48\""));
    }

    @Test
    public void testJsonEntityStructure() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(jsonHttpPath, jsonHttpClient);

        byte[] data = fetcher.fetchFeed();
        String jsonContent = new String(data, StandardCharsets.UTF_8);

        assertTrue("Should contain entity id", jsonContent.contains("\"id\""));
        assertTrue("Should contain serviceDates", jsonContent.contains("\"serviceDates\""));
        assertTrue("Should contain selectedTrips", jsonContent.contains("\"selectedTrips\""));
        assertTrue("Should contain modifications", jsonContent.contains("\"modifications\""));
    }

    @Test
    public void testMultipleFormatParameters() throws Exception {
        String urlWithMultipleParams = httpPath + "?format=json&other=value";
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(urlWithMultipleParams, jsonHttpClient);

        assertEquals("Should detect JSON format with multiple query parameters",
                TripModificationsFormat.JSON,
                fetcher.getTripModificationsFormat());

        byte[] data = fetcher.fetchFeed();
        assertNotNull("Should still fetch data with multiple query parameters", data);
    }
}