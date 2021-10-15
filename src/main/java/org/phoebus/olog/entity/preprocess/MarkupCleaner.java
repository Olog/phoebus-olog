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
 * A pre-processor interface intended to ensure that the <code>description</code> field of
 * a {@link Log} object is free of markup as a text search on the description should not match
 * potential markup text.
 *
 * Implementations added through SPI should make sure that {@link #getName()} returns a string unique
 * among other implementations. The Olog service adds two implementations: "none" and "commonmark", see
 * {@link org.phoebus.olog.entity.preprocess.impl.DefaultMarkupCleaner} and
 * {@link org.phoebus.olog.entity.preprocess.impl.CommonmarkCleaner}.
 *
 * See also application.properties, which defines the default markup scheme, i.e. the wanted markup scheme
 * if client does not specify one explicitly.
 */
public interface MarkupCleaner {

    /**
     * Processes the log entry and returns a processed value where the <code>description</code> field
     * is free of markup.
     * @param log
     * @return
     */
    Log process(Log log);

    /**
     * @return A name unique among other {@link MarkupCleaner}s
     */
    default String getName(){
        return null;
    }
}
