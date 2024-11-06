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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LogTemplateTest {

    @Test
    public void testCreate(){
        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setSource("desc");
        logTemplate.setName("name");

        assertEquals("desc", logTemplate.getSource());
        assertEquals("name", logTemplate.getName());
    }

    @Test
    public void testEquals(){
        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("name");

        LogTemplate logTemplate2 = new LogTemplate();
        logTemplate2.setName("Name");

        assertEquals(logTemplate, logTemplate2);
    }
}
