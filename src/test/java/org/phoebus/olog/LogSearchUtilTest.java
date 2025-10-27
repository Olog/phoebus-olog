/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.olog;

import org.junit.jupiter.api.Test;
import org.phoebus.util.time.TimestampFormats;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;
import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT_WITH_TZ;


@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
class LogSearchUtilTest {

    private LogSearchUtil logSearchUtil = new LogSearchUtil();

    @Test
    void checkForInvalidTimeRanges() {
        String expectedMessage = "CAUSE: Invalid start and end times";

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // start time is after the end time
        Instant now = Instant.now();
        params.put("start", List.of(MILLI_FORMAT.format(now.plusMillis(1000))));
        params.put("end",   List.of(MILLI_FORMAT.format(now.minusMillis(1000))));

        Exception exception = assertThrows(ResponseStatusException.class, () -> logSearchUtil.buildSearchRequest(params));

        params.put("start", List.of(MILLI_FORMAT_WITH_TZ.format(now.plusMillis(1000))));
        params.put("end",   List.of(MILLI_FORMAT_WITH_TZ.format(now.minusMillis(1000))));

        exception = assertThrows(ResponseStatusException.class, () -> logSearchUtil.buildSearchRequest(params));

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    void testGetSearchTerms() {
        String userInput = "";
        List<String> parsed = logSearchUtil.getSearchTerms(userInput);
        assertTrue(parsed.isEmpty());

        userInput = "foo,,neutron";
        parsed = logSearchUtil.getSearchTerms(userInput);
        assertEquals(2, parsed.size());
        assertTrue(parsed.contains("foo"));
        assertTrue(parsed.contains("neutron"));

        userInput = "foo,bar,neutron";
        parsed = logSearchUtil.getSearchTerms(userInput);
        assertEquals(3, parsed.size());
        assertTrue(parsed.contains("foo"));
        assertTrue(parsed.contains("bar"));
        assertTrue(parsed.contains("neutron"));

        userInput = "\"foo\",bar,neutron";
        parsed = logSearchUtil.getSearchTerms(userInput);
        assertEquals(3, parsed.size());
        assertTrue(parsed.contains("\"foo\""));
        assertTrue(parsed.contains("bar"));
        assertTrue(parsed.contains("neutron"));

        userInput = "foo,\"bar\",\"neutron\"";
        parsed = logSearchUtil.getSearchTerms(userInput);
        assertEquals(3, parsed.size());
        assertTrue(parsed.contains("foo"));
        assertTrue(parsed.contains("\"bar\""));
        assertTrue(parsed.contains("\"neutron\""));

        userInput = "\"foo,bar\",\"bar|foo\",\"neutron;proton\",\"electron muon\"";
        parsed = logSearchUtil.getSearchTerms(userInput);
        assertEquals(4, parsed.size());
        assertTrue(parsed.contains("\"foo,bar\""));
        assertTrue(parsed.contains("\"bar|foo\""));
        assertTrue(parsed.contains("\"neutron;proton\""));
        assertTrue(parsed.contains("\"electron muon\""));

        final String _userInput = "foo,\"bar\",\"neutron";
        assertThrows(IllegalArgumentException.class, () -> logSearchUtil.getSearchTerms(_userInput));
    }

    @Test
    public void testGetTimeZone(){

        MultiValueMap<String, String> searchParams = new LinkedMultiValueMap<>();
        searchParams.put("TZ", List.of("Europe/Stockholm"));

        TimeZone timeZone = logSearchUtil.getTimezone(searchParams);
        assertEquals("Europe/Stockholm", timeZone.toZoneId().toString());
        assertEquals(3600000, timeZone.getRawOffset());

        searchParams = new LinkedMultiValueMap<>();
        searchParams.put("start", List.of("1970-01-01 00:00:00.000"));
        searchParams.put("tz", List.of("CET"));

        timeZone = logSearchUtil.getTimezone(searchParams);
        assertEquals("CET", timeZone.toZoneId().toString());
        assertEquals(3600000, timeZone.getRawOffset());

        searchParams = new LinkedMultiValueMap<>();
        searchParams.put("tz", List.of("invalid"));

        timeZone = logSearchUtil.getTimezone(searchParams);
        assertEquals("GMT", timeZone.toZoneId().toString());
        assertEquals(0, timeZone.getRawOffset());
    }

    @Test
    public void testDetermineDateAndTime(){

        System.setProperty("user.timezone", "UTC");

        Map.Entry<String, List<String>> startParameter = new AbstractMap.SimpleEntry<>("start", List.of("2025-10-01 12:00:00.000"));

        ZonedDateTime zonedDateTime = logSearchUtil.determineDateAndTime(startParameter,
                TimeZone.getTimeZone("CET"));
        System.out.println("Local time in CET " + zonedDateTime.toLocalDateTime().toString());
        assertTrue("2025-10-01T14:00".equals(zonedDateTime.toLocalDateTime().toString()) || "2025-10-01T13:00".equals(zonedDateTime.toLocalDateTime().toString()));
        assertTrue("+02:00".equals(zonedDateTime.getOffset().toString()) || "+01:00".equals(zonedDateTime.getOffset().toString()));

        zonedDateTime = logSearchUtil.determineDateAndTime(startParameter,
                TimeZone.getTimeZone("GMT"));
        System.out.println("Local time in GMT " + zonedDateTime.toLocalDateTime().toString());
        assertTrue("2025-10-01T12:00".equals(zonedDateTime.toLocalDateTime().toString()));
        assertTrue("Z".equals(zonedDateTime.getOffset().toString()));

        zonedDateTime = logSearchUtil.determineDateAndTime(startParameter,
                TimeZone.getTimeZone("EST"));

        System.out.println("Local time in EST " + zonedDateTime.toLocalDateTime().toString());
        assertTrue("2025-10-01T07:00".equals(zonedDateTime.toLocalDateTime().toString()) || "2025-10-01T06:00".equals(zonedDateTime.toLocalDateTime().toString()));
        assertTrue("-05:00".equals(zonedDateTime.getOffset().toString()) || "-04:00".equals(zonedDateTime.getOffset().toString()));

    }

    @Test
    public void testDetermineDateAndTimeTemporalAmounts(){

        Map.Entry<String, List<String>> startParameter = new AbstractMap.SimpleEntry<>("start", List.of("2 days"));
        // This should not throw exception
        logSearchUtil.determineDateAndTime(startParameter, TimeZone.getTimeZone("CET"));

        startParameter = new AbstractMap.SimpleEntry<>("start", List.of("2 weeks"));
        // This should not throw exception
        logSearchUtil.determineDateAndTime(startParameter, TimeZone.getTimeZone("CET"));

        Map.Entry<String, List<String>>  _startParameter = new AbstractMap.SimpleEntry<>("start", List.of("2 months"));
        assertThrows(ResponseStatusException.class, () -> logSearchUtil.determineDateAndTime(_startParameter, TimeZone.getTimeZone("CET")));
    }

}
