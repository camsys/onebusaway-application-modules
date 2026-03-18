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

import com.google.transit.realtime.GtfsRealtime;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.realtime.gtfsrt.util.GtfsRealtimeDeserializer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class GtfsTripModificationsDeserializerImplTest {

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
    public void testJsonDeserialization() throws Exception {
        GtfsRealtime.FeedMessage feedMessage = fetchAndParseAsFeedMessage(jsonFilePath);

        assertNotNull("FeedMessage should not be null", feedMessage);
        assertTrue("Feed header should be present", feedMessage.hasHeader());
        assertEquals("2.0", feedMessage.getHeader().getGtfsRealtimeVersion());

        assertEquals("Expected total entity count", 24, feedMessage.getEntityCount());

        Set<String> entityIds = new HashSet<>();
        int alertCount = 0;
        int shapeCount = 0;
        int tripModificationCount = 0;

        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            entityIds.add(entity.getId());

            if (entity.hasAlert()) {
                alertCount++;
            }
            if (entity.hasShape()) {
                shapeCount++;
            }
            if (entity.hasTripModifications()) {
                tripModificationCount++;
            }
        }

        assertEquals("Expected 6 alert entities", 6, alertCount);
        assertEquals("Expected 9 shape entities", 9, shapeCount);
        assertEquals("Expected 9 trip modifications entities", 9, tripModificationCount);

        assertTrue(entityIds.contains("4926a464-16b6-4259-a3cb-258a610f2a34"));
        assertTrue(entityIds.contains("638fbfd7-ad3f-4f14-8b97-64569e508141"));
        assertTrue(entityIds.contains("46c73d9a-ea65-4f5e-8728-4af279a6e0d8"));
        assertTrue(entityIds.contains("4f047e85-75be-41cc-bd42-dcc1817e4595"));
        assertTrue(entityIds.contains("16d97543-16ad-472a-ac9d-a5c833441c4c"));
        assertTrue(entityIds.contains("847b8342-e742-4f92-8b00-f58ef5f439bd"));
    }

    @Test
    public void testProtoBufDeserialization() throws Exception {
        GtfsRealtime.FeedMessage feedMessage = fetchAndParseAsFeedMessage(protobufFilePath);

        assertNotNull("FeedMessage should not be null", feedMessage);
        assertTrue("Feed header should be present", feedMessage.hasHeader());
        assertEquals("2.0", feedMessage.getHeader().getGtfsRealtimeVersion());
        assertEquals("Expected total entity count", 24, feedMessage.getEntityCount());
    }

    @Test
    public void testJsonAndProtobufDeserializationEqual() throws Exception {
        GtfsRealtime.FeedMessage jsonFeedMessage = fetchAndParseAsFeedMessage(jsonFilePath);
        GtfsRealtime.FeedMessage protobufFeedMessage = fetchAndParseAsFeedMessage(protobufFilePath);

        assertEquals(jsonFeedMessage, protobufFeedMessage);
    }

    @Test
    public void testRawProtobufParsing() throws Exception {
        GtfsTripModificationsFetcherImpl protobufFetcher =
                new GtfsTripModificationsFetcherImpl(protobufFilePath);

        byte[] protobufData = protobufFetcher.fetchFeed();

        assertNotNull("Fetched protobuf data should not be null", protobufData);
        assertTrue("Fetched protobuf data should not be empty", protobufData.length > 0);

        GtfsRealtime.FeedMessage protobufFeedMessage =
                GtfsRealtimeDeserializer.parseFeedMessageFromProtobuf(protobufData);

        assertNotNull("FeedMessage parsed directly from protobuf should not be null", protobufFeedMessage);
        assertTrue("Feed header should be present", protobufFeedMessage.hasHeader());
        assertEquals("2.0", protobufFeedMessage.getHeader().getGtfsRealtimeVersion());
        assertEquals("Expected total entity count", 24, protobufFeedMessage.getEntityCount());
    }

    @Test
    public void testSpecificTripModificationContent() throws Exception {
        GtfsRealtime.FeedMessage feedMessage = fetchAndParseAsFeedMessage(jsonFilePath);

        // Target the tripModifications entity for the "Longer Disruption test 4" (S54 detour).
        // This alert ID has exactly one tripModifications entity in the feed.
        GtfsRealtime.FeedEntity targetEntity = null;
        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if ("16d97543-16ad-472a-ac9d-a5c833441c4c".equals(entity.getId())
                    && entity.hasTripModifications()) {
                targetEntity = entity;
                break;
            }
        }

        assertNotNull("Expected trip modifications entity should exist", targetEntity);
        assertTrue("Entity should contain trip modifications", targetEntity.hasTripModifications());

        GtfsRealtime.TripModifications tripModifications = targetEntity.getTripModifications();

        assertEquals(1, tripModifications.getSelectedTripsCount());
        assertEquals(1, tripModifications.getServiceDatesCount());
        assertEquals(1, tripModifications.getModificationsCount());

        GtfsRealtime.TripModifications.SelectedTrips selectedTrips = tripModifications.getSelectedTrips(0);
        assertEquals(38, selectedTrips.getTripIdsCount());
        assertEquals("CA_P6-Weekday-032800_S54_120", selectedTrips.getTripIds(0));
        assertEquals("93dcddea-d1ea-425d-978d-69a23399fb53", selectedTrips.getShapeId());

        assertEquals("20260315", tripModifications.getServiceDates(0));

        GtfsRealtime.TripModifications.Modification modification = tripModifications.getModifications(0);
        assertEquals("201730", modification.getStartStopSelector().getStopId());
        assertEquals("201737", modification.getEndStopSelector().getStopId());
        assertEquals(155, modification.getPropagatedModificationDelay());
        assertEquals("16d97543-16ad-472a-ac9d-a5c833441c4c", modification.getServiceAlertId());
        assertEquals(3, modification.getReplacementStopsCount());
        assertEquals("200593", modification.getReplacementStops(0).getStopId());
        assertEquals(168, modification.getReplacementStops(0).getTravelTimeToStop());
        assertEquals("200591", modification.getReplacementStops(1).getStopId());
        assertEquals(246, modification.getReplacementStops(1).getTravelTimeToStop());
        assertEquals("200533", modification.getReplacementStops(2).getStopId());
        assertEquals(300, modification.getReplacementStops(2).getTravelTimeToStop());
    }

    private GtfsRealtime.FeedMessage fetchAndParseAsFeedMessage(String filePath) throws Exception {
        GtfsTripModificationsFetcherImpl fetcher = new GtfsTripModificationsFetcherImpl(filePath);

        byte[] data = fetcher.fetchFeed();

        assertNotNull("Fetched data should not be null", data);
        assertTrue("Fetched data should not be empty", data.length > 0);

        return GtfsRealtimeDeserializer.parseFeedMessage(data);
    }

}