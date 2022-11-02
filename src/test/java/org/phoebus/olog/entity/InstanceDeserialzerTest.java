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

import com.fasterxml.jackson.core.JsonParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class InstanceDeserialzerTest {

    @Test
    public void testDeserialize() throws Exception{
        JsonParser jsonParser = Mockito.mock(JsonParser.class);
        when(jsonParser.getText()).thenReturn("1622550277.005000000");
        InstanceDeserializer instanceDeserializer = new InstanceDeserializer();

        Instant instant = instanceDeserializer.deserialize(jsonParser, null);
        assertEquals(1622550277L, instant.getEpochSecond());
        assertEquals(5000000, instant.getNano());

        when(jsonParser.getText()).thenReturn("1622550277005");
        instant = instanceDeserializer.deserialize(jsonParser, null);
        assertEquals(1622550277L, instant.getEpochSecond());
        assertEquals(5000000, instant.getNano());
    }
}
