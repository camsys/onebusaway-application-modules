package org.onebusaway.admin.service.bundle.hastus;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.serialization.GtfsWriter;
import org.onebusaway.transit_data_federation.bundle.tasks.MultiCSVLogger;

public class HastusGtfsFactoryTest {

  private static final String BUS_STOP_SHAPE_FILE = "CTBusStops.shp";
  private static final String ROUTE_STOP_SHAPE_FILE = "CTRouteStopSequences.shp"; 
  HastusGtfsFactory factory = new HastusGtfsFactory();
  MultiCSVLogger logger = new TestMultiCSVLogger();
  
  @Before
  public void setUp() throws Exception {
    factory.setLogger(logger);
    URL resource = getClass().getResource(ROUTE_STOP_SHAPE_FILE);
    assertNotNull(resource);
    String path = resource.getPath().substring(0, resource.getPath().length() - ROUTE_STOP_SHAPE_FILE.length());
    System.out.println("path=" + path);
    File gisInputPath = new File(path);
    factory.setGisInputPath(gisInputPath);
    
  }

  @Test
  public void test() throws Exception {
    factory.setShapePrefix("535-nb-Weekday");
    factory.processRoutesStopSequences();
    factory.processShapes();
    
    for (ShapePoint sp : factory.getDao().getAllShapePoints()) {
      System.out.println(sp.getId() + ":" + sp.getLat() + ", " + sp.getLon());
      if (sp.getId().equals(479)) {
        if (sp.getLat() == 47.83824600070492) {
          if (sp.getLon() == -122.27300100106018) {
            fail("sp 479 has invalid value:" + sp);
          }
        }
      }
    }
    
    GtfsWriter writer = new GtfsWriter();
    writer.setOutputLocation(new File(System.getProperty("java.io.tmpdir")));
    writer.run(factory.getDao());
    writer.close();
    
    fail("Not yet implemented");
  }

  private static class TestMultiCSVLogger extends MultiCSVLogger {
    
  }
  
}
