/**
 * Copyright (C) 2022 Cambridge Systematics, Inc.
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
package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.integration_tests;

import org.junit.Test;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.AbstractGtfsRealtimeIntegrationTest;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime.GtfsRealtimeSource;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Run random traces here and test for expected arrivals
 */
public class NyctRandomIntegrationTest extends AbstractGtfsRealtimeIntegrationTest {

  protected String getIntegrationTestPath() {
    return "org/onebusaway/transit_data_federation/impl/realtime/gtfs_realtime/integration_tests/nyct_multi_trips";
  }

  protected String[] getPaths() {
    String[] paths = {"test-data-sources.xml"};
    return paths;
  }

  @Test
  public void test1() throws Exception {
    // missing 0, 11, 15? arrivals on "1" compared to 1.3
    // for stop : "59 St - Columbus Circle"
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_1","MTASBWY_2","MTASBWY_3");
    String expectedStopId = "MTASBWY_125N";
    String expectedRouteId = "MTASBWY_1";
    String path = getIntegrationTestPath() + File.separator;
    String name = "nyct_subways_gtfs_rt.2024-02-11T00:28:07-04:00.pb";

    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, name);
    expectArrival(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId, 1);
    expectArrival(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId, 11);
    expectArrival(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId, 15);

  }

  @Test
  public void test2() throws Exception {
    // Stops 123N and 110S not showing service after midnight
    // GTFS-RT trip start date can't be trusted -- we need to compute service day
    // based on first stop time to avoid negative arrival times
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_1","MTASBWY_2","MTASBWY_3");
    String expectedStopId = "MTASBWY_123N";
    String expectedRouteId = "MTASBWY_2";
    String path = getIntegrationTestPath() + File.separator;
    String name = "nyct_subways_gtfs_rt.2024-02-16T00:53:20-04:00.pb";

    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, name);
    // 147200_1..N03R
    // start date 20240216
    // arrival 1708063139 / Fri Feb 16 00:58:59 EST 2024
    // first stop 1708062869
    expectArrival(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId, 4);
    expectArrival(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId, 5);
//    expectArrival(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId, 11);
//    expectArrival(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId, 15);

  }

  @Test
  public void test3() throws Exception {
  // this pattern breaks head-signs (and A/D on prod)
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_J","MTASBWY_Z");
    String expectedStopId = "MTASBWY_J12N";
    String expectedRouteId = "MTASBWY_J";
    String expectedTripId = "MTASBWY_055000_J..N43R";
    String expectedHeadsign = "Jamaica Center-Parsons/Archer"; // error if "Broadway Junction"
    String path = getIntegrationTestPath() + File.separator;
    String name = "nyct_subways_gtfs_rt.2024-02-21T10:00:33-04:00.pb";

    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, name);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            expectedTripId, null, expectedHeadsign, 0);
  }

  @Test
  public void test4() throws Exception {
    // this pattern breaks head-signs (and A/D on prod)
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_A","MTASBWY_B","MTASBWY_C","MTASBWY_D");
    String expectedStopId = "MTASBWY_A15S";
    String expectedRouteId = "MTASBWY_D";
    String expectedTripId = "MTASBWY_053550_D..S14R";
    String expectedHeadsign = "Coney Island-Stillwell Av";
    String expectedVehicleId = "MTASBWY_1D 0855+ 205/STL";
    String path = getIntegrationTestPath() + File.separator;
    String name = "nyct_subways_gtfs_rt.2024-02-28T09:11:26:00.pb";

    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, name);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            expectedTripId, expectedVehicleId, expectedHeadsign, 4);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_054050_D..S07R", "MTASBWY_1D 0900+ 205/STL", expectedHeadsign, 16);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_055100_D..S07R", "MTASBWY_1D 0911 205/STL", expectedHeadsign, 27);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_056100_D..S07R", "MTASBWY_1D 0921 205/STL", expectedHeadsign, 37);
  }

  @Test
  // MTASBWY_049500_D..S14R -> MTASBWY_049900_D..S14R MTASBWY_1D 0815 205/STL -> MTASBWY_1D 0819 BPK/STL
  // MTASBWY_050100_D..S14R -> MTASBWY_050500_D..S14R MTASBWY_1D 0821 205/STL -> MTASBWY_1D 0825 BPK/STL
  public void test5() throws Exception {
    // another example of missing D trips
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_A","MTASBWY_B","MTASBWY_C","MTASBWY_D");
    String expectedStopId = "MTASBWY_A15S";
    String expectedRouteId = "MTASBWY_D";
    String[] expectedTripIds = Arrays.asList(/*"MTASBWY_044700_D..S14R", "MTASBWY_045700_D..S14R", "MTASBWY_046300_D..S14R",
            "MTASBWY_047050_D..S14R", "MTASBWY_047700_D..S14R", "MTASBWY_048600_D..S14R",*/
            "MTASBWY_049900_D..S14R"/*, "MTASBWY_050500_D..S14R", "MTASBWY_050850_D..S14R"*/).toArray(new String[0]);
    String expectedHeadsign = "Coney Island-Stillwell Av";
    String[] expectedVehicleIds = Arrays.asList(/*"MTASBWY_1D 0727 BPK/STL", "MTASBWY_1D 0737 BPK/STL", "MTASBWY_1D 0743 205/STL",
            "MTASBWY_1D 0750+ 205/STL", "MTASBWY_1D 0757 205/STL", "MTASBWY_1D 0806 205/STL",*/
            "MTASBWY_1D 0819 BPK/STL"/*, "MTASBWY_1D 0825 BPK/STL", "MTASBWY_1D 0828+ 205/STL"*/).toArray(new String[0]);
    int[] expectedArrivals = {7};
    String path = getIntegrationTestPath() + File.separator;
    String name = "nyct_subways_gtfs_rt.2024-03-04T08:28:35:00.pb";
    /* Mon Mar  4 08:28:35 EST 2024
    -A15S 044700_D..S14R     D     044700_D..S14R     Coney Island-Stillwell Av       Ar 0744 (3m)   1D 0727 BPK/STL    A3
    -A15S 045700_D..S14R     D     045700_D..S14R     Coney Island-Stillwell Av       Ar 0754 (13m)  1D 0737 BPK/STL    A3
    -A15S 046300_D..S14R     D     046300_D..S14R     Coney Island-Stillwell Av       Ar 0805 (24m)  1D 0743 205/STL    A3                             --- MISSING TRIP ---                                                                            --- MISSING TRIP ---
    -A15S 047050_D..S14R     D     047050_D..S14R     Coney Island-Stillwell Av       Ar 0812 (31m)  1D 0750+ 205/STL   A3
    -A15S 047700_D..S14R     D     047700_D..S14R     Coney Island-Stillwell Av       Ar 0819 (38m)  1D 0757 205/STL    A3                             --- MISSING TRIP ---                                                                            --- MISSING TRIP ---
    -A15S 048600_D..S14R     D     048600_D..S14R     Coney Island-Stillwell Av       Ar 0825 (44m)  1D 0806 205/STL    A3                             --- MISSING TRIP ---                                                                            --- MISSING TRIP ---
    A15S 049500_D..S14R     D     049500_D..S14R     Coney Island-Stillwell Av       Ar 0837 (56m)  1D 0815 205/STL    A3                             --- MISSING TRIP ---                                                                            --- MISSING TRIP ---
    A15S 050100_D..S14R     D     050100_D..S14R     Coney Island-Stillwell Av       Ar 0843 (1h)   1D 0821 205/STL    A3   unassigned                          --- MISSING TRIP ---                                                                            --- MISSING TRIP ---
    A15S 050850_D..S14R     D     050850_D..S14R     Coney Island-Stillwell Av       Ar 0850 (1h)   1D 0828+ 205/STL   A3   unassigned                          --- MISSING TRIP ---                                                                            --- MISSING TRIP ---
     */

    assertEquals(expectedTripIds.length, expectedVehicleIds.length);
    assertEquals(expectedTripIds.length, expectedArrivals.length);
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, name);
    for (int i=0; i < expectedTripIds.length; i++ ) {
      expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
              expectedTripIds[i], expectedVehicleIds[i], expectedHeadsign, expectedArrivals[i]);
    }
  }

  /**
   * receive 4 updates of mutating tripIds and confirm expected tripIds are seen each time
   * @throws Exception
   */
  @Test
  public void test7() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_A","MTASBWY_B","MTASBWY_C","MTASBWY_D");
    String expectedStopId = "MTASBWY_D14S";
    String expectedRouteId = "MTASBWY_D";
    String path = getIntegrationTestPath() + File.separator;

    // part I: expect 043300_D..S14R  1D 0713 205/STL 60mins
    // D01S at 7:13 (only lists 2 stops!!!), next D03S
    String part1 = "nyct_subways_gtfs_rt.2024-03-05T06:14:33.pb";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, "MTASBWY_D01S", path, part1);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_D01S", expectedRouteId,
            "MTASBWY_043300_D..S14R", "MTASBWY_1D 0713 205/STL", "Bedford Park Blvd", 58, 0);

    // part II: expect 043300_D..S14R  1D 0713 205/STL 57mins
    String part2 = "nyct_subways_gtfs_rt.2024-03-05T06:17:43.pb";
    // now has 30 stops, D01S at 7:13:30
    // and therefor headsign should change!!!!
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part2);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_D01S", expectedRouteId,
            "MTASBWY_043300_D..S14R", "MTASBWY_1D 0713 205/STL", "Coney Island-Stillwell Av", 55, 1.0);
    // AND D14S at 7:44:30
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_043300_D..S14R", "MTASBWY_1D 0713 205/STL", "Coney Island-Stillwell Av", 86, 1.0);
    // AND D43 (last stop)
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_D43S", expectedRouteId,
            "MTASBWY_043300_D..S14R", "MTASBWY_1D 0713 205/STL", "Coney Island-Stillwell Av", 141, 1.0);




    // part III: expect 043300_D..S14R  1D 0713 205/STL 3mins
    String part3 = "nyct_subways_gtfs_rt.2024-03-05T07:17:39.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part3);
    // ok things get interesting here!  we loose the trip, even tho the feed still hav a D14S 7:44 arrival
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_043300_D..S14R", "MTASBWY_1D 0713 205/STL", "Coney Island-Stillwell Av", 26, 1.0);

    // part IV: expect 043700_D..S14R  1D 0717 BPK/STL (departed)
    // grab a downstream stop....D10S is first guess
    String part4 = "nyct_subways_gtfs_rt.2024-03-05T07:43:01.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, "MTASBWY_D10S", path, part4);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_043700_D..S14R", "MTASBWY_1D 0717 BPK/STL", "Coney Island-Stillwell Av", 0, 53.82);

  }

  @Test
  /*
   * If the first stop on a trip is a wrong way concurrency verify that predictions still match.
   */
  public void test8() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_B","MTASBWY_D","MTASBWY_F","MTASBWY_M");
    String expectedStopId = "MTASBWY_M11S"; // wrong way stop, scheduled as M11N
    String expectedRouteId = "MTASBWY_M";
    String path = getIntegrationTestPath() + File.separator;
    String part1 = "nyct_subways_gtfs_rt.2024-03-05T19:58:50.pb";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_118550_M..S20X009", "MTASBWY_1M 1945+ 576/MET", "Middle Village-Metropolitan Av", 17,119.10);
    // when train passes Broadway-Lafayette it drops out of system
    // Missing data for Essex St to Myrtle Av
    // (also late night trips running from Myrtle Ave to Met don't show Myrtle Ave
    // M stopping pattern
    // Delancey St-Essex St: M18S (but actually M18N for wrong way concurrency)
    // Myrtle: M11S
    String part2 = "nyct_subways_gtfs_rt.2024-03-05T19:59:50.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part2);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_118550_M..S20X009", "MTASBWY_1M 1945+ 576/MET", "Middle Village-Metropolitan Av", 15, 1.0);
  }

  /**
   * Test for a drop in service at 00:37.
   */
  @Test
  public void test9() throws Exception {

    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_A","MTASBWY_B","MTASBWY_C","MTASBWY_D");
    String expectedStopId = "MTASBWY_A02N";
    String expectedRouteId = "MTASBWY_A";
    String path = getIntegrationTestPath() + File.separator;
    String part1 = "nyct_subways_gtfs_rt.2024-03-4T00:25:58-04:00.pb";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 36, 45.56);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 45, 99.78);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 58, 76.38);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 15, 98.77);

    String part2 = "nyct_subways_gtfs_rt.2024-03-4T00:26:58-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part2);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 35, 349.31);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 45, 834.05);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 56, 98.33);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 15, 815.15);

    String part3 = "nyct_subways_gtfs_rt.2024-03-4T00:27:58-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part3);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 33, 114.08); // NOTE lower, but it is correct
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 46,1370.364);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 55, 820.18);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 15, 1189.50);

    String part4 = "nyct_subways_gtfs_rt.2024-03-4T00:28:58-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part4);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 33, 640.62); // NOTE jump back
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 45, 1906.67);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 55, 94.18); // NOTE lower
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 15, 1588.26);

    String part5 = "nyct_subways_gtfs_rt.2024-03-4T00:29:58-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part5);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 33, 1167.16);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 44, 167.59); // NOTE lower
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 53, 119.19);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 16, 2328.74);

    String part6 = "nyct_subways_gtfs_rt.2024-03-4T00:30:58-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part6);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 33, 1614.15);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 42, 1370.20); // NOTE jump back
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 53, 984.15);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 13, 89.39); // NOTE lower

    String part7 = "nyct_subways_gtfs_rt.2024-03-4T00:31:58-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part7);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 32, 1.0); // NOTE reset
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 41, 78.26); // NOTE lower
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 52, 66.34); // NOTE lower
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 14, 773.38); // NOTE jump back

    String part8 = "nyct_subways_gtfs_rt.2024-03-4T00:32:58-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part8);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 33, 1.0);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 40, 600.03);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 50, 506.10);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 11, 158.67);

    String part9 = "nyct_subways_gtfs_rt.2024-03-4T00:33:58-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part9);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 32, 379.73);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 39, 85.67);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 50, 115.82);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 10, 1216.50);

    String part10 = "nyct_subways_gtfs_rt.2024-03-4T00:34:59-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part10);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 31, 765.90);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 38, 656.81);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 51, 875.96);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 11, 1.0);


    String part11 = "nyct_subways_gtfs_rt.2024-03-4T00:35:09-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part11);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 30, 830.26);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 38, 752.00);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 50, 980.65);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 10, 1.0);

    String part12 = "nyct_subways_gtfs_rt.2024-03-4T00:35:19-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part12);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 30, 894.62);

    String part13 = "nyct_subways_gtfs_rt.2024-03-4T00:35:29-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part13);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 30, 968.04);

    String part14 = "nyct_subways_gtfs_rt.2024-03-4T00:35:39-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part14);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 30, 1050.52);

    String part15 = "nyct_subways_gtfs_rt.2024-03-4T00:35:49-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part15);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 30, 1133.00);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 38, 1.0);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 48, 1.0);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), "MTASBWY_A28S", "MTASBWY_E",
            "MTASBWY_142350_E..S04R", "MTASBWY_1E 2343+ P-A/WTC", "World Trade Center", 11, 1.0);
  }

  /**
   * Test stale feed behavour.
   * @throws Exception
   */
  @Test
  public void test10() throws Exception {

    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_A", "MTASBWY_B", "MTASBWY_C", "MTASBWY_D");
    String expectedStopId = "MTASBWY_A09N";
    String expectedRouteId = "MTASBWY_A";
    String path = getIntegrationTestPath() + File.separator;

    String part1 = "nyct_subways_gtfs_rt.2024-03-04T00:02:50-04:00.pb";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_136500_A..N", "MTASBWY_1A 2245 LEF/207", "Inwood-207 St", 4);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_136700_A..N", "MTASBWY_1A 2247 FAR/207", "Inwood-207 St", 26);

    for (int i=0; i<10; i++) {
      // feed was stale/repeated for 10 minutes
      source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
      expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
              "MTASBWY_136500_A..N", "MTASBWY_1A 2245 LEF/207", "Inwood-207 St", 4);
      expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
              "MTASBWY_136700_A..N", "MTASBWY_1A 2247 FAR/207", "Inwood-207 St", 26);
    }

    String part2 = "nyct_subways_gtfs_rt.2024-03-04T00:13:15-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part2);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_136700_A..N", "MTASBWY_1A 2247 FAR/207", "Inwood-207 St", 17);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_143050_A..N", "MTASBWY_EA 2350+ CAN/207", "Inwood-207 St", 4);
  }

  @Test
  /*
   * Incorrect headsigns during construction/maintenance.
   * Should not be showing Flusing-Main St not 74 St-Broadway
   */
  public void test11() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_7");
    String expectedStopId = "MTASBWY_701S";
    String expectedRouteId = "MTASBWY_7";
    String path = getIntegrationTestPath() + File.separator;
    String part1 = "nyct_subways_gtfs_rt.2024-03-19T09:27:19-04:00.pb";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_060100_7..S", "MTASBWY_07 1001 MST/34H", "34 St-Hudson Yards", 34);
    // second update
    String part2 = "nyct_subways_gtfs_rt.2024-03-19T09:27:35-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part2);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_060100_7..S", "MTASBWY_07 1001 MST/34H", "34 St-Hudson Yards", 34);
  }

  /**
   * Missing trips after bad behaviour on A/C/E
   *
   */
  @Test
  public void test12() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_A,MTASBWY_C,MTASBWY_E");
    String expectedStopId = "MTASBWY_A15N";
    String expectedRouteId = "MTASBWY_A";
    String path = getIntegrationTestPath() + File.separator;
    String part1 = "nyct_subways_gtfs_rt.2024-03-19T00:35:06-04:00.pb";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 3);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 9);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 39);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_000800_A..N", "MTASBWY_1A 0008 LEF/207", "Inwood-207 St", 45);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_000200_A..N", "MTASBWY_1A 0002 FAR/207", "Inwood-207 St", 52);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_002200_A..N", "MTASBWY_1A 0022 FAR/207", "Inwood-207 St", 69);


    // second update
    String part2 = "nyct_subways_gtfs_rt.2024-03-19T00:35:46-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part2);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_138150_A..N", "MTASBWY_1A 2301+ FAR/207", "Inwood-207 St", 3);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141800_A..N", "MTASBWY_1A 2338 LEF/207", "Inwood-207 St", 9);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141200_A..N", "MTASBWY_1A 2332 FAR/207", "Inwood-207 St", 39);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_000800_A..N", "MTASBWY_1A 0008 LEF/207", "Inwood-207 St", 45);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_000200_A..N", "MTASBWY_1A 0002 FAR/207", "Inwood-207 St", 52);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_002200_A..N", "MTASBWY_1A 0022 FAR/207", "Inwood-207 St", 69);

  }
  /**
   * Missing trips after bad behaviour, as with test12 but 1/2/3
   *
   */
  @Test
  public void test13() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_1,MTASBWY_2,MTASBWY_3");
    String expectedStopId = "MTASBWY_142S";
    String expectedRouteId = "MTASBWY_2";
    String path = getIntegrationTestPath() + File.separator;
    String part1 = "nyct_subways_gtfs_rt.2024-03-19T00:35:04-04:00.pb";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_139250_2..S08X082", "MTASBWY_02 2312+ 241/SFT", "South Ferry", 5);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, "MTASBWY_1",
            "MTASBWY_141850_1..S03R", "MTASBWY_01 2338+ 242/SFT", "South Ferry", 8);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_140650_2..S08X082", "MTASBWY_02 2326+ 241/SFT", "South Ferry", 11);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, "MTASBWY_1",
            "MTASBWY_143250_1..S03R", "MTASBWY_01 2352+ 242/SFT", "South Ferry", 13);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141900_2..S08X082", "MTASBWY_02 2339  241/SFT", "South Ferry", 22);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, "MTASBWY_1",
            "MTASBWY_000650_1..S03R", "MTASBWY_01 0006+ 242/SFT", "South Ferry", 28);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_143900_2..S08X082", "MTASBWY_02 2359  241/SFT", "South Ferry", 36);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, "MTASBWY_1",
            "MTASBWY_002550_1..S03R", "MTASBWY_01 0025+ 242/SFT", "South Ferry", 47);

    // second update
    String part2 = "nyct_subways_gtfs_rt.2024-03-19T00:35:49-04:00.pb";
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part2);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_139250_2..S08X082", "MTASBWY_02 2312+ 241/SFT", "South Ferry", 3);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, "MTASBWY_1",
            "MTASBWY_141850_1..S03R", "MTASBWY_01 2338+ 242/SFT", "South Ferry", 8);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_140650_2..S08X082", "MTASBWY_02 2326+ 241/SFT", "South Ferry", 11);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, "MTASBWY_1",
            "MTASBWY_143250_1..S03R", "MTASBWY_01 2352+ 242/SFT", "South Ferry", 13);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_141900_2..S08X082", "MTASBWY_02 2339  241/SFT", "South Ferry", 22);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, "MTASBWY_1",
            "MTASBWY_000650_1..S03R", "MTASBWY_01 0006+ 242/SFT", "South Ferry", 28);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            "MTASBWY_143900_2..S08X082", "MTASBWY_02 2359  241/SFT", "South Ferry", 36);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, "MTASBWY_1",
            "MTASBWY_002550_1..S03R", "MTASBWY_01 0025+ 242/SFT", "South Ferry", 45);

  }

  @Test
  public void test14a() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_A,MTASBWY_C,MTASBWY_E");
    String expectedStopId = "MTASBWY_A02N";
    String expectedRouteId = "MTASBWY_A";
    String path = getIntegrationTestPath() + File.separator;
    String tripId = "MTASBWY_141800_A..N";
    String vehicleId = "MTASBWY_1A 2338 LEF/207";
    String headsign = "Inwood-207 St";
    String part1 = "gtfs-ace-03202024-003200";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            tripId, vehicleId, headsign, 48);

  }

  /**
   * MTA-114 trips drop between 00:36 and 00:44
   * @throws Exception
   */
  @Test
  public void test14() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_A,MTASBWY_C,MTASBWY_E");
    String expectedStopId = "MTASBWY_A02N";
    String expectedRouteId = "MTASBWY_A";
    String path = getIntegrationTestPath() + File.separator;
    String tripId = "MTASBWY_141800_A..N";
    String vehicleId = "MTASBWY_1A 2338 LEF/207";
    String headsign = "Inwood-207 St";

    // MISSING trips after 00:35

    String[] files = {
//            "gtfs-ace-03192024-235707", "68",
            "gtfs-ace-03192024-235807", "61",
//            "gtfs-ace-03192024-235907", "60",
//            "gtfs-ace-03192024-235957", "59",
//            "gtfs-ace-03202024-000007", "59",
//            "gtfs-ace-03202024-000457", "54",
//            "gtfs-ace-03202024-000958", "65",
//            "gtfs-ace-03202024-001458", "60",
//            "gtfs-ace-03202024-001959", "55",
//            "gtfs-ace-03202024-002459", "53",
//            "gtfs-ace-03202024-002559", "52",
//            "gtfs-ace-03202024-002659", "51",
//            "gtfs-ace-03202024-002800", "50",
//            "gtfs-ace-03202024-002900", "51",
//            "gtfs-ace-03202024-003000", "50",
//            "gtfs-ace-03202024-003100", "49",
            "gtfs-ace-03202024-003200", "48",
//            "gtfs-ace-03202024-003300", "47",
//            "gtfs-ace-03202024-003400", "46",
//            "gtfs-ace-03202024-003500", "45",
//            "gtfs-ace-03202024-003600", "44",
//            "gtfs-ace-03202024-003700", "43",
//            "gtfs-ace-03202024-003801", "42",
//            "gtfs-ace-03202024-003901", "41"
    };
    testForPredictions(routeIdsToCancel, expectedRouteId, expectedStopId, path,
            tripId, vehicleId, headsign,
            files);

  }

  /**
   * Short turn L-trains have the incorrect headisgn.
   * @throws Exception
   */
  @Test
  public void test15() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_L");
    String expectedStopId = "MTASBWY_L17N";
    String expectedRouteId = "MTASBWY_L";
    String path = getIntegrationTestPath() + File.separator;
    String tripId = "MTASBWY_143450_L..N";
    String vehicleId = "MTASBWY_0L 2354+RPY/LOR";
    String headsign = "Lorimer St";

    String part0 = "gtfs-l-03222024-232658";  // trip pattern goes through to L01
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part0);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            tripId, "MTASBWY_0L 2354+RPY/8AV", "8 Av", 43, 0.0); // first update

    String part1 = "gtfs-l-03222024-233513"; //L01=8 Av; L10=Lorimer St
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part1);
    // this update is unassigned and dropped!
//    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
//            tripId, vehicleId, headsign, 35);

    // train is assigned late!
    String part2 = "gtfs-l-03222024-234728"; /// short turn to L10
    source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part2);
    expectArrivalAndTripAndHeadsignDistance(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            tripId, vehicleId, headsign, 23, 1.0);

  }

  /**
   * Missing L trip after 23:30
   * @throws Exception
   */
  @Test
  public void test16() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_L");
    String expectedStopId = "MTASBWY_L17N";
    String expectedRouteId = "MTASBWY_L";
    String path = getIntegrationTestPath() + File.separator;
    String tripId = "MTASBWY_000650_L..N";
    String vehicleId = "MTASBWY_0L 0006+RPY/LOR";
    String headsign = "Lorimer St";

    String part0 = "gtfs-l-03222024-233958";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part0);
    expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
            tripId, vehicleId, headsign, 41);

  }

  /**
   * Confirm that in progress trips with is_assigned=false are suppressed (MTA-148)
   * @throws Exception
   */
  @Test
  public void test17() throws Exception {
    List<String> routeIdsToCancel = Arrays.asList("MTASBWY_6");
    String expectedStopId = "MTASBWY_631N";
    String expectedRouteId = "MTASBWY_6";
    String path = getIntegrationTestPath() + File.separator;
    String tripId = "MTASBWY_067800_6..N01X017";
    String vehicleId = "MTASBWY_06 1118  BBR/WSQ";
    String headsign = "Westchester Sq-E Tremont Av";

    String part0 = "gtfs-06242024-112957";
    GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, part0);
    try {
      expectArrivalAndTripAndHeadsign(source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
              tripId, vehicleId, headsign, 1);
      fail("expected trip " + tripId + " to be missing as its unassigned");
    } catch (AssertionError ae) {
      // success -- trip should not be found.
    }

  }

  private void testForPredictions(List<String> routeIdsToCancel, String expectedRouteId, String expectedStopId,
                                  String path, String tripId, String vehicleId, String headsign,
                                  String[] files) throws Exception {
    int i = 0;
    while (i < files.length) {
      GtfsRealtimeSource source = runRealtime(routeIdsToCancel, expectedRouteId, expectedStopId, path, files[i]);
      expectArrivalAndTripAndHeadsign("run[" + i + "]=" + files[i], source.getGtfsRealtimeTripLibrary().getCurrentTime(), expectedStopId, expectedRouteId,
              tripId, vehicleId, headsign, Integer.parseInt(files[i+1]));
      i+=2;
    }

  }

}
