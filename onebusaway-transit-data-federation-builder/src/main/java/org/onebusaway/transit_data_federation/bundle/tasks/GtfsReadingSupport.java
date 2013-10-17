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
package org.onebusaway.transit_data_federation.bundle.tasks;

import java.io.IOException;
import java.util.Map;

import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.serialization.GtfsEntitySchemaFactory;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.onebusaway.transit_data_federation.bundle.services.EntityReplacementStrategy;
import org.springframework.context.ApplicationContext;

/**
 * Convenience methods for reading multiple gtfs feeds, as defined by a
 * {@link GtfsBundles} entry in an {@link ApplicationContext}, into a common
 * data store, with addition of {@link EntityReplacementStrategy}.
 * 
 * @author bdferris
 * @see GtfsBundles
 * @see GtfsMultiReaderImpl
 * @see EntityReplacementStrategy
 */
public class GtfsReadingSupport {

  /**
   * Supplies a default entity schema factory to
   * {@link #readGtfsIntoStore(ApplicationContext, GenericMutableDao, DefaultEntitySchemaFactory)}
   * 
   * @param context
   * @param store
   * @throws IOException
   */
  public static void readGtfsIntoStore(ApplicationContext context,
      GenericMutableDao store) throws IOException {
    readGtfsIntoStore(context, store,
        GtfsEntitySchemaFactory.createEntitySchemaFactory());
  }

  /**
   * Read gtfs, as defined by {@link GtfsBundles} entries in the application
   * context, into the specified data store. Gtfs will be read in quasi-paralle
   * mode using {@link GtfsMultiReaderImpl}. Any
   * {@link EntityReplacementStrategy} strategies defined in the application
   * context will be applied as well.
   * 
   * @param context
   * @param store
   * @param factory
   * @throws IOException
   */
  public static void readGtfsIntoStore(ApplicationContext context,
      GenericMutableDao store, DefaultEntitySchemaFactory factory)
      throws IOException {

    GtfsMultiReaderImpl multiReader = new GtfsMultiReaderImpl();
    multiReader.setStore(store);

    if (context.containsBean("entityReplacementStrategy")) {
      EntityReplacementStrategy strategy = (EntityReplacementStrategy) context.getBean("entityReplacementStrategy");
      multiReader.setEntityReplacementStrategy(strategy);
    }

    GtfsBundles gtfsBundles = getGtfsBundles(context);

    for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {

      System.out.println("gtfs=" + gtfsBundle.getPath());

      GtfsReader reader = new GtfsReader();
      reader.setEntitySchemaFactory(factory);
      reader.setInputLocation(gtfsBundle.getPath());

      if (gtfsBundle.getDefaultAgencyId() != null)
        reader.setDefaultAgencyId(gtfsBundle.getDefaultAgencyId());

      for (Map.Entry<String, String> entry : gtfsBundle.getAgencyIdMappings().entrySet())
        reader.addAgencyIdMapping(entry.getKey(), entry.getValue());

      multiReader.addGtfsReader(reader);
    }

    multiReader.run();
  }

  /**
   * Looks for instances of {@link GtfsBundles} or {@link GtfsBundle} in the
   * application context.
   * 
   * @param context
   * @return
   */
  private static GtfsBundles getGtfsBundles(ApplicationContext context) {

    GtfsBundles bundles = (GtfsBundles) context.getBean("gtfs-bundles");
    if (bundles != null)
      return bundles;

    GtfsBundle bundle = (GtfsBundle) context.getBean("gtfs-bundle");
    if (bundle != null) {
      bundles = new GtfsBundles();
      bundles.getBundles().add(bundle);
      return bundles;
    }

    throw new IllegalStateException(
        "must define either \"gtfs-bundles\" or \"gtfs-bundle\" in config");
  }
}
