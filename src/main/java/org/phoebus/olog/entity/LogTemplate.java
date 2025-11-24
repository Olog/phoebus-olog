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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;


import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A class representing a template for a log entry. Structure similar to {@link Log},
 * but stripped of fields that do not make sense in a template.
 */
public class LogTemplate{

    @NotNull
    @Size(min = 1, message = "A name must be specified.")
    private String name;

    @Id
    private String id;

    private String owner;
    private String source;

    @NotNull
    @Size(min = 1, message = "A title must be specified.")
    private String title;

    private String level = "Info";

    @JsonSerialize(using = InstanceSerializer.class)
    @JsonDeserialize(using = InstanceDeserializer.class)
    private Instant createdDate;
    @JsonSerialize(using = InstanceSerializer.class)
    @JsonDeserialize(using = InstanceDeserializer.class)
    private Instant modifyDate;

    @NotNull
    @Size(min = 1, message = "At least one logbook must be specified.")
    private Set<Logbook> logbooks = new HashSet<>();
    private Set<Tag> tags = new HashSet<>();
    private Set<Property> properties = new HashSet<>();

    public @NotNull @Size(min = 1, message = "A name must be specified.") String getName() {
        return name;
    }

    public void setName(@NotNull @Size(min = 1, message = "A name must be specified.") String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public @NotNull @Size(min = 1, message = "A title must be specified.") String getTitle() {
        return title;
    }

    public void setTitle(@NotNull @Size(min = 1, message = "A title must be specified.") String title) {
        this.title = title;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getCreatedDate(){
        return createdDate;
    }

    public Instant getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Instant modifyDate) {
        this.modifyDate = modifyDate;
    }

    public @NotNull @Size(min = 1, message = "At least one logbook must be specified.") Set<Logbook> getLogbooks() {
        return logbooks;
    }

    public void setLogbooks(@NotNull @Size(min = 1, message = "At least one logbook must be specified.") Set<Logbook> logbooks) {
        this.logbooks = logbooks;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Set<Property> getProperties() {
        return properties;
    }

    public void setProperties(Set<Property> properties) {
        this.properties = properties;
    }

    /**
     * Equality based on lower case transform of the {@link #name} field.
     * @param other Object to compare to
     * @return <code>true</code> only if lower case {@link #name} fields are equal
     */
    @Override
    public boolean equals(Object other){
        if(!(other instanceof LogTemplate)){
            return false;
        }
        return ((LogTemplate)other).getName().equalsIgnoreCase(name);
    }

    @Override
    public int hashCode(){
        return name.toLowerCase().hashCode();
    }
}
