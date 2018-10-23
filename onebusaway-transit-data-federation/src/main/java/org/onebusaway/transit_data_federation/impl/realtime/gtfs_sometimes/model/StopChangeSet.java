/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.model;

import java.util.List;

public class StopChangeSet {

    private List<StopChange> stopChanges;

    public StopChangeSet(List<StopChange> stopChanges) {
        this.stopChanges = stopChanges;
    }

    public List<StopChange> getStopChanges() {
        return stopChanges;
    }
}
