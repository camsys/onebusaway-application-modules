/**
 * Copyright (C) 2018 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes;

import com.camsys.transit.servicechange.Feed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class JsonFileGtfsSometimesSource implements GtfsSometimesSource {

    private String _filename;

    private static final Logger _log = LoggerFactory.getLogger(JsonFileGtfsSometimesSource.class);

    @Override
    public Feed getFeed() {
        File file = new File(_filename);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(file, Feed.class);
        } catch(IOException e) {
            _log.error("Exception processing file: {}", e);
            return null;
        }
    }

    public void setFilename(String filename) {
        _filename = filename;
    }

}
