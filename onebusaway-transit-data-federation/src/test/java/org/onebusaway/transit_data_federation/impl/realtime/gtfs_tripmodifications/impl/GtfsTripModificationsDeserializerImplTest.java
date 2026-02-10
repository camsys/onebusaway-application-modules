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

import com.google.protobuf.util.JsonFormat;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.GtfsTripModificationsDeserializer;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class GtfsTripModificationsDeserializerImplTest {
    private String jsonFilePath;
    private String protobufFilePath;
    private GtfsTripModificationsDeserializer deserializer;

    @Before
    public void setUp() throws Exception {
        Path jsonPath = Paths.get(getClass().getResource("trip-modifications.json").toURI());
        Path pbPath = Paths.get(getClass().getResource("trip-modifications.pb").toURI());
        jsonFilePath = jsonPath.toUri().toString();
        protobufFilePath = pbPath.toUri().toString();
        deserializer = new GtfsTripModificationsDeserializerImpl();
    }

    @Test
    public void testJsonDeserializationNotEmpty() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(jsonFilePath);

        byte[] data = fetcher.fetchFeed();

        assertNotNull("Fetched data should not be null", data);
        assertTrue("Fetched data should not be empty", data.length > 0);

        GtfsRealtime.FeedMessage feedMessage = deserializer.getFeedMessageFromJson(data);

        assertNotNull("FeedMessage should not be null", feedMessage);
    }

    @Test
    public void testProtoBufDeserializationNotEmpty() throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(protobufFilePath);

        byte[] data = fetcher.fetchFeed();

        assertNotNull("Fetched data should not be null", data);
        assertTrue("Fetched data should not be empty", data.length > 0);

        GtfsRealtime.FeedMessage feedMessage = deserializer.getFeedMessageFromProtobuf(data);

        assertNotNull("FeedMessage should not be null", feedMessage);
    }

    @Test
    public void testJsonProtobufDeserializationEqual() throws Exception {
        GtfsTripModificationsFetcherImpl jsonFetcher = new GtfsTripModificationsFetcherImpl(jsonFilePath);
        byte[] jsonData = jsonFetcher.fetchFeed();
        GtfsRealtime.FeedMessage jsonFeedMessage = deserializer.getFeedMessageFromJson(jsonData);

        GtfsTripModificationsFetcherImpl protobufFetcher = new GtfsTripModificationsFetcherImpl(protobufFilePath);
        byte[] protobufData = protobufFetcher.fetchFeed();
        GtfsRealtime.FeedMessage protobufFeedMessage = deserializer.getFeedMessageFromProtobuf(protobufData);

        assertEquals(jsonFeedMessage, protobufFeedMessage);
    }



}
