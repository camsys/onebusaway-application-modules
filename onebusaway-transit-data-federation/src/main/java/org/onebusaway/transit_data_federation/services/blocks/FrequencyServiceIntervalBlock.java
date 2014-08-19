/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google, Inc.
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
package org.onebusaway.transit_data_federation.services.blocks;

import java.io.Serializable;

import org.onebusaway.gtfs.model.calendar.ServiceInterval;

/**
 * Specifies an immutable interval of min and max arrival and departure times.
 * 
 * TODO: Rename to FrequencyIntervalIndex to avoid confusing use of 'Block'.
 * 
 * @author bdferris
 * 
 */
public final class FrequencyServiceIntervalBlock implements Serializable,
    Comparable<FrequencyServiceIntervalBlock> {

  private static final long serialVersionUID = 1L;

  private final int[] startTimes;
  private final int[] endTimes;

  /**
   * 
   */
  public FrequencyServiceIntervalBlock(int[] startTimes, int[] endTimes) {
    this.startTimes = startTimes;
    this.endTimes = endTimes;

    if (startTimes.length != endTimes.length)
      throw new IllegalArgumentException("arrays must have same length");
  }

  /**
   * 
   * @return frequency block from times in seconds since midnight
   */
  public int[] getStartTimes() {
    return startTimes;
  }

  /**
   * 
   * @return frequency block to times in seconds since midnight
   */
  public int[] getEndTimes() {
    return endTimes;
  }

  public ServiceInterval getRange() {
    int n = startTimes.length - 1;
    return new ServiceInterval(startTimes[0], startTimes[0], endTimes[n],
        endTimes[n]);
  }

  @Override
  public int compareTo(FrequencyServiceIntervalBlock o) {
    return startTimes[0] - o.startTimes[0];
  }
}
