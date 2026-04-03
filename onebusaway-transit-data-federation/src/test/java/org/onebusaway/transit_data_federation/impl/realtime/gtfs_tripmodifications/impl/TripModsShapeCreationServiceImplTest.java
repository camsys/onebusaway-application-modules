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

import com.google.transit.realtime.GtfsRealtime.Shape;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShape;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShapes;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TripModsShapeCreationServiceImplTest {

    // A minimal valid encoded polyline (encodes a single point at ~0,0)
    private static final String VALID_POLYLINE = "_ibE_seK";

    @Mock
    private EntityIdService entityIdService;

    @Mock
    private TransitGraphDao transitGraphDao;

    @InjectMocks
    private TripModsShapeCreationServiceImpl service;

    private AgencyAndId shapeAgencyAndId;

    private String entityId;

    @Before
    public void setUp() {
        shapeAgencyAndId = new AgencyAndId("MTA", "SHAPE_1");
        entityId = UUID.randomUUID().toString();
        when(entityIdService.getShapeId(anyString())).thenReturn(shapeAgencyAndId);
        when(transitGraphDao.getShape(any())).thenReturn(null); // shape does not exist by default
    }

    // -------------------------------------------------------------------------
    // createAddedShapes — happy path
    // -------------------------------------------------------------------------

    @Test
    public void createAddedShapes_returnsAddedShapeForValidNewShape() {
        Shape shape = validShape("SHAPE_1");

        AddedShapes result = service.createAddedShapes(Collections.singletonMap(entityId, shape));

        assertEquals(1, result.getAddedShapes().size());
        assertTrue(result.getFailedAddedShapeIds().isEmpty());
    }

    @Test
    public void createAddedShapes_shapeIdIsSetOnReturnedShapePoints() {
        Shape shape = validShape("SHAPE_1");

        AddedShapes result = service.createAddedShapes(Collections.singletonMap(entityId, shape));

        AddedShape addedShape = result.getAddedShapes().get(0);
        assertEquals(shapeAgencyAndId, addedShape.getShapeId());
        assertEquals(shapeAgencyAndId, addedShape.getShapePoints().getShapeId());
    }

    @Test
    public void createAddedShapes_shapePointsContainDecodedCoordinates() {
        Shape shape = validShape("SHAPE_1");

        AddedShapes result = service.createAddedShapes(Collections.singletonMap(entityId, shape));

        ShapePoints sp = result.getAddedShapes().get(0).getShapePoints();
        assertNotNull(sp.getLats());
        assertNotNull(sp.getLons());
        assertTrue(sp.getLats().length > 0);
        assertEquals(sp.getLats().length, sp.getLons().length);
    }

    @Test
    public void createAddedShapes_distTraveledIsPopulated() {
        Shape shape = validShape("SHAPE_1");

        AddedShapes result = service.createAddedShapes(Collections.singletonMap(entityId, shape));

        double[] distTraveled = result.getAddedShapes().get(0).getShapePoints().getDistTraveled();
        assertNotNull(distTraveled);
        // ensureDistTraveled() fills first entry as 0
        assertEquals(0.0, distTraveled[0], 0.0001);
    }

    @Test
    public void createAddedShapes_processesMultipleValidShapes() {
        AgencyAndId id2 = new AgencyAndId("MTA", "SHAPE_2");
        when(entityIdService.getShapeId("SHAPE_1")).thenReturn(shapeAgencyAndId);
        when(entityIdService.getShapeId("SHAPE_2")).thenReturn(id2);

        Map<String, Shape> shapes = new HashMap<>();
        shapes.put("SHAPE_1", validShape("SHAPE_1"));
        shapes.put("SHAPE_2", validShape("SHAPE_2"));

        AddedShapes result = service.createAddedShapes(shapes);

        assertEquals(2, result.getAddedShapes().size());
        assertTrue(result.getFailedAddedShapeIds().isEmpty());
    }

    @Test
    public void createAddedShapes_returnsEmptyResultForEmptyInput() {
        AddedShapes result = service.createAddedShapes(Collections.emptyMap());

        assertTrue(result.getAddedShapes().isEmpty());
        assertTrue(result.getFailedAddedShapeIds().isEmpty());
    }

    // -------------------------------------------------------------------------
    // createAddedShapes — failure cases (invalid or duplicate shapes)
    // -------------------------------------------------------------------------

    @Test
    public void createAddedShapes_addsToFailedWhenShapeIdIsEmpty() {
        Shape shape = Shape.newBuilder()
                .setShapeId("")
                .setEncodedPolyline(VALID_POLYLINE)
                .build();

        AddedShapes result = service.createAddedShapes(Collections.singletonMap(entityId, shape));

        assertTrue(result.getAddedShapes().isEmpty());
        assertEquals(1, result.getFailedAddedShapeIds().size());
        assertTrue(result.getFailedAddedShapeIds().contains("missing_shapeId"));
    }

    @Test
    public void createAddedShapes_addsToFailedWhenShapeIdIsBlank() {
        Shape shape = Shape.newBuilder()
                .setShapeId("   ")
                .setEncodedPolyline(VALID_POLYLINE)
                .build();

        AddedShapes result = service.createAddedShapes(Collections.singletonMap(entityId, shape));

        assertTrue(result.getAddedShapes().isEmpty());
        assertEquals(1, result.getFailedAddedShapeIds().size());
    }

    @Test
    public void createAddedShapes_addsToFailedWhenPolylineIsMissing() {
        Shape shape = Shape.newBuilder()
                .setShapeId("SHAPE_1")
                // no encoded polyline set
                .build();

        AddedShapes result = service.createAddedShapes(Collections.singletonMap(entityId, shape));

        assertTrue(result.getAddedShapes().isEmpty());
        assertTrue(result.getFailedAddedShapeIds().contains("SHAPE_1"));
    }

    @Test
    public void createAddedShapes_addsToFailedWhenShapeAlreadyExistsInTransitGraph() {
        when(transitGraphDao.getShape(shapeAgencyAndId)).thenReturn(new ShapePoints());

        AddedShapes result = service.createAddedShapes(Collections.singletonMap(entityId, validShape("SHAPE_1")));

        assertTrue(result.getAddedShapes().isEmpty());
        assertTrue(result.getFailedAddedShapeIds().contains("SHAPE_1"));
    }

    @Test
    public void createAddedShapes_continuesProcessingAfterFailedShape() {
        // First shape is invalid (no polyline), second is valid
        Shape invalid = Shape.newBuilder().setShapeId("SHAPE_BAD").build();
        Shape valid = validShape("SHAPE_1");
        when(entityIdService.getShapeId("SHAPE_1")).thenReturn(shapeAgencyAndId);

        Map<String, Shape> shapes = new HashMap<>();
        shapes.put("SHAPE_BAD",invalid);
        shapes.put("SHAPE_1", valid);

        AddedShapes result = service.createAddedShapes(shapes);

        assertEquals(1, result.getAddedShapes().size());
        assertEquals(1, result.getFailedAddedShapeIds().size());
        assertTrue(result.getFailedAddedShapeIds().contains("SHAPE_BAD"));
    }

    @Test
    public void createAddedShapes_doesNotCallEntityIdServiceForInvalidShape() {
        Shape shape = Shape.newBuilder().setShapeId("").build();

        service.createAddedShapes(Collections.singletonMap(entityId, shape));

        verify(entityIdService, never()).getShapeId(anyString());
    }

    @Test
    public void createAddedShapes_doesNotCallTransitGraphForInvalidShape() {
        Shape shape = Shape.newBuilder().setShapeId("SHAPE_1").build(); // no polyline

        service.createAddedShapes(Collections.singletonMap(entityId, shape));

        verify(transitGraphDao, never()).getShape(any());
    }

    // -------------------------------------------------------------------------
    // isValidShape
    // -------------------------------------------------------------------------

    @Test
    public void isValidShape_returnsTrueForShapeWithIdAndPolyline() {
        assertTrue(service.isValidShape(validShape("SHAPE_1")));
    }

    @Test
    public void isValidShape_returnsFalseForNullShapeId() {
        // Protobuf does not allow null for string fields; the closest is an absent/empty field
        Shape shape = Shape.newBuilder().setEncodedPolyline(VALID_POLYLINE).build();
        assertFalse(service.isValidShape(shape));
    }

    @Test
    public void isValidShape_returnsFalseForBlankShapeId() {
        Shape shape = Shape.newBuilder().setShapeId("  ").setEncodedPolyline(VALID_POLYLINE).build();
        assertFalse(service.isValidShape(shape));
    }

    @Test
    public void isValidShape_returnsFalseWhenPolylineAbsent() {
        Shape shape = Shape.newBuilder().setShapeId("SHAPE_1").build();
        assertFalse(service.isValidShape(shape));
    }

    // -------------------------------------------------------------------------
    // shapeAlreadyExists
    // -------------------------------------------------------------------------

    @Test
    public void shapeAlreadyExists_returnsFalseWhenTransitGraphReturnsNull() {
        when(transitGraphDao.getShape(shapeAgencyAndId)).thenReturn(null);
        assertFalse(service.shapeAlreadyExists(shapeAgencyAndId));
    }

    @Test
    public void shapeAlreadyExists_returnsTrueWhenTransitGraphReturnsShapePoints() {
        when(transitGraphDao.getShape(shapeAgencyAndId)).thenReturn(new ShapePoints());
        assertTrue(service.shapeAlreadyExists(shapeAgencyAndId));
    }

    // -------------------------------------------------------------------------
    // createShapePoints
    // -------------------------------------------------------------------------

    @Test
    public void createShapePoints_setsShapeId() {
        Shape shape = validShape("SHAPE_1");
        ShapePoints result = service.createShapePoints(shape, shapeAgencyAndId);
        assertEquals(shapeAgencyAndId, result.getShapeId());
    }

    @Test
    public void createShapePoints_latAndLonArraysHaveSameLength() {
        Shape shape = validShape("SHAPE_1");
        ShapePoints result = service.createShapePoints(shape, shapeAgencyAndId);
        assertEquals(result.getLats().length, result.getLons().length);
    }

    @Test
    public void createShapePoints_distTraveledLengthMatchesPoints() {
        Shape shape = validShape("SHAPE_1");
        ShapePoints result = service.createShapePoints(shape, shapeAgencyAndId);
        assertEquals(result.getLats().length, result.getDistTraveled().length);
    }

    @Test
    public void createShapePoints_distTraveledIsNonDecreasing() {
        // A polyline with multiple points to verify ensureDistTraveled() ordering
        Shape shape = Shape.newBuilder()
                .setShapeId("SHAPE_1")
                .setEncodedPolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@") // 3-point polyline
                .build();

        ShapePoints result = service.createShapePoints(shape, shapeAgencyAndId);

        double[] dist = result.getDistTraveled();
        for (int i = 1; i < dist.length; i++) {
            assertTrue("distTraveled must be non-decreasing", dist[i] >= dist[i - 1]);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Shape validShape(String shapeId) {
        return Shape.newBuilder()
                .setShapeId(shapeId)
                .setEncodedPolyline(VALID_POLYLINE)
                .build();
    }
}