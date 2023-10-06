/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Log object that can be represented as JSON in payload data.
 *
 * @author Kunal Shroff
 */
public class Log implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    private String owner;

    private String source;
    private String description;

    @NotNull
    @Size(min = 1, message = "A title must be specified.")
    private String title;

    private String level = "Info";
    private State state = State.Active;

    @JsonSerialize(using = InstanceSerializer.class)
    @JsonDeserialize(using = InstanceDeserializer.class)
    private Instant createdDate;
    @JsonSerialize(using = InstanceSerializer.class)
    @JsonDeserialize(using = InstanceDeserializer.class)
    private Instant modifyDate;

    private List<Event> events;

    @NotNull
    @Size(min = 1, message = "At least one logbook must be specified.")
    private Set<Logbook> logbooks = new HashSet<>();
    private Set<Tag> tags = new HashSet<>();
    private Set<Property> properties = new HashSet<>();

    private Set<Attachment> attachments = new HashSet<>();

    private Log() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Getter for log id
     *
     * @return the log id
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for log id
     *
     * @param id - the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for log owner
     *
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for log owner
     *
     * @param owner - the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Getter for log source
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Setter for log source
     *
     * @param source - the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Getter for log description
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter for log description
     *
     * @param description - the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter for log level
     *
     * @return the level
     */
    public String getLevel() {
        return level;
    }

    /**
     * Setter for log level
     *
     * @param level - the level to set
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Getter for log state
     *
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * Setter for log state
     *
     * @param state - the state to set
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Getter for log create date
     *
     * @return the createdDate
     */
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * Setter for log createdDate
     *
     * @param createdDate - the createdDate to set
     */
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Getter for log modify date
     *
     * @return the modifyDate
     */
    public Instant getModifyDate() {
        return modifyDate;
    }

    /**
     * Setter for log modifyDate
     *
     * @param modifyDate - the modifyDate to set
     */
    public void setModifyDate(Instant modifyDate) {
        this.modifyDate = modifyDate;
    }

    /**
     * Getter for log events
     *
     * @return the events
     */
    public List<Event> getEvents() {
        return events;
    }

    /**
     * Setter for log events
     *
     * @param events - the events to set
     */
    public void setEvents(List<Event> events) {
        this.events = events;
    }

    /**
     * Getter for log logbooks
     *
     * @return the logbooks
     */
    public Set<Logbook> getLogbooks() {
        return logbooks;
    }

    /**
     * Setter for log logbooks
     *
     * @param logbooks - the logbooks to set
     */
    public void setLogbooks(Set<Logbook> logbooks) {
        this.logbooks = logbooks;
    }

    /**
     * Getter for log tags
     *
     * @return the tags
     */
    public Set<Tag> getTags() {
        return tags;
    }

    /**
     * Setter for log tags
     *
     * @param tags - the tags to set
     */
    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    /**
     * Getter for log properties
     *
     * @return the properties
     */
    public Set<Property> getProperties() {
        return properties;
    }

    /**
     * Setter for log properties
     *
     * @param properties - the properties to set
     */
    public void setProperties(Set<Property> properties) {
        this.properties = properties;
    }

    /**
     * Getter for log attachments
     *
     * @return the attachments
     */
    public Set<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Setter for log attachments
     *
     * @param attachments - the attachments to set
     */
    public void setAttachments(Set<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    /**
     * @return A string representation of the log entry
     */
    public String toLogger() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getId());
        sb.append(":");
        sb.append(this.getOwner());
        sb.append(this.getDescription());
        sb.append("(");
        sb.append(this.getLevel());
        sb.append(this.getState());
        sb.append(")");
        for (Logbook logbook : logbooks) {
            sb.append(logbook.toLogger());
        }
        for (Tag tag : tags) {
            sb.append(tag.toLogger());
        }
        for (Property property : properties) {
            sb.append(property.toLogger());
        }
        return sb.toString();
    }

    /**
     * A builder for creating a {@link Log} entry
     *
     * @author Kunal Shroff
     */
    @JsonIgnoreType
    public static class LogBuilder {

        private Long id;
        private Instant createDate;
        private Instant modifyDate;
        private List<Event> events = new ArrayList<>();

        private String owner;
        private StringBuilder source = new StringBuilder();
        private StringBuilder description = new StringBuilder();
        private StringBuilder title = new StringBuilder();

        private String level = "Info";
        private State state = State.Active;

        private Set<Property> properties = new HashSet<>();
        private Set<Logbook> logbooks = new HashSet<>();
        private Set<Tag> tags = new HashSet<>();
        private Set<Attachment> attachments = new HashSet<>();

        public LogBuilder() {
        }

        /**
         * Create a logbuilder initialized based on the given {@link Log}
         *
         * @param log - the log to be used to initialize the {@link LogBuilder}
         */
        LogBuilder(Log log) {
            this.id = log.getId();
            this.createDate = log.getCreatedDate();
            this.modifyDate = log.getModifyDate();
            this.owner = log.getOwner();
            if (log.getSource() != null) {
                this.source = new StringBuilder(log.getSource());
            } else {
                this.source = new StringBuilder();
            }
            this.description = new StringBuilder(log.getDescription());
            this.title = new StringBuilder(log.getTitle());
            this.level = log.getLevel();
            this.state = log.getState();

            this.events = log.getEvents();

            this.properties = log.getProperties();
            this.logbooks = log.getLogbooks();
            this.tags = log.getTags();

            this.attachments = log.getAttachments();
        }

        public LogBuilder(String source) {
            this.source = new StringBuilder(source);
        }

        public static LogBuilder createLog() {
            return new LogBuilder();
        }

        public static LogBuilder createLog(String source) {
            return new LogBuilder(source);
        }

        public static LogBuilder createLog(Log log) {
            return new LogBuilder(log);
        }

        public LogBuilder owner(String owner) {
            if (owner != null)
                this.owner = owner;
            else if (owner == null)
                this.owner = "";
            return this;
        }

        public LogBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LogBuilder createDate(Instant createDate) {
            this.createDate = createDate;
            return this;
        }

        public LogBuilder withEvents(List<Event> events) {
            this.events = events;
            return this;
        }

        public LogBuilder description(String description) {
            if (description != null) {
                this.description = new StringBuilder(description);
            }
            return this;
        }

        public LogBuilder title(String title) {
            if (title != null) {
                this.title = new StringBuilder(title);
            }
            return this;
        }

        public LogBuilder appendDescription(String description) {
            if (this.description.length() > 0) {
                this.description.append("\n");
            }
            this.description.append(description);
            return this;
        }

        public LogBuilder source(String source) {
            if (source != null) {
                this.source = new StringBuilder(source);
            }
            return this;
        }

        public LogBuilder appendSource(String source) {
            if (this.source.length() > 0) {
                this.source.append("\n");
            }
            this.source.append(source);
            return this;
        }

        public LogBuilder level(String level) {
            this.level = level;
            return this;
        }

        public LogBuilder withLogbook(Logbook logbook) {
            this.logbooks.add(logbook);
            return this;
        }

        public LogBuilder withLogbooks(Set<Logbook> logbooks) {
            this.logbooks.addAll(logbooks);
            return this;
        }

        public LogBuilder setLogbooks(Set<Logbook> logbooks) {
            this.logbooks = logbooks;
            return this;
        }

        public LogBuilder withTag(Tag tag) {
            this.tags.add(tag);
            return this;
        }

        public LogBuilder withTags(Set<Tag> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public LogBuilder setTags(Set<Tag> tags) {
            this.tags = tags;
            return this;
        }

        public LogBuilder withProperty(Property property) {
            this.properties.add(property);
            return this;
        }

        public LogBuilder withProperties(Set<Property> properties) {
            this.properties.addAll(properties);
            return this;
        }

        public LogBuilder setProperties(Set<Property> properties) {
            this.properties = properties;
            return this;
        }

        public LogBuilder withAttachment(Attachment attachment) {
            this.attachments.add(attachment);
            return this;
        }

        public LogBuilder setAttachments(Set<Attachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public LogBuilder modifyDate(Instant modifyDate) {
            this.modifyDate = modifyDate;
            return this;
        }

        /**
         * Create the log entry
         *
         * @return an instance of {@link Log}
         */
        public Log build() {
            Log log = new Log();
            log.setId(id);
            log.setOwner(owner);
            if (this.createDate != null) {
                log.setCreatedDate(createDate);
            }
            if (this.modifyDate != null) {
                log.setModifyDate(modifyDate);
            }
            log.setEvents(events);
            log.setDescription(this.description.toString());
            log.setTitle(this.title.toString());
            log.setSource(this.source.toString());
            log.setLevel(level);
            log.setState(state);
            log.setLogbooks(logbooks);
            log.setTags(tags);
            log.setProperties(properties);
            log.setAttachments(attachments);
            return log;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((events == null) ? 0 : events.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((logbooks == null) ? 0 : logbooks.hashCode());
        result = prime * result + ((modifyDate == null) ? 0 : modifyDate.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Log other = (Log) obj;
        if (createdDate == null) {
            if (other.createdDate != null)
                return false;
        } else if (!createdDate.equals(other.createdDate))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (events == null) {
            if (other.events != null)
                return false;
        } else if (!events.equals(other.events))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (!level.equals(other.level))
            return false;
        if (logbooks == null) {
            if (other.logbooks != null)
                return false;
        } else if (!logbooks.equals(other.logbooks))
            return false;
        if (modifyDate == null) {
            if (other.modifyDate != null)
                return false;
        } else if (!modifyDate.equals(other.modifyDate))
            return false;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (state != other.state)
            return false;
        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        return true;
    }

}
