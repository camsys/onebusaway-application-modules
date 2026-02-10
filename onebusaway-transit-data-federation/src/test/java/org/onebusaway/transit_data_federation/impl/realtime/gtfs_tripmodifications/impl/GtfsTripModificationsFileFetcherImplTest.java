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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class GtfsTripModificationsFileFetcherImplTest {
    private String jsonFilePath;
    private String protobufFilePath;

    @Before
    public void setUp() throws Exception {
        Path jsonPath = Paths.get(getClass().getResource("trip-modifications.json").toURI());
        Path pbPath = Paths.get(getClass().getResource("trip-modifications.pb").toURI());

        jsonFilePath = jsonPath.toUri().toString();
        protobufFilePath = pbPath.toUri().toString();
    }


    @Test
    public void testFetchJsonFile() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(jsonFilePath);

        byte[] data = fetcher.fetchFeed();

        assertNotNull("Fetched data should not be null", data);
        assertTrue("Fetched data should not be empty", data.length > 0);

        // Verify JSON structure
        String jsonContent = new String(data, StandardCharsets.UTF_8);
        assertTrue("Should contain header element", jsonContent.contains("\"header\""));
        assertTrue("Should contain entity element", jsonContent.contains("\"entity\""));
        assertTrue("Should contain gtfsRealtimeVersion", jsonContent.contains("\"gtfsRealtimeVersion\""));
    }

    @Test
    public void testFetchProtobufFile() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(protobufFilePath);

        byte[] data = fetcher.fetchFeed();

        assertNotNull("Fetched data should not be null", data);
        assertTrue("Fetched data should not be empty", data.length > 0);
        assertTrue("Protobuf data should be binary", data.length > 10);
    }


    @Test
    public void testProtobufFormatDetectionDefault() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(protobufFilePath);

        assertEquals("Should default to PROTOBUF format without query parameter",
                TripModificationsFormat.PROTOBUF,
                fetcher.getTripModificationsFormat());
    }


    @Test
    public void testJsonContentStructure() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(jsonFilePath);

        byte[] data = fetcher.fetchFeed();
        String jsonContent = new String(data, StandardCharsets.UTF_8);

        assertTrue("Should contain alert data", jsonContent.contains("\"alert\""));
        assertTrue("Should contain trip modifications", jsonContent.contains("\"tripModifications\""));
        assertTrue("Should contain shape data", jsonContent.contains("\"shape\""));
        assertTrue("Should contain encoded polyline", jsonContent.contains("\"encodedPolyline\""));
        assertTrue("Should contain route S40", jsonContent.contains("\"S40\""));
        assertTrue("Should contain route S61", jsonContent.contains("\"S61\""));
    }

    @Test
    public void testJsonEntityStructure() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(jsonFilePath);

        byte[] data = fetcher.fetchFeed();
        String jsonContent = new String(data, StandardCharsets.UTF_8);

        assertTrue("Should contain entity id", jsonContent.contains("\"id\""));
        assertTrue("Should contain isDeleted field", jsonContent.contains("\"isDeleted\""));
        assertTrue("Should contain serviceDates", jsonContent.contains("\"serviceDates\""));
        assertTrue("Should contain selectedTrips", jsonContent.contains("\"selectedTrips\""));
        assertTrue("Should contain modifications", jsonContent.contains("\"modifications\""));
    }


    @Test
    public void testFileNotFound() throws Exception {
        String nonExistentFile = "file:///path/to/nonexistent/file.json";
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(nonExistentFile);

        try {
            fetcher.fetchFeed();
            fail("Should throw IOException for non-existent file");
        } catch (IOException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

}