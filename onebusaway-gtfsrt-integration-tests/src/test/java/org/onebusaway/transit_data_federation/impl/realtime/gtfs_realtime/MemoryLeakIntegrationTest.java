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

import org.junit.Test;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.transit_data_federation.impl.realtime.TestVehicleLocationListener;
import org.onebusaway.transit_data_federation.impl.realtime.VehicleStatusServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.getProperty;

/**
 * build and load bundles verifying garbage collection reclaims memory.
 */
public class MemoryLeakIntegrationTest {

  protected static Logger _log = LoggerFactory.getLogger(MemoryLeakIntegrationTest.class);
  private BundleLoader _bundleLoader;
  public BundleLoader getBundleLoader() {
    return _bundleLoader;
  }

  private BundleBuilder _bundleBuilder;
  public BundleBuilder getBundleBuilder() {
    return _bundleBuilder;
  }

  protected String getIntegrationTestPath() {
    return "org/onebusaway/transit_data_federation/impl/realtime/gtfs_realtime/integration_tests/memory_leak";
  }

  protected String[] getPaths() {
    String[] paths = {"test-data-sources.xml"};
    return paths;
  }

  @Test
  public void test() throws Exception {
    long totalMemory = 0;
    List<BundleContext> bundleContexts = new ArrayList<>();
    String bundleRootDir = getProperty("bundleRootDir");
    String lastIndexFile = null;
    if (bundleRootDir != null) {
      _log.error("bundleRootDir configured {}, not building bundles");
      lastIndexFile = "/tmp/index.json";
      bundleContexts.addAll(createBundleContexts(bundleRootDir, lastIndexFile));
      updateIndexFile(lastIndexFile, bundleContexts.get(0)._bundleGzipURI);
    } else {
      _log.info("building bundles");
      for (int i = 0; i < 5; i++) {
        _bundleBuilder = new BundleBuilder();
        _bundleBuilder.setup(getIntegrationTestPath());
        bundleContexts.add(_bundleBuilder.getBundleContext());
      }
    }

    GtfsRealtimeSource source = null;
    while (!Thread.interrupted()) {
      for (BundleContext bundleContext : bundleContexts) {
        _log.info("working with gzip {}", bundleContext._bundleGzipURI);
        if (lastIndexFile != null) {
          updateIndexFile(lastIndexFile, bundleContext._bundleGzipURI);
        }

        if (lastIndexFile == null) {
          _log.info("loading bundle {}", bundleContext._bundleGzipURI);
          setupLoader(bundleContext);
          lastIndexFile = bundleContext._bundleIndexURI;
        }

        if (source == null) {
          if (_bundleLoader == null) {
            setupLoader(bundleContext);
          }
          source = setupSource(_bundleLoader);
        }

        for (int wait=0; wait<12; wait++) {
          long newTotalMemory = Runtime.getRuntime().totalMemory();
          if (newTotalMemory > totalMemory) {
            totalMemory = newTotalMemory;
          } else if (newTotalMemory < totalMemory) {
            _log.info("SUCCESS, memory reduced from {} to {}", totalMemory, newTotalMemory);
            return;
          }
          _log.info("ping with memory {}", totalMemory / 1024 / 1024);
          Thread.sleep(10 * 1000);
        }
      }
    }
  }

  private void setupLoader(BundleContext bundleContext) throws Exception {
    _bundleLoader = new BundleLoader(bundleContext);
    _bundleLoader.create(getPaths());
    _bundleLoader.load();
  }

  private GtfsRealtimeSource setupSource(BundleLoader bundleLoader) throws Exception {
    GtfsRealtimeSource source = bundleLoader.getSource();
    TestVehicleLocationListener listener = new TestVehicleLocationListener();
    VehicleLocationListener actualListener = getBundleLoader().getApplicationContext().getBean(VehicleStatusServiceImpl.class);
    source.setAgencyId("MTASBWY");    listener.setVehicleLocationListener(actualListener);
    source.setVehicleLocationListener(listener);

    source.setTripUpdatesUrl(new URL("https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2Fgtfs"));
    Map<String, String> headers = new HashMap<>();
    headers.put("x-api-key", getProperty("apikey"));
    source.setHeadersMap(headers);
    return source;
  }

  private List<BundleContext> createBundleContexts(String bundleRootDir, String lastIndexFile) throws Exception {
    String[] bundleNames = getProperty("bundles").split(",");
    List<BundleContext> bundleContexts = new ArrayList<>();
    for (String bundleName : bundleNames) {
      BundleContext bc = new BundleContext();
      String bundleSubDir = bundleRootDir + File.separator + bundleName;
      bc.setup(bundleRootDir, findGzip(bundleSubDir), lastIndexFile);
      bundleContexts.add(bc);
    }
    return bundleContexts;
  }

  private String findGzip(String bundleDir) throws Exception {
    File file = new File(bundleDir);
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File f : files) {
        String result = findGzip(f.toString());
        if (result != null)
          return result;
      }
    } else {
      if (file.getName().endsWith(".tar.gz")) {
        return file.toURI().toString();
      }
    }
  return null;
 }

  private void updateIndexFile(String lastIndexFile, String bundleGzipURI) throws Exception {
    String indexJson = "{\"latest\":\"" + bundleGzipURI + "\"}";
    FileWriter fw = new FileWriter(lastIndexFile);
    fw.write(indexJson);
    fw.close();
    _log.error("wrote {} to {}", bundleGzipURI, lastIndexFile);
  }

}
