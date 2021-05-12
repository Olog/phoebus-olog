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

import org.phoebus.olog.entity.Log;

/**
 * Default implementation of {@link LogPreprocessor}.
 */
public class DefaultPreprocessor implements LogPreprocessor{

    /**
     * Processes the log entry under the assumption that the source field of a {@link Log} object
     * as posted by client is always null, i.e. client does not set the field. Consequently this
     * method copies the description field to the source field.
     * @param log
     * @return The processed log record.
     */
    @Override
    public Log process(Log log){
        log.setSource(log.getDescription());
        return log;
    }
}
