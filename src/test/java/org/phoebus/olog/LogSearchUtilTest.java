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
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.phoebus.olog.LogSearchUtil.MILLI_FORMAT;

@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
class LogSearchUtilTest {

    @Test
    void testSortOrder() {
        LogSearchUtil logSearchUtil = new LogSearchUtil();

        // Test DESC and ASC
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("sort", List.of("asc"));

        // Explicit ascending
        /*
        SearchRequest searchRequest = logSearchUtil.buildSearchRequest(params);
        FieldSortBuilder fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.ASC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

        params = new LinkedMultiValueMap<>();
        params.put("sort", Arrays.asList("AsCenDinG"));
        searchRequest = logSearchUtil.buildSearchRequest(params);
        fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.ASC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

        // No sort order => expect descending
        params = new LinkedMultiValueMap<>();
        searchRequest = logSearchUtil.buildSearchRequest(params);
        fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.DESC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

        //Explicit descending
        params = new LinkedMultiValueMap<>();
        params.put("sort", Arrays.asList("desc"));
        searchRequest = logSearchUtil.buildSearchRequest(params);
        fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.DESC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

        params = new LinkedMultiValueMap<>();
        params.put("sort", Arrays.asList("DEsCendiNG"));
        searchRequest = logSearchUtil.buildSearchRequest(params);
        fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.DESC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

        // test UP and DOWN

        params = new LinkedMultiValueMap<>();
        params.put("sort", Arrays.asList("up"));

        // Explicit ascending
        searchRequest = logSearchUtil.buildSearchRequest(params);
        fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.ASC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

        params = new LinkedMultiValueMap<>();
        params.put("sort", Arrays.asList("UPp"));
        searchRequest = logSearchUtil.buildSearchRequest(params);
        fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.ASC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

        //Explicit descending
        params = new LinkedMultiValueMap<>();
        params.put("sort", Arrays.asList("down"));
        searchRequest = logSearchUtil.buildSearchRequest(params);
        fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.DESC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

        params = new LinkedMultiValueMap<>();
        params.put("sort", Arrays.asList("doWNunder"));
        searchRequest = logSearchUtil.buildSearchRequest(params);
        fieldSortBuilder = (FieldSortBuilder) searchRequest.source().sorts().get(0);
        assertEquals(SortOrder.DESC, fieldSortBuilder.order());
        assertEquals("createdDate", fieldSortBuilder.getFieldName());

         */

    }

    @Test
    void checkForInvalidTimeRanges() {
        LogSearchUtil logSearchUtil = new LogSearchUtil();
        String expectedMessage = "CAUSE: Invalid start and end times";

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // start time is after the end time
        Instant now = Instant.now();
        params.put("start", List.of(MILLI_FORMAT.format(now.plusMillis(1000))));
        params.put("end",   List.of(MILLI_FORMAT.format(now.minusMillis(1000))));

        Exception exception = assertThrows(ResponseStatusException.class, () -> logSearchUtil.buildSearchRequest(params));

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    void testGetSearchTerms() {
        LogSearchUtil logSearchUtil = new LogSearchUtil();
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
}
