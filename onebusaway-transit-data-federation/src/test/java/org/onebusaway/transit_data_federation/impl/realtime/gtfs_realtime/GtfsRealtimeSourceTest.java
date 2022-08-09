package org.onebusaway.transit_data_federation.impl.realtime.gtfs_realtime;

import static org.junit.Assert.assertEquals;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtimeConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsRealtimeSourceTest {
    private GtfsRealtimeEntitySource _entitySource;

    private static final long NOW = System.currentTimeMillis();
    private static final long DAY = 24 * 60 * 60 * 1000;
    private static final String TEST_1 = "Test Alert 1";
    private static final String TEST_2 = "Test Alert 2";
    private static final String TEST_3 = "Test Alert 3";
    private static final String DESC_1 = "Construction delay at Mass Ave and Boylston";
    private static final String DESC_2 = "Car stuck on tracks at Heath St";
    private static final String DESC_3 = "Flooding near Park St";
    private static final GtfsRealtime.Alert.Cause CAUSE_1 = GtfsRealtime.Alert.Cause.CONSTRUCTION;
    private static final GtfsRealtime.Alert.Cause CAUSE_2 = GtfsRealtime.Alert.Cause.ACCIDENT;
    private static final GtfsRealtime.Alert.Cause CAUSE_3 = GtfsRealtime.Alert.Cause.WEATHER;
    private static final GtfsRealtime.Alert.Effect EFFECT_1 = GtfsRealtime.Alert.Effect.SIGNIFICANT_DELAYS;
    private static final GtfsRealtime.Alert.Effect EFFECT_2 = GtfsRealtime.Alert.Effect.DETOUR;
    private static final GtfsRealtime.Alert.Effect EFFECT_3 = GtfsRealtime.Alert.Effect.REDUCED_SERVICE;
    private static final String URL_1 = "http://SomeUrl.org";
    private static final String URL_2 = "http://AnotherUrl.org";
    private static final String URL_3 = "http://ThirdUrl.org";
    private static final long TIME_START_1 = NOW;
    private static final long TIME_START_2 = NOW - DAY;
    private static final long TIME_START_3 = NOW - (DAY * 2);
    private static final long TIME_END_1 = NOW + DAY;
    private static final long TIME_END_2 = NOW + (DAY * 2);
    private static final long TIME_END_3 = NOW + (DAY * 3);
    private static final String AGENCY_1 = "1";
    private static final String AGENCY_2 = "3";
    private static final String AGENCY_3 = "19";
    private static final String ROUTE_1 = null;
    private static final String ROUTE_2 = "1225";
    private static final String ROUTE_3 = null;
    private static final String STOP_1 = null;
    private static final String STOP_2 = null;
    private static final String STOP_3 = "402";

    private GtfsRealtimeSource _source;

    @Before
    public void Before(){
        _source = new GtfsRealtimeSource();
    }

    @Test
    public void testFilterAlerts(){

        // Create GTFS Feed with service alerts
        GtfsRealtime.FeedEntity alertEntityA = createAlert("alertA", TEST_1, DESC_1, CAUSE_1,
                EFFECT_1, URL_1, TIME_START_1, TIME_END_1, AGENCY_1, ROUTE_1, STOP_1);
        GtfsRealtime.FeedEntity alertEntityB = createAlert("alertB", TEST_2, DESC_2, CAUSE_2,
                EFFECT_2, URL_2, TIME_START_2, TIME_END_2, AGENCY_2, ROUTE_2, STOP_2);
        GtfsRealtime.FeedEntity alertEntityC = createAlert("alertC", TEST_3, DESC_3, CAUSE_3,
                EFFECT_3, URL_3, TIME_START_3, TIME_END_3, AGENCY_3, ROUTE_3, STOP_3);

        // Create FeedMessage
        GtfsRealtime.FeedMessage.Builder unfilteredAlertFeed = createFeed();
        unfilteredAlertFeed.addEntity(alertEntityA);
        unfilteredAlertFeed.addEntity(alertEntityB);
        unfilteredAlertFeed.addEntity(alertEntityC);
        GtfsRealtime.FeedMessage alerts = unfilteredAlertFeed.build();

        GtfsRealtime.FeedMessage.Builder filteredAlertFeed = createFeed();
        filteredAlertFeed.addEntity(alertEntityA);
        filteredAlertFeed.addEntity(alertEntityC);
        GtfsRealtime.FeedMessage filteredAlerts = filteredAlertFeed.build();

        _source.setFilterRegex("");


        GtfsRealtime.FeedMessage result = _source.filterAlerts(alerts);

        assertEquals(result, filteredAlerts);





    }

    private GtfsRealtime.FeedEntity createAlert(String alertId, String header, String desc,
                                                GtfsRealtime.Alert.Cause cause, GtfsRealtime.Alert.Effect effect, String url, long startTime,
                                                long endTime, String agency, String route, String stop) {
        GtfsRealtime.Alert.Builder alertBldr = GtfsRealtime.Alert.newBuilder();

        // Header
        GtfsRealtime.TranslatedString.Translation translation = GtfsRealtime.TranslatedString.Translation.newBuilder().setLanguage(
                "en").setText(header).build();
        GtfsRealtime.TranslatedString trnStr = GtfsRealtime.TranslatedString.newBuilder().addTranslation(
                translation).build();
        alertBldr.setHeaderText(trnStr);

        // Description
        translation = GtfsRealtime.TranslatedString.Translation.newBuilder().setLanguage("en").setText(
                desc).build();
        trnStr = GtfsRealtime.TranslatedString.newBuilder().addTranslation(translation).build();
        alertBldr.setDescriptionText(trnStr);

        // Cause
        alertBldr.setCause(cause);

        // Effect
        alertBldr.setEffect(effect);

        // URL
        translation = GtfsRealtime.TranslatedString.Translation.newBuilder().setLanguage("en").setText(
                url).build();
        trnStr = GtfsRealtime.TranslatedString.newBuilder().addTranslation(translation).build();
        alertBldr.setUrl(trnStr);

        // Build TimeRangeEntity
        GtfsRealtime.TimeRange timeRange = createTimeRange(startTime, endTime);
        alertBldr.addActivePeriod(timeRange);

        // Build EntitySelectorEntity
        GtfsRealtime.EntitySelector entitySelector = createEntitySelector(agency, route, stop);
        alertBldr.addInformedEntity(entitySelector);

        GtfsRealtime.FeedEntity.Builder alertEntity = GtfsRealtime.FeedEntity.newBuilder();
        alertEntity.setId(alertId);
        alertEntity.setAlert(alertBldr.build());
        return alertEntity.build();
    }

    private GtfsRealtime.TimeRange createTimeRange(long startTime, long endTime) {
        GtfsRealtime.TimeRange.Builder timeRange = GtfsRealtime.TimeRange.newBuilder();
        timeRange.setStart(startTime);
        timeRange.setEnd(endTime);
        return timeRange.build();
    }

    private GtfsRealtime.EntitySelector createEntitySelector(String agencyId, String route,
                                                             String stop) {
        GtfsRealtime.EntitySelector.Builder entitySelector = GtfsRealtime.EntitySelector.newBuilder();
        entitySelector.setAgencyId(agencyId);
        if (route != null) {
            entitySelector.setRouteId(route);
        }
        if (stop != null) {
            entitySelector.setStopId(stop);
        }
        return entitySelector.build();
    }
    private GtfsRealtime.FeedMessage.Builder createFeed() {
        GtfsRealtime.FeedMessage.Builder builder = GtfsRealtime.FeedMessage.newBuilder();
        GtfsRealtime.FeedHeader.Builder header = GtfsRealtime.FeedHeader.newBuilder();
        header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
        builder.setHeader(header);
        return builder;
    }

}
