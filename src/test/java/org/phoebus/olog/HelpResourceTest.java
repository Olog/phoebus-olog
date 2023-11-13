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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(LogResource.class)
class HelpResourceTest extends ResourcesTestBase{

    @Test
    void testGetCheatSheet() throws  Exception {
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.HELP_URI + "/CommonmarkCheatsheet")
                .header("Accept-Language", "en-US, en;q=0.9, fr;q=0.8, de;q=0.7, *;q=0.5");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void testGetCheatSheetWithLanguageParameter() throws  Exception {
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.HELP_URI + "/CommonmarkCheatsheet?lang=en");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void testGetSearchHelp() throws  Exception {
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.HELP_URI + "/SearchHelp");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void testGetCheatSheetUnrecognizedAcceptLanguage() throws  Exception {
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.HELP_URI + "/CommonmarkCheatsheet")
                .header("Accept-Language", "xx-YY");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void testGetCheatSheetUnsupportedLanguageParameter() throws  Exception {
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.HELP_URI + "/CommonmarkCheatsheet?lang=xx");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void testGetCheatSheetUnsupportedHelpType() throws  Exception {
        MockHttpServletRequestBuilder request = get("/" + OlogResourceDescriptors.HELP_URI + "/unsupported");
        mockMvc.perform(request).andExpect(status().isNotFound());
    }
}
