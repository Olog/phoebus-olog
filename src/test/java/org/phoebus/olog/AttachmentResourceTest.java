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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.InputStreamSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests {@link Log} resource endpoints. The authentication scheme used is the
 * hard coded user/userPass credentials. The {@link LogRepository} is mocked.
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class})})
@WebMvcTest(AttachmentResource.class)
@TestPropertySource(locations = "classpath:no_ldap_test_application.properties")
class AttachmentResourceTest extends ResourcesTestBase {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Test
    void testGetAttachment() throws Exception {
        Attachment attachment = Mockito.mock(Attachment.class);
        InputStreamSource inputStreamSource = Mockito.mock(InputStreamSource.class);
        when(inputStreamSource.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(attachment.getAttachment()).thenReturn(inputStreamSource);
        when(attachment.getFilename()).thenReturn("file.jpg");
        when(attachmentRepository.findById("valid")).thenReturn(Optional.of(attachment));
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.ATTACHMENT_URI + "/valid");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();
        String responseData = result.getResponse().getContentAsString();
        assertEquals("data", responseData);
    }

    @Test
    void testGetAttachmentIOException() throws Exception {
        Attachment attachment = Mockito.mock(Attachment.class);
        InputStreamSource inputStreamSource = Mockito.mock(InputStreamSource.class);
        when(inputStreamSource.getInputStream()).thenThrow(new IOException());
        when(attachment.getAttachment()).thenReturn(inputStreamSource);
        when(attachment.getFilename()).thenReturn("file.jpg");
        when(attachmentRepository.findById("valid")).thenReturn(Optional.of(attachment));
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.ATTACHMENT_URI + "/valid");
        mockMvc.perform(request).andExpect(status().isInternalServerError());
    }

    @Test
    void testGetAttachmentInvalidId() throws Exception {
        when(attachmentRepository.findById("invalid")).thenReturn(Optional.empty());
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.ATTACHMENT_URI + "/invalid");
        mockMvc.perform(request).andExpect(status().isNotFound());
    }
}
