package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import com.google.transit.realtime.GtfsRealtime.Stop;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.EAccessibility;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.service.StopHandler;
import org.onebusaway.transit_data_federation.impl.transit_graph.StopEntryImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.TransitGraphImpl;
import org.onebusaway.transit_data_federation.model.narrative.StopNarrative;
import org.onebusaway.transit_data_federation.services.EntityIdService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class StopHandlerImpl implements StopHandler {

    private Logger _log = LoggerFactory.getLogger(StopHandlerImpl.class);

    private EntityIdService _entityIdService;

    private TransitGraphDao _dao;

    private NarrativeService _narrativeService;

    @Autowired
    public void setEntityIdService(EntityIdService entityIdService) {
        _entityIdService = entityIdService;
    }

    @Autowired
    public void setTransitGraphDao(TransitGraphDao dao) {
        _dao = dao;
    }

    @Autowired
    public void setNarrativeService(NarrativeService narrativeService) {
        _narrativeService = narrativeService;
    }


    @Override
    public List<StopEntry> addStops(List<Stop> changeset) {

        List<StopEntry> stopEntryList = new ArrayList<>();

        for (Stop stop : changeset) {
            if (stop.getStopId() == null || stop.getStopId().isEmpty()) {
                _log.warn("Stop with empty id found, skipping addition.");
                continue;
            }

            if (!stop.hasStopLat() && !stop.hasStopLon()) {
                _log.warn("Stop with id {} missing lat/lon, skipping addition.", stop.getStopId());
                continue;
            }

            if (!stop.hasWheelchairBoarding()) {
                _log.warn("Stop with id {} missing wheelchair boarding info, skipping addition.", stop.getStopId());
                continue;
            }

            //TODO figure out agencyID
            AgencyAndId stopId = new AgencyAndId("MTA", stop.getStopId());

            if (_dao.getStopEntryForId(stopId) != null) {
                _log.info("Stop with id {} already exists, skipping addition.", stop.getStopId());
                continue;
            }

            StopEntryImpl stopEntry = new StopEntryImpl(stopId, stop.getStopLat(), stop.getStopLon());
            stopEntry.setIndex(getLatestStopIndex());
            stopEntry.setWheelchairBoarding(getWheelchairBoardingAccessibilityForStop(stop));

            _narrativeService.addStop(stopId, newStopNarrative(stop));
            _dao.addStopEntry(stopEntry);
            stopEntryList.add(stopEntry);
        }

        return stopEntryList;
    }

    private int getLatestStopIndex() {
        return _dao.getAllStops().size();
    }

    private StopNarrative newStopNarrative(Stop stop) {
        StopNarrative.Builder builder = StopNarrative.builder();
        //TODO handle multiple translations
        builder.setName(stop.getStopName().getTranslation(0).getText());
        builder.setCode(stop.getStopCode().getTranslation(0).getText());
        builder.setDescription(stop.getStopDesc().getTranslation(0).getText());
        //TODO stop direction? location type"
        builder.setDirection("direction?");
        builder.setLocationType(0);
        builder.setUrl(stop.getStopUrl().getTranslation(0).getText());
        return builder.create();
    }

    private EAccessibility getWheelchairBoardingAccessibilityForStop(Stop stop) {
        switch (stop.getWheelchairBoarding().getNumber()) {
            case 1:
                return EAccessibility.ACCESSIBLE;
            case 2:
                return EAccessibility.NOT_ACCESSIBLE;
            default:
                return EAccessibility.UNKNOWN;
        }
    }
}
