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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Helper class when dealing with replies to log entries.
 */
public class LogEntryGroupHelper {

    public static final String LOG_ENTRY_GROUP = "Log Entry Group";
    public static final String ATTRIBUTE_ID = "id";

    /**
     * @param originalLogEntry The log entry to which a user wishes to reply.
     * @return An {@link Optional} that is empty if the original log entry does not already contain
     * a log entry group property.
     */
    public static Property getLogEntryGroupProperty(Log originalLogEntry){
        // Check if the original log entry already contains the log entry group property
        Set<Property> originalLogEntryProperties = originalLogEntry.getProperties();
        Optional<Property> prop = null;
        if(originalLogEntryProperties != null){
            prop =
                originalLogEntryProperties.stream().filter(property -> property.getName().equals(LOG_ENTRY_GROUP)).findFirst();
        }
        return prop.isPresent() ? prop.get() : null;
    }

    /**
     * @return A {@link Property} containing two {@link Attribute}s: one with the unique log entry group id,
     * one with the title of the original entry.
     */
    public static Property createNewLogEntryProperty(){
        Attribute idAttribute = new Attribute(ATTRIBUTE_ID, UUID.randomUUID().toString());
        return new Property(LOG_ENTRY_GROUP, Set.of(idAttribute));
    }
}
