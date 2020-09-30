/**
 * Copyright (C) 2020 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.services.revenue;

public interface RevenueSearchService {
  public Boolean stopHasRevenueServiceOnRoute(String agencyId, String stopId,
      String routeId, String directionId);
  
  public Boolean stopHasRevenueService(String agencyId, String stopId);

  void addRevenueService(String agencyId, String stopId, String routeId, String directionId);

  void removeRevenueService(String agencyId, String stopId, String routeId, String directionId);
}
