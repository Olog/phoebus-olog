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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.Log.LogBuilder;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LogEntryGroupHelperTest {

    private static Logbook logbook1;
    private static Logbook logbook2;

    private static Tag tag1;
    private static Tag tag2;

    private static Property property1;

    @BeforeAll
    public static void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");

        tag1 = new Tag("tag1");
        tag2 = new Tag("tag2");

        property1 = new Property();
        property1.setName("prop1");
        property1.addAttributes(new Attribute("name1", "value1"));
    }

    @Test
    void testAddLogEntryGroupPorpertyNoProperties() {
        Log originalLog = LogBuilder.createLog()
                .owner("user")
                .title("original title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .withTags(Set.of(tag1, tag2))
                .description("description1")
                .createDate(Instant.now())
                .level("Urgent")
                .build();

        Property prop = LogEntryGroupHelper.getLogEntryGroupProperty(originalLog);
        assertNull(prop);
    }

    @Test
    void testAddLogEntryGroupPorpertyOriginalHasLogEntryGroup() {
        Log originalLog = LogBuilder.createLog()
                .owner("user")
                .title("original title")
                .withLogbooks(Set.of(logbook1, logbook2))
                .withTags(Set.of(tag1, tag2))
                .description("description1")
                .createDate(Instant.now())
                .level("Urgent")
                .withProperties(Set.of(property1))
                .build();

        Property logEntryGroupProperty = LogEntryGroupHelper.createNewLogEntryProperty();
        originalLog.getProperties().add(logEntryGroupProperty);

        Property prop = LogEntryGroupHelper.getLogEntryGroupProperty(originalLog);
        assertNotNull(prop);
    }
}
