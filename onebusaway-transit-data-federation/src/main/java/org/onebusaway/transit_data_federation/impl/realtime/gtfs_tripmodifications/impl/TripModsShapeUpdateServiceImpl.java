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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShape;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.AddedShapesResult;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.TripModsShapeUpdateService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TripModsShapeUpdateServiceImpl implements TripModsShapeUpdateService {

    private final TransitGraphDao _dao;

    private final Logger _log = LoggerFactory.getLogger(TripModsShapeUpdateServiceImpl.class);

    @Autowired
    public TripModsShapeUpdateServiceImpl(TransitGraphDao dao) {
        _dao = dao;
    }

    @Override
    public AddedShapesResult addShapes(List<AddedShape> addedShapeList) {
        AddedShapesResult result = new AddedShapesResult();
        for(AddedShape addedShape : addedShapeList){
            if(_dao.addShape(addedShape.getShapePoints())){
                result.addSuccessfullyUpdatedShapeId(addedShape.getShapeId());
            }
            else {
                result.addFailedUpdatedShapeId(addedShape.getShapeId());
                _log.warn("Unable to add shape " + addedShape.getShapeId());
            }
        }
        return result;
    }

    @Override
    public void removeShapes(List<AgencyAndId> shapeIds) {
        for(AgencyAndId shapeId : shapeIds){
            _dao.removeShape(shapeId);
        }
    }
}
