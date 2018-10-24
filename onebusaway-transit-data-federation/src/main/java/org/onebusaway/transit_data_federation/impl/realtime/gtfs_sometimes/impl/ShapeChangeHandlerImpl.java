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
import com.camsys.transit.servicechange.field_descriptors.AbstractFieldDescriptor;
import com.camsys.transit.servicechange.field_descriptors.ShapesFields;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.AddShape;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model.ShapeChangeSet;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.ShapeChangeHandler;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Component
public class ShapeChangeHandlerImpl implements ShapeChangeHandler {

    private EntityIdService _entityIdService;

    private TransitGraphDao _dao;

    @Autowired
    public void setEntityIdService(EntityIdService entityIdService) {
        _entityIdService = entityIdService;
    }

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Override
    public ShapeChangeSet getAllShapeChanges(Collection<ServiceChange> changes) {
        ShapeChangeSet changeset = new ShapeChangeSet();

        Multimap<String, ShapesFields> shapesFieldsMap = ArrayListMultimap.create();
        for (ServiceChange change : changes) {
            if (Table.SHAPES.equals(change.getTable())
                    && ServiceChangeType.ADD.equals(change.getServiceChangeType())) {
                for (AbstractFieldDescriptor desc : change.getAffectedField()) {
                    if (desc instanceof ShapesFields) {
                        ShapesFields shapesFields = (ShapesFields) desc;
                        shapesFieldsMap.put(shapesFields.getShapeId(), shapesFields);
                    }
                }
            }
        }

        for (String id : shapesFieldsMap.keySet()) {
            List<ShapesFields> shapeList = new ArrayList<>(shapesFieldsMap.get(id));
            int nPoints = shapeList.size();
            double[] lat = new double[nPoints];
            double[] lon = new double[nPoints];
            double[] distTraveled = new double[nPoints];
            shapeList.sort(Comparator.comparingInt(ShapesFields::getShapePtSequence));
            int i = 0;
            for (ShapesFields fields : shapeList) {
                lat[i] = fields.getShapePtLat();
                lon[i] = fields.getShapePtLon();
                if (fields.getShapeDistTraveled() != null && fields.getShapeDistTraveled() != ShapePoint.MISSING_VALUE) {
                    distTraveled[i] = fields.getShapeDistTraveled();
                }
                i++;
            }
            ShapePoints shapePoints = new ShapePoints();
            shapePoints.setLats(lat);
            shapePoints.setLons(lon);
            shapePoints.setDistTraveled(distTraveled);
            AgencyAndId shapeId = _entityIdService.getShapeId(id);
            shapePoints.setShapeId(shapeId);
            shapePoints.ensureDistTraveled();
            changeset.addAddedShape(new AddShape(shapeId, shapePoints));
        }
        return changeset;
    }

    @Override
    public ShapeChangeSet handleShapeChanges(ShapeChangeSet changeset) {
        ShapeChangeSet revertSet = new ShapeChangeSet();
        for (AddShape addShape : changeset.getAddedShapes()) {
            if (handleShapeChange(addShape)) {
                revertSet.addDeletedShape(addShape.getShapeId());
            }
        }
        for (AgencyAndId shapeId : changeset.getDeletedShapes()) {
            AddShape addShape = getAddedShapeFromExistingShape(shapeId);
            revertSet.addAddedShape(addShape);
            _dao.removeShape(shapeId);
        }
        return revertSet;
    }

    private boolean handleShapeChange(AddShape addShape) {
        return _dao.addShape(addShape.getAddedShapePoints());
    }

    private AddShape getAddedShapeFromExistingShape(AgencyAndId shapeId) {
        ShapePoints shapePoints = _dao.getShape(shapeId);
        return new AddShape(shapeId, shapePoints);
    }
}
