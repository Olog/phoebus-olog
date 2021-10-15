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

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
public class LogSearchUtilTest {

    @Test
    public void testSortOrder() {
        LogSearchUtil logSearchUtil = new LogSearchUtil();

        // Test DESC and ASC
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("sort", Arrays.asList("asc"));

        // Explicit ascending
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

    }
}
