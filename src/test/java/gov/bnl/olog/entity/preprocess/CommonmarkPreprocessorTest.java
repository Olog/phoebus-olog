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

package gov.bnl.olog.entity.preprocess;

import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Log.LogBuilder;
import gov.bnl.olog.entity.preprocess.CommonmarkPreprocessor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommonmarkPreprocessorTest {

    private CommonmarkPreprocessor commonmarkPreprocessor = new CommonmarkPreprocessor();

    @Test
    public void testDescriptionNonNull(){
        Log log = LogBuilder.createLog()
                .description("**BOLD** ![alt](http://foo.bar)")
                .source(null)
                .build();

        log = commonmarkPreprocessor.process(log);
        assertEquals("**BOLD** ![alt](http://foo.bar)", log.getSource());
        assertEquals("BOLD \"alt\" (http://foo.bar)", log.getDescription());

    }

    @Test
    public void testDescriptionNull(){
        Log log = LogBuilder.createLog()
                .description(null)
                .source(null)
                .build();

        log = commonmarkPreprocessor.process(log);
        assertEquals("", log.getSource());
        assertEquals("", log.getDescription());

    }
}
