/**
 * Copyright (C) 2010 OpenPlans
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
package org.onebusaway.presentation.impl.realtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.presentation.impl.realtime.SiriSupport.OnwardCallsMode;
import org.onebusaway.presentation.services.realtime.PresentationService;
import org.onebusaway.presentation.services.realtime.RealtimeService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.transit_data_federation.siri.SiriJsonSerializer;
import org.onebusaway.transit_data_federation.siri.SiriXmlSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;

/**
 * A source of SIRI classes containing real time data, subject to the conventions expressed
 * in the PresentationService.
 * 
 * @author jmaki
 *
 */
@Component
public class RealtimeServiceImpl implements RealtimeService {

  private TransitDataService _transitDataService;;

  private PresentationService _presentationService;
  
  private SiriXmlSerializer _siriXmlSerializer = new SiriXmlSerializer();

  private SiriJsonSerializer _siriJsonSerializer = new SiriJsonSerializer();

  private Long _now = null;
  
  @Override
  public void setTime(long time) {
    _now = time;
    _presentationService.setTime(time);
  }

  public long getTime() {
    if(_now != null)
      return _now;
    else
      return System.currentTimeMillis();
  }

  @Autowired
  public void setTransitDataService(TransitDataService transitDataService) {
    _transitDataService = transitDataService;
  }

  @Autowired
  public void setPresentationService(PresentationService presentationService) {
    _presentationService = presentationService;
  }
  
  @Override
  public PresentationService getPresentationService() {
    return _presentationService;
  }

  @Override
  public SiriJsonSerializer getSiriJsonSerializer() {
    return _siriJsonSerializer;
  }
  
  @Override
  public SiriXmlSerializer getSiriXmlSerializer() {
    return _siriXmlSerializer;
  }

  /**
   * SIRI METHODS
   */
  
  @Override
  public List<VehicleActivityStructure> getVehicleActivityForRoute(String routeId, String directionId, int maximumOnwardCalls, long currentTime) {
    List<VehicleActivityStructure> output = new ArrayList<VehicleActivityStructure>();
        
    ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId, currentTime);
    for(TripDetailsBean tripDetails : trips.getList()) {
      
      // filter out interlined routes
      if(routeId != null && !tripDetails.getTrip().getRoute().getId().equals(routeId))
        continue;

      // filtered out by user
      if(directionId != null && !tripDetails.getTrip().getDirectionId().equals(directionId))
        continue;
      
      if(!_presentationService.include(tripDetails.getStatus()))
        continue;
        
      VehicleActivityStructure activity = new VehicleActivityStructure();
      activity.setRecordedAtTime(new Date(tripDetails.getStatus().getLastUpdateTime()));

      List<TimepointPredictionRecord> timePredictionRecords = null;
	  timePredictionRecords = _transitDataService.getPredictionRecordsForTrip(AgencyAndId.convertFromString(routeId).getAgencyId(), tripDetails.getStatus());
      activity.setMonitoredVehicleJourney(new MonitoredVehicleJourney());  
      SiriSupport.fillMonitoredVehicleJourney(activity.getMonitoredVehicleJourney(), 
          tripDetails.getTrip(), tripDetails.getStatus(), null, OnwardCallsMode.VEHICLE_MONITORING,
          _presentationService, _transitDataService, maximumOnwardCalls, 
          timePredictionRecords, currentTime);
            
      output.add(activity);
    }

    Collections.sort(output, new Comparator<VehicleActivityStructure>() {
      public int compare(VehicleActivityStructure arg0, VehicleActivityStructure arg1) {
        try {
          SiriExtensionWrapper wrapper0 = (SiriExtensionWrapper)arg0.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
          SiriExtensionWrapper wrapper1 = (SiriExtensionWrapper)arg1.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
          return wrapper0.getDistances().getDistanceFromCall().compareTo(wrapper1.getDistances().getDistanceFromCall());        
        } catch(Exception e) {
          return -1;
        }
      }
    });
    
    return output;
  }

  @Override
  public VehicleActivityStructure getVehicleActivityForVehicle(String vehicleId, int maximumOnwardCalls, long currentTime) {    
  
	TripForVehicleQueryBean query = new TripForVehicleQueryBean();
    query.setTime(new Date(currentTime));
    query.setVehicleId(vehicleId);

    TripDetailsInclusionBean inclusion = new TripDetailsInclusionBean();
    inclusion.setIncludeTripStatus(true);
    inclusion.setIncludeTripBean(true);
    query.setInclusion(inclusion);

    TripDetailsBean tripDetailsForCurrentTrip = _transitDataService.getTripDetailsForVehicleAndTime(query);
    if (tripDetailsForCurrentTrip != null) {
      if(!_presentationService.include(tripDetailsForCurrentTrip.getStatus()))
        return null;
      
      VehicleActivityStructure output = new VehicleActivityStructure();
      output.setRecordedAtTime(new Date(tripDetailsForCurrentTrip.getStatus().getLastUpdateTime()));

      List<TimepointPredictionRecord> timePredictionRecords = null;
      timePredictionRecords = _transitDataService.getPredictionRecordsForTrip(AgencyAndId.convertFromString(vehicleId).getAgencyId(), tripDetailsForCurrentTrip.getStatus());
      
      output.setMonitoredVehicleJourney(new MonitoredVehicleJourney());
      SiriSupport.fillMonitoredVehicleJourney(output.getMonitoredVehicleJourney(), 
    	  tripDetailsForCurrentTrip.getTrip(), tripDetailsForCurrentTrip.getStatus(), null, OnwardCallsMode.VEHICLE_MONITORING,
    	  _presentationService, _transitDataService, maximumOnwardCalls,
    	  timePredictionRecords, currentTime);

      return output;
    }
    
    return null;
  }

  @Override
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, int maximumOnwardCalls, long currentTime) {
    List<MonitoredStopVisitStructure> output = new ArrayList<MonitoredStopVisitStructure>();

    for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId, currentTime)) {
      TripStatusBean statusBeanForCurrentTrip = adBean.getTripStatus();
      TripBean tripBeanForAd = adBean.getTrip();

      if(statusBeanForCurrentTrip == null)
    	  continue;

      if(!_presentationService.include(statusBeanForCurrentTrip) || !_presentationService.include(adBean, statusBeanForCurrentTrip))
          continue;

      MonitoredStopVisitStructure stopVisit = new MonitoredStopVisitStructure();
      stopVisit.setRecordedAtTime(new Date(statusBeanForCurrentTrip.getLastUpdateTime()));
        
      List<TimepointPredictionRecord> timePredictionRecords = null;
      timePredictionRecords = createTimePredictionRecordsForStop(adBean, stopId);
      stopVisit.setMonitoredVehicleJourney(new MonitoredVehicleJourneyStructure());
      SiriSupport.fillMonitoredVehicleJourney(stopVisit.getMonitoredVehicleJourney(), 
    	  tripBeanForAd, statusBeanForCurrentTrip, adBean.getStop(), OnwardCallsMode.STOP_MONITORING,
    	  _presentationService, _transitDataService, maximumOnwardCalls,
    	  timePredictionRecords, currentTime);

      output.add(stopVisit);
    }
    
    Collections.sort(output, new Comparator<MonitoredStopVisitStructure>() {
      public int compare(MonitoredStopVisitStructure arg0, MonitoredStopVisitStructure arg1) {
        try {
          SiriExtensionWrapper wrapper0 = (SiriExtensionWrapper)arg0.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
          SiriExtensionWrapper wrapper1 = (SiriExtensionWrapper)arg1.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
          return wrapper0.getDistances().getDistanceFromCall().compareTo(wrapper1.getDistances().getDistanceFromCall());
        } catch(Exception e) {
          return -1;
        }
      }
    });
    
    return output;
  }
  
  private List<TimepointPredictionRecord> createTimePredictionRecordsForStop(
      ArrivalAndDepartureBean adBean, String stopId) {

    List<TimepointPredictionRecord> tprs = new ArrayList<TimepointPredictionRecord>();
    TimepointPredictionRecord tpr = new TimepointPredictionRecord();
    tpr.setTimepointId(AgencyAndIdLibrary.convertFromString(stopId));
    tpr.setTimepointScheduledTime(adBean.getScheduledArrivalTime());
    tpr.setTimepointPredictedTime(adBean.getPredictedArrivalTime());
    tprs.add(tpr);
    return tprs;
  }

  /**
   * CURRENT IN-SERVICE VEHICLE STATUS FOR ROUTE
   */

  /**
   * Returns true if there are vehicles in service for given route+direction
   */
  
  @Override
  public boolean getVehiclesInServiceForRoute(String routeId, String directionId, long currentTime) {
	  ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId, currentTime);
	  for(TripDetailsBean tripDetails : trips.getList()) {
		  // filter out interlined routes
		  if(routeId != null && !tripDetails.getTrip().getRoute().getId().equals(routeId))
			  continue;

		  // filtered out by user
		  if(directionId != null && !tripDetails.getTrip().getDirectionId().equals(directionId))
			  continue;

		  if(!_presentationService.include(tripDetails.getStatus()))
			  continue;

		  return true;
	  } 

	  return false;
  }

  /**
   * Returns true if there are vehicles in service for given route+direction that will stop
   * at the indicated stop in the future.
   */
  
  @Override
  public boolean getVehiclesInServiceForStopAndRoute(String stopId, String routeId, long currentTime) {
	  for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId, currentTime)) {
		  TripStatusBean statusBean = adBean.getTripStatus();
		  if(!_presentationService.include(statusBean) || !_presentationService.include(adBean, statusBean))
			  continue;

		  // filtered out by user
		  if(routeId != null && !adBean.getTrip().getRoute().getId().equals(routeId))
			  continue;

		  return true;
	  }

	  return false;
  }
  
  /**
   * SERVICE ALERTS METHODS
   */
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsForRoute(String routeId) {
    return getServiceAlertsForRouteAndDirection(routeId, null); 
  }
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId) {
    SituationQueryBean query = new SituationQueryBean();
    SituationQueryBean.AffectsBean affects = new SituationQueryBean.AffectsBean();
    query.getAffects().add(affects);

    affects.setRouteId(routeId);
    if (directionId != null) {
      affects.setDirectionId(directionId);
    } else { 
      /*
       * TODO
       * The route index is not currently being populated correctly; query by route and direction,
       * and supply both directions if not present
       */
  
      SituationQueryBean.AffectsBean affects1 = new SituationQueryBean.AffectsBean();
      query.getAffects().add(affects1);
      affects1.setRouteId(routeId);
      affects1.setDirectionId("0");
      SituationQueryBean.AffectsBean affects2 = new SituationQueryBean.AffectsBean();
      query.getAffects().add(affects2);
      affects2.setRouteId(routeId);
      affects2.setDirectionId("1");
    }
    
    ListBean<ServiceAlertBean> serviceAlerts = _transitDataService.getServiceAlerts(query);
    return serviceAlerts.getList();
  }
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsGlobal() {
    SituationQueryBean query = new SituationQueryBean();
    SituationQueryBean.AffectsBean affects = new SituationQueryBean.AffectsBean();
    
    affects.setAgencyId("__ALL_OPERATORS__");
    query.getAffects().add(affects);

    ListBean<ServiceAlertBean> serviceAlerts = _transitDataService.getServiceAlerts(query);
    return serviceAlerts.getList();
  }
  
  /**
   * PRIVATE METHODS
   */
  
  private ListBean<TripDetailsBean> getAllTripsForRoute(String routeId, long currentTime) {
    TripsForRouteQueryBean tripRouteQueryBean = new TripsForRouteQueryBean();
    tripRouteQueryBean.setRouteId(routeId);
    tripRouteQueryBean.setTime(currentTime);
    
    TripDetailsInclusionBean inclusionBean = new TripDetailsInclusionBean();
    inclusionBean.setIncludeTripBean(true);
    inclusionBean.setIncludeTripStatus(true);
    tripRouteQueryBean.setInclusion(inclusionBean);

    return _transitDataService.getTripsForRoute(tripRouteQueryBean);
  } 
  
  private List<ArrivalAndDepartureBean> getArrivalsAndDeparturesForStop(String stopId, long currentTime) {
    ArrivalsAndDeparturesQueryBean query = new ArrivalsAndDeparturesQueryBean();
    query.setTime(currentTime);
    query.setMinutesBefore(5 * 60);
    query.setMinutesAfter(5 * 60);
    
    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures =
      _transitDataService.getStopWithArrivalsAndDepartures(stopId, query);

    return stopWithArrivalsAndDepartures.getArrivalsAndDepartures();
  }
  
}