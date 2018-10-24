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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service;

import com.camsys.transit.servicechange.ServiceChange;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ShapeChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.StopChangeSet;

import java.util.Collection;

public interface StopChangeHandler {

    /**
     * Read in GTFS-Sometimes model classes and return a StopChangeSet to apply.
     */
    StopChangeSet getAllStopChanges(Collection<ServiceChange> changes);

    /**
     * Apply a set of stop changes, returning a set that will revert the changes.
     *
     * @param changeset changes to apply
     * @return changes that would revert the supplied changes.
     */
    StopChangeSet handleStopChanges(StopChangeSet changeset);
}
