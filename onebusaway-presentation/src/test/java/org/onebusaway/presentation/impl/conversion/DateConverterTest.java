/**
 * Copyright (C) 2026 Cambridge Systematics, Inc.
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
package org.onebusaway.presentation.impl.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.struts2.conversion.TypeConversionException;
import org.junit.Test;

/**
 * Pins how {@link DateConverter} turns request parameters into dates, so the Struts 6 move
 * (OBANYC-4247) is proven not to change API behavior.
 */
public class DateConverterTest {

  private final DateConverter _converter = new DateConverter();

  @Test
  public void millisecondsBecomeDate() {
    Object result = _converter.convertFromString(null, new String[] {"1762300800000"}, Date.class);
    assertEquals(new Date(1762300800000L), result);
  }

  @Test
  public void millisecondsStayLongForLongTarget() {
    assertEquals(1762300800000L,
        _converter.convertFromString(null, new String[] {"1762300800000"}, Long.TYPE));
  }

  @Test
  public void isoDateParsesInDefaultTimeZone() throws Exception {
    Date expected = new SimpleDateFormat("yyyy-MM-dd").parse("2026-11-04");
    assertEquals(expected,
        _converter.convertFromString(null, new String[] {"2026-11-04"}, Date.class));
  }

  @Test
  public void emptyOrMissingValueGivesNull() {
    assertNull(_converter.convertFromString(null, null, Date.class));
    assertNull(_converter.convertFromString(null, new String[] {""}, Date.class));
  }

  @Test(expected = TypeConversionException.class)
  public void badValueThrowsStrutsConversionError() {
    _converter.convertFromString(null, new String[] {"not-a-date"}, Date.class);
  }

  @Test
  public void dateFormatsBackToIsoDate() throws Exception {
    Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2026-11-04");
    assertEquals("2026-11-04", _converter.convertToString(null, date));
  }
}
