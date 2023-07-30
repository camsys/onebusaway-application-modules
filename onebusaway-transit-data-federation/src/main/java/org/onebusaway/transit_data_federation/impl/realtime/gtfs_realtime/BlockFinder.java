/**
 * Copyright (C) 2023 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Methods for searching for a block.
 */
public class BlockFinder {
  private BlockCalendarService _blockCalendarService;
  public BlockFinder(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  /**
   * We need a concept of service date -- the day the block is anchored in.
   * Because GTFS supports both negative start times and 25+ hour blocks,
   * this needs to be a heuristic.
   */
  public BlockServiceDate getBlockServiceDateFromTrip(TripEntry tripEntry,
                                                      long currentTime) {
    ServiceDate serviceDate;
    for (ServiceDate serviceDateGuess : getPossibleServiceDates(currentTime)) {
      BlockInstance blockInstance = _blockCalendarService.getBlockInstance(tripEntry.getBlock().getId(),
              serviceDateGuess.getAsDate().getTime());
      if (blockInstance != null) {
        serviceDate = new ServiceDate(new Date(blockInstance.getServiceDate()));
        return new BlockServiceDate(serviceDate, blockInstance);
      }
    }

      return null;
  }

  private List<ServiceDate> getPossibleServiceDates(long currentTime) {
    List<ServiceDate> possibleDates = new ArrayList<>();
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(currentTime);
    // if we are less than 4:00 we could be previous start date
    if (cal.get(Calendar.HOUR_OF_DAY) < 4) {
      possibleDates.add(yesterday(currentTime));
    }
    // always check current date
    possibleDates.add(today(currentTime));
    // if we are past 20:00 we could be a next start time
    if (cal.get(Calendar.HOUR_OF_DAY) > 20) {
      possibleDates.add(tomorrow(currentTime));
    }
    return possibleDates;
  }

  private ServiceDate tomorrow(long currentTime) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(currentTime);
    cal.add(Calendar.DAY_OF_MONTH, +1);
    return new ServiceDate(cal);
  }

  private ServiceDate yesterday(long currentTime) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(currentTime);
    cal.add(Calendar.DAY_OF_MONTH, -1);
    return new ServiceDate(cal);
  }

  private ServiceDate today(long currentTime) {
    return new ServiceDate(new Date(currentTime));
  }

  /*

    if (serviceDate != null) {
    	instance = _blockCalendarService.getBlockInstance(block.getId(),
    			serviceDate.getAsDate().getTime());
    	if (instance == null) {
    		_log.debug("block " + block.getId() + " does not exist on service date "
    				+ serviceDate);
    		return null;
    	}
    } else {
      // we have legacy support for missing service date
      // mostly for unit tests but also legacy feeds
    	long timeFrom = currentTime - 30 * 60 * 1000;
    	long timeTo = currentTime + 30 * 60 * 1000;

    	List<BlockInstance> instances = _blockCalendarService.getActiveBlocks(
    			block.getId(), timeFrom, timeTo);

    	if (instances.isEmpty()) {
    		instances = _blockCalendarService.getClosestActiveBlocks(block.getId(),
    				currentTime);
    	}

    	if (instances.isEmpty()) {
    		_log.debug("could not find any active instances for the specified block="
    				+ block.getId() + " trip=" + trip);
    		return null;
    	}
    	instance = instances.get(0);
    }

    if (serviceDate == null) {
    	serviceDate = new ServiceDate(new Date(instance.getServiceDate()));
    }

   */
}
