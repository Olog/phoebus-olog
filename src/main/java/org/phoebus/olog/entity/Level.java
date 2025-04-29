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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a "Level" value. Sites may choose another name for this entity, e.g. "Entry Type".
 * In the underlying repository only one object is allowed to be the default level.
 * @param name Unique, case-sensitive name
 * @param defaultLevel Specifies if this is the default level. Clients can use this to
 *                     avoid active selection.
 */
public record Level(String name, boolean defaultLevel) {
}
