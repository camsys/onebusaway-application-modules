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
package org.onebusaway.transit_data.model.blocks;

import java.io.Serializable;

import org.onebusaway.transit_data.model.schedule.StopTimeBean;

public class BlockStopTimeBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private int blockSequence;

  private double distanceAlongBlock;

  private double accumulatedSlackTime;

  private StopTimeBean stopTime;

  public int getBlockSequence() {
    return blockSequence;
  }

  public void setBlockSequence(int blockSequence) {
    this.blockSequence = blockSequence;
  }

  public double getDistanceAlongBlock() {
    return distanceAlongBlock;
  }

  public void setDistanceAlongBlock(double distanceAlongBlock) {
    this.distanceAlongBlock = distanceAlongBlock;
  }

  public double getAccumulatedSlackTime() {
    return accumulatedSlackTime;
  }

  public void setAccumulatedSlackTime(double accumulatedSlackTime) {
    this.accumulatedSlackTime = accumulatedSlackTime;
  }

  public StopTimeBean getStopTime() {
    return stopTime;
  }

  public void setStopTime(StopTimeBean stopTime) {
    this.stopTime = stopTime;
  }
}
