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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PropertyTest {

    @Test
    void testEquals() {
        Property p1 = new Property();
        Property p2 = new Property();
        assertEquals(p1, p2);

        p1 = new Property("a");
        p2 = new Property("a");
        assertEquals(p1, p2);

        p2 = new Property("b");
        assertNotEquals(p1, p2);

        p2 = new Property("a");
        p2.setAttributes(Set.of(new Attribute("a1", "")));
        assertNotEquals(p1, p2);

        p1.setAttributes(Set.of(new Attribute("a1", "")));
        assertEquals(p1, p2);

        p1.setAttributes(Set.of(new Attribute("a2", "")));
        assertNotEquals(p1, p2);
    }

    @Test
    void testHashCode() {
        Property p1 = new Property();
        Property p2 = new Property();
        assertEquals(p1.hashCode(), p2.hashCode());

        p1 = new Property("a");
        p2 = new Property("a");
        assertEquals(p1.hashCode(), p2.hashCode());

        p2 = new Property("b");
        assertNotEquals(p1.hashCode(), p2.hashCode());

        p2 = new Property("a");
        p2.setAttributes(Set.of(new Attribute("a1", "")));
        assertNotEquals(p1.hashCode(), p2.hashCode());

        p1.setAttributes(Set.of(new Attribute("a1", "")));
        assertEquals(p1.hashCode(), p2.hashCode());

        p1.setAttributes(Set.of(new Attribute("a2", "")));
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }
}
