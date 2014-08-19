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
package org.onebusaway.webapp.gwt.oba_application.model;

import org.onebusaway.transit_data.model.oba.LocalSearchResult;
import org.onebusaway.transit_data.model.oba.TimedPlaceBean;

public class TimedLocalSearchResult {

  private static final long serialVersionUID = 1L;

  private LocalSearchResult result;

  private TimedPlaceBean bean;

  private String resultId;

  public TimedLocalSearchResult() {

  }

  public TimedLocalSearchResult(String resultId, LocalSearchResult result,
      TimedPlaceBean bean) {
    this.resultId = resultId;
    this.result = result;
    this.bean = bean;
  }

  public String getResultId() {
    return this.resultId;
  }

  public String getId() {
    return result.getId();
  }

  /**
   * @return trip time in seconds
   */
  public int getTime() {
    return bean.getTime();
  }

  public LocalSearchResult getLocalSearchResult() {
    return result;
  }

  public TimedPlaceBean getTimedPlace() {
    return bean;
  }
}
