package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.impl.TimeServiceImpl;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_sometimes.service.TimeService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.TripModificationsChanges;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GtfsTripModificationsHandlerReapplyTest {

    private TimeService _timeService = new TimeServiceImpl();

    private TripModificationsChanges _changes = new TripModificationsChanges();

    @InjectMocks
    private GtfsTripModificationsHandlerImpl _handler;

    private static final byte[] HASH = {1, 2, 3};
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);
    private static final LocalDateTime REAPPLY_TIME = TODAY.plusDays(1).atTime(3, 0);

    @Before
    public void setUp() throws Exception {
        _changes.setHash(HASH);
        //when(_timeService.getCurrentDate()).thenReturn(TODAY);

        // Simulate a prior successful feed application
        setField("_lastKnownHash", HASH);
        setField("_reapplyTime", REAPPLY_TIME);
    }

    @Test
    public void testReapplyTime_null_doesNotEnterBranch() {
        setField("_reapplyTime", null);
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME.plusHours(1));

        assertFalse("Should not apply when _reapplyTime is null",
                _handler.shouldApplyChanges(_changes));
    }

    @Test
    public void testCurrentTime_beforeReapplyTime_doesNotEnterBranch() {
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME.minusMinutes(1));

        assertFalse("Should not apply when current time is before reapply time",
                _handler.shouldApplyChanges(_changes));
    }

    @Test
    public void testCurrentTime_exactlyAtReapplyTime_doesNotEnterBranch() {
        // isAfter is exclusive — exactly at reapply time should not enter the branch
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME);

        assertFalse("Should not apply when current time equals reapply time",
                _handler.shouldApplyChanges(_changes));
    }

    @Test
    public void testCurrentTime_oneSecondAfterReapplyTime_shouldApply() {
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME.plusSeconds(1));

        assertTrue("Should apply when current time is just after reapply time",
                _handler.shouldApplyChanges(_changes));
    }

    @Test
    public void testCurrentTime_wellAfterReapplyTime_shouldApply() {
        when(_timeService.getCurrentTime()).thenReturn(REAPPLY_TIME.plusHours(5));

        assertTrue("Should apply when current time is well after reapply time",
                _handler.shouldApplyChanges(_changes));
    }

    // --- Helper ---

    private void setField(String fieldName, Object value) {
        try {
            java.lang.reflect.Field field =
                    GtfsTripModificationsHandlerImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(_handler, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}