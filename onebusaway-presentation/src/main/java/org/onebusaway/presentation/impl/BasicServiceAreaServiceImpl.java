package org.onebusaway.presentation.impl;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Qualifier("BasicServiceAreaService")
@Component
public class BasicServiceAreaServiceImpl extends ServiceAreaServiceImpl {

    @Override
    public CoordinateBounds getServiceArea(){
        return getDefaultBounds();
    }

}
