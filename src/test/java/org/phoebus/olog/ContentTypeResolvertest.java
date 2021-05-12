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

import org.junit.Test;
import org.phoebus.olog.ContentTypeResolver;
import org.springframework.http.MediaType;

import static org.junit.Assert.*;

public class ContentTypeResolvertest {

    @Test
    public void testDetermineMediaType(){

        assertNull(ContentTypeResolver.determineMediaType(null));
        assertNull(ContentTypeResolver.determineMediaType(""));

        MediaType mediaType = ContentTypeResolver.determineMediaType("foo.jpg");
        assertEquals("image", mediaType.getType());
        assertEquals("jpeg", mediaType.getSubtype());

        mediaType = ContentTypeResolver.determineMediaType("foo.png");
        assertEquals("image", mediaType.getType());
        assertEquals("png", mediaType.getSubtype());

        mediaType = ContentTypeResolver.determineMediaType("foo.html");
        assertEquals("text", mediaType.getType());
        assertEquals("html", mediaType.getSubtype());

        mediaType = ContentTypeResolver.determineMediaType("foo.htm");
        assertEquals("text", mediaType.getType());
        assertEquals("html", mediaType.getSubtype());

        mediaType = ContentTypeResolver.determineMediaType("foo.json");
        assertEquals("application", mediaType.getType());
        assertEquals("json", mediaType.getSubtype());

        mediaType = ContentTypeResolver.determineMediaType("foo.doc");
        assertEquals("application", mediaType.getType());
        assertEquals("msword", mediaType.getSubtype());

        mediaType = ContentTypeResolver.determineMediaType("foo.docx");
        assertEquals("application", mediaType.getType());
        assertTrue(mediaType.getSubtype().contains("openxmlformats"));

        mediaType = ContentTypeResolver.determineMediaType("foo.xlsx");
        assertEquals("application", mediaType.getType());
        assertTrue(mediaType.getSubtype().contains("openxmlformats"));

        mediaType = ContentTypeResolver.determineMediaType("foo.xls");
        assertEquals("application", mediaType.getType());
        assertTrue(mediaType.getSubtype().contains("ms-excel"));

        mediaType = ContentTypeResolver.determineMediaType("foo.pdf");
        assertEquals("application", mediaType.getType());
        assertEquals("pdf", mediaType.getSubtype());
    }
}
