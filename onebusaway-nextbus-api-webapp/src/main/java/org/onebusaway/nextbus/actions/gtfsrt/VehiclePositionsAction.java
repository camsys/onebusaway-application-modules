package org.onebusaway.nextbus.actions.gtfsrt;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtimeConstants;
import com.opensymphony.xwork2.ModelDriven;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nextbus.actions.api.NextBusApiBase;
import org.onebusaway.nextbus.impl.gtfsrt.GtfsrtHelper;
import org.onebusaway.nextbus.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VehiclePositionsAction extends NextBusApiBase  implements
        ModelDriven<FeedMessage> {

    private static Logger _log = LoggerFactory.getLogger(VehiclePositionsAction.class);

    @Autowired
    private HttpUtil _httpUtil;

    private GtfsrtHelper _gtfsrtHelper = new GtfsrtHelper();

    public static final String VEHICLE_UPDATES_COMMAND = "/command/gtfs-rt/vehiclePositions";

    private String agencyId;

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public DefaultHttpHeaders index() {
        return new DefaultHttpHeaders("success");
    }

    @Override
    public FeedMessage getModel() {
        FeedMessage.Builder feedMessage = createFeedWithDefaultHeader();
        FeedMessage remoteFeedMessage = null;

        List<String> agencyIds = new ArrayList<String>();

        if (agencyId != null) {
            agencyIds.add(agencyId);
        } else {
            Map<String,List<CoordinateBounds>> agencies = _transitDataService.getAgencyIdsWithCoverageArea();
            agencyIds.addAll(agencies.keySet());
        }

        for(String agencyId : agencyIds){
            String gtfsrtUrl = getServiceUrl() + agencyId + VEHICLE_UPDATES_COMMAND;
            try{
                remoteFeedMessage = _httpUtil.getFeedMessage(gtfsrtUrl, 30);
                feedMessage.addAllEntity(remoteFeedMessage.getEntityList());
            }
            catch(Exception e){
                _log.error(e.getMessage());
            }
        }

        return feedMessage.build();
    }

    public FeedMessage.Builder createFeedWithDefaultHeader() {
        return _gtfsrtHelper.createFeedWithDefaultHeader();
    }
}