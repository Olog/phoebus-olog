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

import org.phoebus.olog.LogbookRepository;
import org.phoebus.olog.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom validator for a {@link Log} object.
 */
public class LogEntryValidator implements ConstraintValidator<ValidLog, Log> {

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    private TagRepository tagRepository;

    private final Logger logger = Logger.getLogger(LogEntryValidator.class.getName());

    /**
     * Checks that the {@link Log}'s logbooks and tags are valid, i.e. that they exist in Elastic.
     * @param log The {@link Log} entry to check.
     * @param context Validation context
     * @return <code>true</code> if the log entry is considered valid, otherwise <code>false</code>
     */
    public boolean isValid(Log log, ConstraintValidatorContext context) {
        List<String> existingLogbookNames = new ArrayList<>();
        logbookRepository.findAll().forEach(l -> existingLogbookNames.add(l.getName()));
        for(Logbook logbook : log.getLogbooks()){
            if(!existingLogbookNames.contains(logbook.getName())){
                logger.log(Level.INFO, "Logbook '" + logbook.getName() + "' is invalid.");
                return false;
            }
        }

        List<String> existingTagNames = new ArrayList<>();
        tagRepository.findAll().forEach(t -> existingTagNames.add(t.getName()));
        for(Tag tag : log.getTags()){
            if(!existingTagNames.contains(tag.getName())){
                logger.log(Level.INFO, "Tag '" + tag.getName() + "' is invalid.");
                return false;
            }
        }
        return true;
    }
}
