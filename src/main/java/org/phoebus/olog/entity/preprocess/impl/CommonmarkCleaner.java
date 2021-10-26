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

package org.phoebus.olog.entity.preprocess.impl;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.preprocess.MarkupCleaner;

public class CommonmarkCleaner implements MarkupCleaner {

    private TextContentRenderer textContentRenderer = TextContentRenderer.builder().build();
    private Parser parser = Parser.builder().build();

    /**
     * Processes the log entry under the assumption that the source field of a {@link Log} object
     * as posted by client can be overwritten, if specified. This method treats the
     * description field as a Commonmark source and copies it to the source field. Then the same
     * string is processed to set the description field to a "plain text" variant of the Commonmark source.
     * @param log The {@link Log} entry to clean of markup.
     * @return The processed log entry.
     */
    @Override
    public Log process(Log log){
        if(log.getDescription() != null){ // Should not be null, but clients cannot be trusted.
            log.setSource(log.getDescription());
            Node document = parser.parse(log.getDescription());
            String plainText = textContentRenderer.render(document);
            log.setDescription(plainText);
            return log;
        }
        return log;
    }

    @Override
    public String getName(){
        return "commonmark";
    }
}
