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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Log.LogBuilder;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextHierarchy({@ContextConfiguration(classes = {LogEntryValidatorTestConfig.class})})
public class LogEntryValidatorTest {

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private LogEntryValidator logEntryValidator;

    private Logbook logbook1;
    private Logbook logbook2;
    private Tag tag1;
    private Tag tag2;

    private Instant now = Instant.now();

    @Before
    public void init() {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");

        tag1 = new Tag("tag1");
        tag2 = new Tag("tag2");
    }

    @Test
    public void testValidLogEntry() {
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));

        Log log = LogBuilder.createLog()
                .id(1L)
                .title("title")
                .owner("user")
                .withLogbooks(Set.of(logbook1, logbook2))
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .withTags(Set.of(tag1, tag2))
                .build();

        Errors errors = new BeanPropertyBindingResult(log, "validLog");
        logEntryValidator.validate(log, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    public void testInvalidLogbooks() {
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));
        Logbook badLogbook = new Logbook("bad", "owner");

        Log log = LogBuilder.createLog()
                .id(1L)
                .title("title")
                .owner("user")
                .withLogbooks(Set.of(badLogbook, logbook1, logbook2))
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .withTags(Set.of(tag1, tag2))
                .build();

        Errors errors = new BeanPropertyBindingResult(log, "validLog");
        logEntryValidator.validate(log, errors);

        assertEquals(1, errors.getErrorCount());
    }

    @Test
    public void testNoLogbooks() {
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));

        Log log = LogBuilder.createLog()
                .id(1L)
                .title("title")
                .owner("user")
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .withTags(Set.of(tag1, tag2))
                .build();

        Errors errors = new BeanPropertyBindingResult(log, "validLog");
        logEntryValidator.validate(log, errors);

        assertEquals(1, errors.getErrorCount());
    }

    @Test
    public void testInvalidTags() {
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));

        Tag badTag = new Tag("bad");

        Log log = LogBuilder.createLog()
                .id(1L)
                .title("title")
                .owner("user")
                .withLogbooks(Set.of(logbook1, logbook2))
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .withTags(Set.of(badTag, tag1, tag2))
                .build();

        Errors errors = new BeanPropertyBindingResult(log, "validLog");
        logEntryValidator.validate(log, errors);

        assertEquals(1, errors.getErrorCount());
    }

    @Test
    public void testInvalidTitle() {
        when(logbookRepository.findAll()).thenReturn(Arrays.asList(logbook1, logbook2));
        when(tagRepository.findAll()).thenReturn(Arrays.asList(tag1, tag2));

        Log log = LogBuilder.createLog()
                .id(1L)
                .owner("user")
                .withLogbooks(Set.of(logbook1, logbook2))
                .description("description1")
                .createDate(now)
                .level("Urgent")
                .withTags(Set.of(tag1, tag2))
                .build();

        Errors errors = new BeanPropertyBindingResult(log, "validLog");
        logEntryValidator.validate(log, errors);

        assertEquals(1, errors.getErrorCount());
    }
}
