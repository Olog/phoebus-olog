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

import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom validator for a {@link Log} object.
 */
public class LogEntryValidator implements Validator {

    @Autowired
    @SuppressWarnings("unused")
    private LogbookRepository logbookRepository;

    @Autowired
    @SuppressWarnings("unused")
    private TagRepository tagRepository;

    private final Logger logger = Logger.getLogger(LogEntryValidator.class.getName());

    @Override
    public boolean supports(Class clazz) {
        return Log.class.equals(clazz);
    }

    @Override
    public void validate(Object object, Errors errors){
        Log log = (Log)object;

        if(log.getTitle() == null || log.getTitle().isEmpty()){
            logger.log(Level.INFO, TextUtil.LOG_NOT_TITLE);
            errors.rejectValue("logbooks", "no.title");
        }

        List<String> existingLogbookNames = new ArrayList<>();
        logbookRepository.findAll().forEach(l -> existingLogbookNames.add(l.getName()));

        Set<Logbook> logbooks = log.getLogbooks();
        if(logbooks.isEmpty()){
            logger.log(Level.INFO, TextUtil.LOGBOOKS_NOT_SPECIFIED);
            errors.rejectValue("logbooks", "no.logbooks");
        }

        for(Logbook logbook : log.getLogbooks()){
            if(!existingLogbookNames.contains(logbook.getName())){
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.LOGBOOK_INVALID, logbook.getName()));
                errors.rejectValue("logbooks", "invalid.logbooks");
            }
        }

        List<String> existingTagNames = new ArrayList<>();
        tagRepository.findAll().forEach(t -> existingTagNames.add(t.getName()));
        for(Tag tag : log.getTags()){
            if(!existingTagNames.contains(tag.getName())){
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.TAG_INVALID, tag.getName()));
                errors.rejectValue("tags", "invalid.tags");
            }
        }
    }
}
