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

package org.phoebus.olog.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.phoebus.olog.entity.Log.LogBuilder;

import static org.junit.Assert.*;

public class LogTest {

    @Test
    public void testLogBuilder1(){
        LogBuilder logBuilder = new LogBuilder();
        Log log = logBuilder.build();
        assertEquals(0, log.getProperties().size());
        assertEquals(0, log.getAttachments().size());
        assertEquals(0, log.getLogbooks().size());
        assertEquals(0, log.getEvents().size());
        assertNull(log.getOwner());
        assertEquals("", log.getDescription());
        assertEquals("", log.getSource());
        assertEquals("", log.getTitle());
        assertEquals("Info", log.getLevel());
    }

    @Test
    public void testLogBuilder2() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();
        Log log = objectMapper.readValue(this.getClass().getResourceAsStream("/logentry.json"), Log.class);
        Log newLog = LogBuilder.createLog(log).build();
        assertEquals(Long.valueOf(170L), newLog.getId());
        assertEquals(log.getCreatedDate(), newLog.getCreatedDate());
        assertTrue(newLog.getDescription().length() > 0);
        assertTrue(newLog.getTitle().length() > 0);
        assertTrue(newLog.getOwner().length() > 0);
        assertTrue(newLog.getLogbooks().size() > 0);
    }

    @Test
    public void testLogBuilder3() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();
        Log log = objectMapper.readValue(this.getClass().getResourceAsStream("/logentry_nocreateddate.json"), Log.class);
        Log newLog = LogBuilder.createLog(log).build();
        assertEquals(Long.valueOf(170L), newLog.getId());
        assertNotNull(newLog.getCreatedDate());
        assertTrue(newLog.getDescription().length() > 0);
        assertTrue(newLog.getTitle().length() > 0);
        assertTrue(newLog.getOwner().length() > 0);
        assertTrue(newLog.getLogbooks().size() > 0);
    }
}
