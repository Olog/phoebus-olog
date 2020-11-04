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

package gov.bnl.olog;

import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Logbook;
import gov.bnl.olog.entity.ServiceConfiguration;
import gov.bnl.olog.entity.Tag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(LogResource.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
public class ServiceConfigurationResourceTest extends ResourcesTestBase {

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    private TagRepository tagRepository;

    private Logbook logbook1;
    private Logbook logbook2;

    private Tag tag1;
    private Tag tag2;

    @Before
    public void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");

        tag1 = new Tag("tag1");
        tag2 = new Tag("tag2");

    }

    @Test
    public void testServiceConfiguration() throws Exception{
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));

        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.SERVICE_CONFIGURATION_URI);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        ServiceConfiguration serviceConfiguration = objectMapper.readValue(result.getResponse().getContentAsString(), ServiceConfiguration.class);
        assertTrue(serviceConfiguration.getLogbooks().iterator().hasNext());
        assertTrue(serviceConfiguration.getTags().iterator().hasNext());
        assertTrue(serviceConfiguration.getLevels().size() > 0);
        assertEquals("A", serviceConfiguration.getLevels().get(0));
    }
}
