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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl;

import com.camsys.transit.servicechange.ServiceChange;
import com.camsys.transit.servicechange.ServiceChangeType;
import com.camsys.transit.servicechange.Table;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.transit_data_federation.impl.MockEntityIdServiceImpl;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ShapeChange;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.dateDescriptors;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.serviceChange;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.shapeFields;

public class ShapeChangeHandlerImplTest {

    ShapeChangeHandlerImpl handler;

    @Before
    public void setup() {
        handler = new ShapeChangeHandlerImpl();
        handler.setEntityIdService(new MockEntityIdServiceImpl());
    }

    // Shape change

    @Test
    public void shapeChangeTest() {
        ServiceChange change1 = serviceChange(Table.SHAPES,
                ServiceChangeType.ADD,
                null,
                Arrays.asList(shapeFields("1", 0, 0, 1),
                        shapeFields("2", 0, 0, 1),
                        shapeFields("1", 0, 0, 2)),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        ServiceChange change2 = serviceChange(Table.SHAPES,
                ServiceChangeType.ADD,
                null,
                Arrays.asList(shapeFields("1", 0, 0, 3),
                        shapeFields("3", 0, 0, 1)),
                dateDescriptors(LocalDate.of(2018, 6, 1), LocalDate.of(2018, 7, 1)));
        List<ShapeChange> shapeChanges = handler.getAllShapeChanges(Arrays.asList(change1, change2)).getShapeChanges();
        assertEquals(3, shapeChanges.size());
        shapeChanges.sort(Comparator.comparing(ShapeChange::getShapeId));
        assertEquals("1", shapeChanges.get(0).getShapeId().getId());
        assertEquals("2", shapeChanges.get(1).getShapeId().getId());
        assertEquals("3", shapeChanges.get(2).getShapeId().getId());
        assertEquals(3, shapeChanges.get(0).getAddedShapePoints().getSize());
        assertEquals(1, shapeChanges.get(1).getAddedShapePoints().getSize());
        assertEquals(1, shapeChanges.get(2).getAddedShapePoints().getSize());
    }

}
