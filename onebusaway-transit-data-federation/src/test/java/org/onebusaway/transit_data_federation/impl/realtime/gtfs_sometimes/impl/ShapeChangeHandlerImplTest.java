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
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.AddShape;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.AddTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ModifyTrip;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ShapeChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.TripChangeSet;
import org.onebusaway.transit_data_federation.impl.transit_graph.TripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.dateDescriptors;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.serviceChange;
import static org.onebusaway.transit_data_federation.testing.ServiceChangeUnitTestingSupport.shapeFields;

import static org.mockito.Mockito.mock;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.aid;


public class ShapeChangeHandlerImplTest {

    ShapeChangeHandlerImpl handler;

    @Before
    public void setup() {
        handler = new ShapeChangeHandlerImpl();
        handler.setEntityIdService(new MockEntityIdServiceImpl());
        TransitGraphDao dao = mock(TransitGraphDao.class);
        when(dao.addShape(any())).thenReturn(true);
        handler.setTransitGraphDao(dao);
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
        ShapeChangeSet changeset = handler.getAllShapeChanges(Arrays.asList(change1, change2));
        assertTrue(changeset.getDeletedShapes().isEmpty());
        List<AddShape> addShapes = changeset.getAddedShapes();
        assertEquals(3, addShapes.size());
        addShapes.sort(Comparator.comparing(AddShape::getShapeId));
        assertEquals("1", addShapes.get(0).getShapeId().getId());
        assertEquals("2", addShapes.get(1).getShapeId().getId());
        assertEquals("3", addShapes.get(2).getShapeId().getId());
        assertEquals(3, addShapes.get(0).getAddedShapePoints().getSize());
        assertEquals(1, addShapes.get(1).getAddedShapePoints().getSize());
        assertEquals(1, addShapes.get(2).getAddedShapePoints().getSize());

        // revert
        ShapeChangeSet revertset = handler.handleShapeChanges(changeset);
        assertTrue(revertset.getAddedShapes().isEmpty());
        assertEquals(3, revertset.getDeletedShapes().size());
    }

    // Filter
    @Test
    public void testFilterShapes() {
        // Check null shape Ids don't break anything
        AddTrip badAdd = new AddTrip();
        badAdd.setTripEntry(new TripEntryImpl());
        AddTrip addTrip = new AddTrip();
        addTrip.setTripEntry(new TripEntryImpl());
        addTrip.getTripEntry().setShapeId(aid("1_shapeA"));
        ModifyTrip badMod = new ModifyTrip();
        ModifyTrip modTrip = new ModifyTrip();
        modTrip.setShapeId(aid("1_shapeB"));

        TripChangeSet trips = new TripChangeSet();
        trips.addAddedTrip(badAdd);
        trips.addAddedTrip(addTrip);
        trips.addModifiedTrip(modTrip);
        trips.addModifiedTrip(badMod);

        AddShape shapeA = new AddShape(aid("1_shapeA"), new ShapePoints());
        AddShape shapeB = new AddShape(aid("1_shapeB"), new ShapePoints());
        AddShape shapeC = new AddShape(aid("1_shapeC"), new ShapePoints());

        ShapeChangeSet shapes = new ShapeChangeSet();
        shapes.addAddedShape(shapeA);
        shapes.addAddedShape(shapeB);
        shapes.addAddedShape(shapeC);

        handler.filterShapeChanges(shapes, trips);

        assertEquals(Arrays.asList(shapeA, shapeB), shapes.getAddedShapes());
    }

}
