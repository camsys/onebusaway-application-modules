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
package org.onebusaway.transit_data_federation.util;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class HashUtilTest {

    // getEncodedString

    @Test
    public void getEncodedStringReturnsSameOutputForSameInput() throws NoSuchAlgorithmException {
        String result1 = HashUtil.getEncodedString("trip_a,trip_b");
        String result2 = HashUtil.getEncodedString("trip_a,trip_b");
        assertEquals(result1, result2);
    }

    @Test
    public void getEncodedStringReturnsDifferentOutputForDifferentInput() throws NoSuchAlgorithmException {
        String result1 = HashUtil.getEncodedString("trip_a,trip_b");
        String result2 = HashUtil.getEncodedString("trip_a,trip_c");
        assertNotEquals(result1, result2);
    }

    @Test
    public void getEncodedStringReturnsNonEmptyString() throws NoSuchAlgorithmException {
        String result = HashUtil.getEncodedString("trip_a");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void getEncodedStringHandlesEmptyInput() throws NoSuchAlgorithmException {
        String result = HashUtil.getEncodedString("");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // getJoinedIdentifier

    @Test
    public void getJoinedIdentifierJoinsWithUnderscore() {
        assertEquals("a_b_c", HashUtil.getJoinedIdentifier("a", "b", "c"));
    }

    @Test
    public void getJoinedIdentifierWithSingleInput() {
        assertEquals("a", HashUtil.getJoinedIdentifier("a"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getJoinedIdentifierThrowsOnNullInput() {
        HashUtil.getJoinedIdentifier((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getJoinedIdentifierThrowsOnEmptyInput() {
        HashUtil.getJoinedIdentifier();
    }
}
