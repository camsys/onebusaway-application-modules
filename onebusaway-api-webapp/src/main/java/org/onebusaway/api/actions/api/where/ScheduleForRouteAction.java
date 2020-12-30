/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.actions.api.where;

import java.util.Date;
import java.util.List;

import com.opensymphony.xwork2.conversion.annotations.TypeConversion;
import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.RouteScheduleBean;
import org.onebusaway.transit_data_federation.services.beans.RouteScheduleBeanService;
import org.onebusaway.util.AgencyAndIdLibrary;
import org.onebusaway.util.SystemTime;
import org.springframework.beans.factory.annotation.Autowired;


public class ScheduleForRouteAction extends ApiActionSupport {

    private static final long serialVersionUID = 1L;

    private static final int V2 = 2;

    @Autowired
    private RouteScheduleBeanService _service;

    private String _id;

    private Date _date = new Date(SystemTime.currentTimeMillis());

    public ScheduleForRouteAction() {
        super(V2);
    }

    @RequiredFieldValidator
    public void setId(String id) {
        _id = id;
    }

    public String getId() {
        return _id;
    }

    @TypeConversion(converter = "org.onebusaway.presentation.impl.conversion.DateConverter")
    public void setDate(Date date) {
        _date = date;
    }

    public DefaultHttpHeaders show() throws ServiceException {

        if (hasErrors())
            return setValidationErrorsResponse();

        AgencyAndId id = convertAgencyAndId(_id);
        ServiceDate serviceDate = new ServiceDate(_date);

        RouteScheduleBean routeSchedule = _service.getScheduledArrivalsForDate(id, serviceDate);

        BeanFactoryV2 factory = getBeanFactoryV2();
        return setOkResponse(factory.getResponse(routeSchedule));
    }

    /****
     * Private Methods
     ****/

    protected AgencyAndId convertAgencyAndId(String id) {
        return AgencyAndIdLibrary.convertFromString(id);
    }
}