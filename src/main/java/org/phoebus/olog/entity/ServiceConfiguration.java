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

import java.util.List;

public class ServiceConfiguration {

    private Iterable<Logbook> logbooks;
    private Iterable<Tag> tags;
    private List<String> levels;

    public Iterable<Logbook> getLogbooks() {
        return logbooks;
    }

    public void setLogbooks(Iterable<Logbook> logbooks) {
        this.logbooks = logbooks;
    }

    public Iterable<Tag> getTags() {
        return tags;
    }

    public void setTags(Iterable<Tag> tags) {
        this.tags = tags;
    }

    public List<String> getLevels() {
        return levels;
    }

    public void setLevels(List<String> levels) {
        this.levels = levels;
    }
}
