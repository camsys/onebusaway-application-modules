package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsSometimesDummyClientImpl implements GtfsSometimesClient {

    private static final Logger _log = LoggerFactory.getLogger(GtfsSometimesDummyClientImpl.class);

    @Override
    public void update() {
        _log.info("Update called in dummy class");
    }
}
