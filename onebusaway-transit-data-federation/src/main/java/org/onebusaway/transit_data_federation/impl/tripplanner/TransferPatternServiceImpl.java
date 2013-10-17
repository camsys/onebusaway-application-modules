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
package org.onebusaway.transit_data_federation.impl.tripplanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.tripplanner.CompactedTransferPattern;
import org.onebusaway.transit_data_federation.services.tripplanner.HubNode;
import org.onebusaway.transit_data_federation.services.tripplanner.TransferNode;
import org.onebusaway.transit_data_federation.services.tripplanner.TransferParent;
import org.onebusaway.transit_data_federation.services.tripplanner.TransferPattern;
import org.onebusaway.transit_data_federation.services.tripplanner.TransferPatternData;
import org.onebusaway.transit_data_federation.services.tripplanner.TransferPatternService;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class TransferPatternServiceImpl implements TransferPatternService {

  private static Logger _log = LoggerFactory.getLogger(TransferPatternServiceImpl.class);

  private Map<StopEntry, TransferPattern> _transferPatternsByStop = new HashMap<StopEntry, TransferPattern>();

  private FederatedTransitDataBundle _bundle;

  private TransitGraphDao _transitGraphDao;

  private boolean _enabled = true;

  @Autowired
  public void setBundle(FederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }
  
  public void setEnabled(boolean enabled) {
    _enabled = enabled;
  }

  @PostConstruct
  @Refreshable(dependsOn = RefreshableResources.TRANSFER_PATTERNS)
  public void setup() throws IOException, ClassNotFoundException {

    _transferPatternsByStop.clear();
    
    if( ! _enabled )
      return;

    File path = _bundle.getSerializedTransferPatternsPath();

    if (!path.exists())
      return;

    _log.info("loading transfer patterns...");
    Map<AgencyAndId, CompactedTransferPattern> patternsByStopId = ObjectSerializationLibrary.readObject(path);
    _log.info("transfer patterns loaded");

    for (Map.Entry<AgencyAndId, CompactedTransferPattern> entry : patternsByStopId.entrySet()) {
      AgencyAndId stopId = entry.getKey();
      StopEntry stop = _transitGraphDao.getStopEntryForId(stopId, true);
      CompactedTransferPattern pattern = entry.getValue();
      pattern.setAllStops(_transitGraphDao.getAllStops());
      _transferPatternsByStop.put(stop, pattern);
    }
  }

  @Override
  public boolean isEnabled() {
    return !_transferPatternsByStop.isEmpty();
  }

  @Override
  public TransferParent getTransferPatternsForStops(
      TransferPatternData transferPatternData, StopEntry stopFrom,
      List<StopEntry> stopsTo) {

    TransferParent root = getTransferPatternsForStopsInternal(
        transferPatternData, stopFrom, stopsTo);
    transferPatternData.clearMinRemainingWeights();
    return root;
  }

  @Override
  public Collection<TransferNode> getReverseTransferPatternsForStops(
      TransferPatternData transferPatternData, Iterable<StopEntry> stopsFrom,
      StopEntry stopTo) {

    List<StopEntry> stopToAsList = Arrays.asList(stopTo);

    TransferParent root = new TransferParent(transferPatternData);

    TransferPatternData forwardData = new TransferPatternData();

    for (StopEntry stopFrom : stopsFrom) {

      TransferParent trees = getTransferPatternsForStopsInternal(forwardData,
          stopFrom, stopToAsList);
      Set<TransferNode> visited = new HashSet<TransferNode>();
      for (TransferNode tree : trees.getTransfers())
        reverseTree(tree, root, true, visited);
    }

    transferPatternData.clearMinRemainingWeights();

    return root.getTransfers();
  }

  @Override
  public Collection<TransferNode> expandNode(HubNode node) {
    throw new IllegalStateException();
    // return getTransferPatternForStops(node.getHubStop(), node.getStopsTo());
  }

  /****
   * Private Methods
   ****/

  private TransferParent getTransferPatternsForStopsInternal(
      TransferPatternData transferPatternData, StopEntry stopFrom,
      List<StopEntry> stopsTo) {

    TransferParent root = new TransferParent(transferPatternData);

    TransferPattern pattern = _transferPatternsByStop.get(stopFrom);
    if (pattern == null)
      return root;

    pattern.getTransfersForStops(root, stopsTo);

    Map<StopEntry, List<TransferParent>> hubParentsByStop = pattern.getTransfersForHubStops(root);

    for (Map.Entry<StopEntry, List<TransferParent>> entry : hubParentsByStop.entrySet()) {

      StopEntry hubStop = entry.getKey();
      List<TransferParent> parents = entry.getValue();

      TransferParent nodes = transferPatternData.getNodesForHubStop(hubStop);

      if (nodes == null) {
        nodes = getTransferPatternsForStopsInternal(transferPatternData,
            hubStop, stopsTo);
        transferPatternData.setNodesForHubStop(hubStop, nodes);
      }

      for (TransferParent parent : parents) {
        for (TransferNode node : nodes.getTransfers()) {
          parent.addTransferNode(node);
        }
      }
    }

    return root;
  }

  /**
   * We want to reverse the transfer pattern tree. Given a starting node, we
   * search down the tree until we reach an end-point and then construct tree
   * back up. Since there can be cycles in the tree
   * 
   * @param node
   * @param root
   * @param exitAllowed
   * @param visited
   * @return
   */
  private List<TransferParent> reverseTree(TransferNode node,
      TransferParent root, boolean exitAllowed, Set<TransferNode> visited) {

    visited.add(node);

    List<TransferParent> results = new ArrayList<TransferParent>();

    if (node.isExitAllowed()) {
      TransferNode extended = root.extendTree(node.getToStop(),
          node.getFromStop(), exitAllowed);
      results.add(extended);
    }

    for (TransferNode subNode : node.getTransfers()) {

      /**
       * There can be circular paths in the transfer tree. If we've already
       * visited the sub-node, we don't visit it again.
       */
      if (visited.contains(subNode))
        continue;

      List<TransferParent> parents = reverseTree(subNode, root, false, visited);

      for (TransferParent parent : parents) {
        TransferNode extended = parent.extendTree(node.getToStop(),
            node.getFromStop(), exitAllowed);
        results.add(extended);
      }
    }

    return results;
  }

}
