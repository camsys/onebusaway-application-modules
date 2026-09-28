/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model;

/**
 * Describes a single GTFS-realtime Trip Modifications feed to be polled: where to fetch it,
 * how often, whether it's enabled, and its priority relative to other configured feeds when
 * two feeds report a modification for the same trip (lower value wins).
 */
public class TripModificationsFeedDefinition {

    private final String feedId;
    private final String url;
    private final boolean enabled;
    private final int refreshIntervalSeconds;
    private final int priority;

    public TripModificationsFeedDefinition(String feedId, String url, boolean enabled,
                                           int refreshIntervalSeconds, int priority) {
        this.feedId = feedId;
        this.url = url;
        this.enabled = enabled;
        this.refreshIntervalSeconds = refreshIntervalSeconds;
        this.priority = priority;
    }

    public String getFeedId() {
        return feedId;
    }

    public String getUrl() {
        return url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TripModificationsFeedDefinition)) return false;
        TripModificationsFeedDefinition other = (TripModificationsFeedDefinition) o;
        return feedId != null ? feedId.equals(other.feedId) : other.feedId == null;
    }

    @Override
    public int hashCode() {
        return feedId != null ? feedId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TripModificationsFeedDefinition{" +
                "feedId='" + feedId + '\'' +
                ", url='" + url + '\'' +
                ", enabled=" + enabled +
                ", refreshIntervalSeconds=" + refreshIntervalSeconds +
                ", priority=" + priority +
                '}';
    }
}