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
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.GtfsTripModificationsDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class GtfsTripModificationsDeserializerImpl implements GtfsTripModificationsDeserializer {
    Logger _log = LoggerFactory.getLogger(GtfsTripModificationsDeserializerImpl.class);

    @Override
    public GtfsRealtime.FeedMessage getFeedMessageFromJson(byte[] message) throws IOException {
        try {
            String jsonString = new String(message, StandardCharsets.UTF_8);
            GtfsRealtime.FeedMessage.Builder builder = GtfsRealtime.FeedMessage.newBuilder();
            JsonFormat.parser()
                    .ignoringUnknownFields()
                    .merge(jsonString, builder);

            _log.debug("Successfully parsed as JSON");
            return builder.build();
        } catch (Exception e) {
            throw new IOException("Failed to parse as JSON", e);
        }
    }

    @Override
    public GtfsRealtime.FeedMessage getFeedMessageFromProtobuf(byte[] message) throws IOException {
        try {
            GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom(message);
            _log.debug("Successfully parsed as binary protobuf");
            return feed;
        } catch (Exception e) {
            throw new IOException("Failed to parse as protobuf", e);
        }
    }
}
