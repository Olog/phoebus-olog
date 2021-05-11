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

package org.phoebus.olog.entity.preprocess;

import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Log.LogBuilder;
import gov.bnl.olog.entity.preprocess.DefaultPreprocessor;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultPreprocessorTest {

    private DefaultPreprocessor defaultPreprocessor = new DefaultPreprocessor();

    @Test
    public void testSourceNull(){
        Log log = LogBuilder.createLog()
                .description("description")
                .source(null)
                .build();

        log = defaultPreprocessor.process(log);
        assertEquals("description", log.getSource());
        assertEquals("description", log.getDescription());
    }
}
