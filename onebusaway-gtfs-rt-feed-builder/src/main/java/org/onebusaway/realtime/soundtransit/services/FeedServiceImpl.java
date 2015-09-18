/**
 * Copyright (C) 2015 Cambridge Systematics, Inc.
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
package org.onebusaway.realtime.soundtransit.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.commons.lang3.StringUtils;





import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.transit.realtime.GtfsRealtime.Alert;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedHeader.Incrementality;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtimeConstants;

@Component
/**
 * Maps the GTFS-realtime protocol buffer models to the archiver models.
 * 
 */
public class FeedServiceImpl implements FeedService {
	private static Logger _log = LoggerFactory.getLogger(FeedServiceImpl.class);
	private Map<String, String> stopMapping = null;
	private Map<String, String> tripMapping = null;

	private static final String DEFAULT_TRIP_ID = "99991000";
	private static final String LINK_ROUTE_ID = "1_100479";
	
	public Map<String, String> getStopMapping() {
		if (stopMapping == null) {
			_log.info("Reading from AvlStopMappingFile");
			BufferedReader br = null;
			try {
				stopMapping = new HashMap<String, String>();
				String ln = "";
				br = new BufferedReader(new FileReader
						("/var/lib/obanyc/gtfs-rt/AvlStopMapping.txt"));
				while ((ln = br.readLine()) != null) {
					int idx = ln.indexOf(',');
					if (idx > 0) {
						stopMapping.put(ln.substring(0,idx), ln.substring(idx+1));
					}
				}
			} catch (IOException e) {
				_log.error("Error reading StopMapping file " + e.getMessage());
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					_log.error("Exception closing file reader: " + e.getMessage());
				}
			}
		}
		return stopMapping;
	}

	public void setStopMapping(Map<String, String> stopMapping) {
		this.stopMapping = stopMapping;
	}

	public Map<String, String> getTripMapping() {
		if (tripMapping == null) {
			_log.info("Reading from AvlTripMappingFile");
			BufferedReader br = null;
			try {
				tripMapping = new HashMap<String, String>();
				String ln = "";
				br = new BufferedReader(new FileReader
						("/var/lib/obanyc/gtfs-rt/AvlTripMapping.txt"));
				while ((ln = br.readLine()) != null) {
					int idx = ln.indexOf(',');
					if (idx > 0) {
						tripMapping.put(ln.substring(0,idx), ln.substring(idx+1));
					}
				}
			} catch (IOException e) {
				_log.error("Error reading TripMapping file " + e.getMessage());
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					_log.error("Exception closing file reader: " + e.getMessage());
				}
			}
		}
		return tripMapping;
	}

	public void setTripMapping(Map<String, String> tripMapping) {
		this.tripMapping = tripMapping;
	}

	@Override
	public void buildFeed(String rawAvlData) {
	}

	@Override
	public Map<String,Object> feedToJson(String feedData) {
		Map<String,Object> jsonData = new HashMap<String,Object>(); 
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationConfig.Feature.AUTO_DETECT_FIELDS, true);
		boolean parseFailed = false;
		try {
			jsonData = mapper.readValue(feedData, HashMap.class);
		} catch (JsonParseException e) {
			_log.error("JsonParseException trying to parse feed data.");
			parseFailed = true;
		} catch (JsonMappingException e) {
			_log.error("JsonMappingException trying to parse feed data.");
			parseFailed = true;
		} catch (IOException e) {
			_log.error("IOException trying to parse feed data.");
			parseFailed = true;
		}
		if (parseFailed) {
			return null;
		}
		return jsonData;
	}

	public FeedMessage buildVPMessage(Map<String,Object> parsedAvlUpdates) {
		FeedMessage vehiclePositionsFM = null;

		Map<String, Object> tripsData = (Map<String, Object>) parsedAvlUpdates.get("Trips");
		ArrayList<Map<String,Object>> trips = null;
		if (tripsData != null) {
			trips = (ArrayList<Map<String,Object>>) tripsData.get("Trip");
		}
		FeedMessage.Builder feedMessageBuilder = FeedMessage.newBuilder();
		FeedHeader.Builder header = FeedHeader.newBuilder();
		header.setTimestamp(System.currentTimeMillis());
		header.setIncrementality(Incrementality.FULL_DATASET);
		header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
		feedMessageBuilder.setHeader(header);
		if (trips != null) {
		  _log.info("Number of trips is " + trips.size());
			for (Map<String,Object> trip : trips) {
				VehiclePosition.Builder vp = VehiclePosition.newBuilder();
				VehicleDescriptor.Builder vd = VehicleDescriptor.newBuilder();
				String vehicleId = (String) trip.get("VehicleId");
				_log.info("Processing VehiclePosition for " + trip.get("VehicleId"));
				if (vehicleId == null) {
					vehicleId = "";
				}
				vd.setId(vehicleId);
				vp.setVehicle(vd);

				String stopId = (String) trip.get("LastStopId");
				if (stopId == null) {
					stopId = "";
				} else {
					stopId = getStopMapping().get(stopId);
					if (stopId == null) {
						_log.info("Could not map stop: " + (String) trip.get("LastStopId"));
						stopId = "";
					}
				}
				vp.setStopId(stopId);
				vp.setTimestamp(System.currentTimeMillis());
				vp.setCurrentStatus(VehiclePosition.VehicleStopStatus.INCOMING_AT);
				
				String nextStop = findNextStopOnTrip(trip);
				// Loop through StopUpdates to determine the trip start time and date.
				// Initially, set nextStopTime to an arbitrarily high value.
				Calendar cal = Calendar.getInstance();
				cal.set(2099, 12, 31);
				Date nextStopTime = cal.getTime();
        Date tripStartTime = null;
        Map<String, Object> stopTimeUpdateData = (Map<String, Object>) trip.get("StopUpdates");
        ArrayList<Map<String, Object>> stopTimeUpdates = (ArrayList<Map<String, Object>>) stopTimeUpdateData.get("Update");
        if (stopTimeUpdates != null && stopTimeUpdates.size() > 0) {
          for (Map<String, Object> stopTimeUpdate : stopTimeUpdates) {
            StopTimeUpdate.Builder stu = StopTimeUpdate.newBuilder();
            Map<String, String> arrivalTime = (Map<String, String>) stopTimeUpdate.get("ArrivalTime");
            String arrival = null;
            if (arrivalTime != null) {
              arrival = arrivalTime.get("Actual");
            }
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            if (arrival != null) {   // If this is the earliest time, use it for the trip start time
              Date parsedDate = null;
              try {
                parsedDate = df.parse(arrival);
              } catch (Exception e) {
                System.out.println("Exception parsing Estimated time: " + arrival);
              }       
              if (tripStartTime == null) {
                tripStartTime = parsedDate;
              } else if (parsedDate.before(tripStartTime)) {
                tripStartTime = parsedDate;
              }
            } 
          }
        }	
        // Reset stop id to next stop if it was unmapped
        if (stopId == "") {
          vp.setStopId(getStopMapping().get(nextStop));
        }
        TripDescriptor td = buildTripDescriptor(trip);
        vp.setTrip(td);

				FeedEntity.Builder entity = FeedEntity.newBuilder();
				entity.setId(vehicleId);
				_log.info("Add VehiclePosition for " + vp.getVehicle().getId());
				entity.setVehicle(vp);
				feedMessageBuilder.addEntity(entity);
			}
		} else {
			//TODO: decide what to do if no data is found.
			Map<String, String> feedResult = (Map<String, String>) parsedAvlUpdates.get("Fault");
		}
		vehiclePositionsFM = feedMessageBuilder.build();
		return vehiclePositionsFM;
	}

	public FeedMessage buildTUMessage(Map<String,Object> parsedAvlUpdates) {
		FeedMessage tripUpdatesFM = null;

		Map<String, Object> tripsData = (Map<String, Object>) parsedAvlUpdates.get("Trips");
		ArrayList<Map<String,Object>> trips = null;
		if (tripsData != null) {
			trips = (ArrayList<Map<String,Object>>) tripsData.get("Trip");
		}
		FeedMessage.Builder feedMessageBuilder = FeedMessage.newBuilder();
		FeedHeader.Builder header = FeedHeader.newBuilder();
		header.setTimestamp(System.currentTimeMillis());
		header.setIncrementality(Incrementality.FULL_DATASET);
		header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
		feedMessageBuilder.setHeader(header);
		if (trips != null) {
			for (Map<String,Object> trip : trips) {
			  
				TripUpdate.Builder tu = TripUpdate.newBuilder();

        // Build the StopTimeUpdates
        List<StopTimeUpdate> stopTimeUpdates = buildStopTimeUpdates(trip);
        tu.addAllStopTimeUpdate(stopTimeUpdates);

				// Build the VehicleDescriptor
        VehicleDescriptor.Builder vd = VehicleDescriptor.newBuilder();
        String vehicleId = (String) trip.get("VehicleId");
        if (vehicleId == null) {
          vehicleId = "";
        }
        vd.setId(vehicleId);
        tu.setVehicle(vd);
       
        // Build the TripDescriptor
        TripDescriptor td = buildTripDescriptor(trip);
				tu.setTrip(td);
				FeedEntity.Builder entity = FeedEntity.newBuilder();
				entity.setId((String)trip.get("TripId"));
				entity.setTripUpdate(tu);
				feedMessageBuilder.addEntity(entity);
			}
		} else {
			//TODO: decide what to do if no data is found.
			Map<String, String> feedResult = (Map<String, String>) parsedAvlUpdates.get("Fault");
		}
		tripUpdatesFM = feedMessageBuilder.build();
		return tripUpdatesFM;
	} 
	
	private String findNextStopOnTrip(Map<String,Object> trip) {
	  // Check the times for the StopUpdates to determine which stop the vehicle 
	  // will reach next.  That will be the stop with the earliest estimated 
	  // arrival time, but an actual time of null.  If the trip is already 
	  // completed, i.e. every stop update has an actual arrival time, then an 
	  // empty string will be returned.
	  trip = (trip == null ? Collections.EMPTY_MAP : trip); //Check for null
	  
    String nextStop = "";
    // Initially, set nextStopTime to an arbitrarily high value.
    Calendar cal = Calendar.getInstance();
    cal.set(2099, 12, 31);
    Date nextStopTime = cal.getTime();
    Map<String, Object> stopTimeUpdateData = (Map<String, Object>) trip.get("StopUpdates");
    List<Map<String, Object>> stopTimeUpdates = (ArrayList<Map<String, Object>>) stopTimeUpdateData.get("Update");
    stopTimeUpdates = (stopTimeUpdates == null ? Collections.EMPTY_LIST : stopTimeUpdates);  // Check for null
    for (Map<String, Object> stopTimeUpdate : stopTimeUpdates) {
      Map<String, String> arrivalTime = (Map<String, String>) stopTimeUpdate.get("ArrivalTime");
      if (arrivalTime == null) continue;
      String arrival = arrivalTime.get("Actual");
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
      if (arrival == null) {  // No "Actual", so this stop hasn't been reached yet.
        arrival = arrivalTime.get("Estimated");
        Date parsedDate = null;
        try {
          parsedDate = df.parse(arrival);
        } catch (ParseException e) {
          _log.error("Exception parsing Estimated time: " + arrival);
          parsedDate = nextStopTime;
        }
        if (parsedDate.before(nextStopTime)) {
          nextStopTime = parsedDate;
          nextStop = (String) stopTimeUpdate.get("StopId");
        }
      }
    }

	  return nextStop;
	}
	
	private List<StopTimeUpdate> buildStopTimeUpdates(Map<String,Object> trip) {
	  List<StopTimeUpdate> stopTimeUpdateList = new ArrayList<StopTimeUpdate>();
    Map<String, Object> stopTimeUpdateData = (Map<String, Object>) trip.get("StopUpdates");
    List<Map<String, Object>> stopTimeUpdates = (ArrayList<Map<String, Object>>) stopTimeUpdateData.get("Update");
    if (stopTimeUpdates != null && stopTimeUpdates.size() > 0) {
      for (Map<String, Object> stopTimeUpdate : stopTimeUpdates) {
        StopTimeUpdate.Builder stu = StopTimeUpdate.newBuilder();
        Map<String, String> arrivalTime = (Map<String, String>) stopTimeUpdate.get("ArrivalTime");
        String arrival = null;
        if (arrivalTime != null) {
          arrival = arrivalTime.get("Actual");
        }
        // If "Actual" is null, the stop hasn't happened yet, so use the "Estimated" time.
        if (arrival == null && arrivalTime != null) {   
          arrival = arrivalTime.get("Estimated");
        }
        if (arrival != null) {
          try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSSXXX");
            Date parsedDate = df.parse(arrival);
            StopTimeEvent.Builder ste = StopTimeEvent.newBuilder();
            ste.setTime(parsedDate.getTime());

            String stopId = (String) stopTimeUpdate.get("StopId");
            if (stopId != null) {
              stopId = getStopMapping().get(stopId);
              if (stopId == null) {
                continue;     // No mapping for this stop, so don't add it.
              }
              stu.setStopId(stopId);
              stu.setArrival(ste);
              stu.setDeparture(ste);
            }
          } catch (Exception e) {
            _log.error("Exception parsing Estimated time: " + arrival);
          }             
        }
        stopTimeUpdateList.add(stu.build());
      }
    }
	  return stopTimeUpdateList;
	}
	
	private TripDescriptor buildTripDescriptor(Map<String,Object> trip) {
    TripDescriptor.Builder td = TripDescriptor.newBuilder();
    String tripId = (String) trip.get("TripId");
    if (tripId == null) {
      _log.info("trip id is null");
      tripId = "";
    } else {
      _log.info("Mapping trip: " + tripId);
      tripId = getTripMapping().get(tripId);
      // Set unmapped trips to the default trip id
      if (tripId == null) {
        _log.info("Could not map trip " + trip.get("TripId")
            + ". Defaulting to trip " + DEFAULT_TRIP_ID);
        tripId = DEFAULT_TRIP_ID;
      }
    }
    // Set trip start time and date from tripStartTime
    Date tripStartTime = getTripStartTime(trip);
    if (tripStartTime != null) {
      DateFormat df = new SimpleDateFormat("kk:mm:ss");
      String startTime = df.format(tripStartTime);
      df = new SimpleDateFormat("yyyyMMdd");
      String startDate = df.format(tripStartTime);
      td.setStartTime(startTime);
      td.setStartDate(startDate);
    } else {
      _log.info("Null tripStartTime for trip " + tripId);
    }   
    
    td.setTripId(tripId);
    td.setRouteId(LINK_ROUTE_ID);
    
    return td.build();
	}
	
	private Date getTripStartTime(Map<String,Object> trip) {
    Date tripStartTime = null;
    
    // Check each StopTimeUpdate to find the earliest stop arrival time
    Map<String, Object> stopTimeUpdateData = (Map<String, Object>) trip.get("StopUpdates");
    ArrayList<Map<String, Object>> stopTimeUpdates = (ArrayList<Map<String, Object>>) stopTimeUpdateData.get("Update");
    if (stopTimeUpdates != null && stopTimeUpdates.size() > 0) {
      for (Map<String, Object> stopTimeUpdate : stopTimeUpdates) {
        Map<String, String> arrivalTime = (Map<String, String>) stopTimeUpdate.get("ArrivalTime");
        String arrival = null;
        if (arrivalTime != null) {
          arrival = arrivalTime.get("Actual");
        }
        if (arrival == null) {   // No Actual time, so use Estimated
          arrival = arrivalTime.get("Estimated");
        }        
        // If this is the earliest time, use it for the trip start time
        if (arrival != null) {   
          Date parsedDate = null;
          try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            parsedDate = df.parse(arrival);
          } catch (Exception e) {
            System.out.println("Exception parsing Estimated time: " + arrival);
          }       
          if (tripStartTime == null) {
            tripStartTime = parsedDate;
          } else if (parsedDate.before(tripStartTime)) {
            tripStartTime = parsedDate;
          }
        }
      }
    }     
    return tripStartTime;
	}
}
