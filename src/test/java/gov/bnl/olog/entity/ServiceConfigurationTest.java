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

package gov.bnl.olog.entity;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ServiceConfigurationTest {

    @Test
    public void testBuilder(){
        ServiceConfiguration serviceConfiguration = ServiceConfiguration.builder().build();
        assertNull(serviceConfiguration.getLevels());
        assertNull(serviceConfiguration.getTags());
        assertNull(serviceConfiguration.getLogbooks());
    }

    @Test
    public void testNoArgsConstructor(){
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        assertNull(serviceConfiguration.getLevels());
        assertNull(serviceConfiguration.getTags());
        assertNull(serviceConfiguration.getLogbooks());
    }

    @Test
    public void testAllArgsConstructor(){
        ServiceConfiguration serviceConfiguration =
                new ServiceConfiguration(Arrays.asList(new Logbook("name", "owner")),
                        Arrays.asList(new Tag("name")),
                        Arrays.asList("A"));
        assertNotNull(serviceConfiguration.getLevels());
        assertNotNull(serviceConfiguration.getTags());
        assertNotNull(serviceConfiguration.getLogbooks());
    }
}
