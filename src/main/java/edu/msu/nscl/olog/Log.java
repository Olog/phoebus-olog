/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package edu.msu.nscl.olog;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Log object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "id", "createdDate", "modifiedDate", "owner", "source", "description", "logbooks", "tags",
        "properties", "attachments" })
@XmlRootElement(name = "log")
public class Log implements Serializable {

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((logbooks == null) ? 0 : logbooks.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (level != other.level)
            return false;
        if (logbooks == null) {
            if (other.logbooks != null)
                return false;
        } else if (!logbooks.equals(other.logbooks))
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
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
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

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Long id;

    private Instant createdDate;
    private String owner;

    private String source;
    private String description;

    private Level level;
    private State state;

    private Set<Property> properties = new HashSet<Property>();
    private Set<Logbook> logbooks = new HashSet<Logbook>();
    private Set<Tag> tags = new HashSet<Tag>();

    // TODO add support for attachments
    private Log() {
    }

    private Log(Long id, String version, String owner, String source, String description, Level level, State state,
            Date modifiedDate, Set<Property> properties, Set<Logbook> logbooks, Set<Tag> tags) {

        super();
        this.id = id;
        this.owner = owner;
        this.source = source;
        this.description = description;
        this.level = level;
        this.state = state;

        this.properties = properties;
        this.logbooks = logbooks;
        this.tags = tags;
    }

    /**
     * @param id
     *            the id to set
     */
    void setId(Long id) {
        this.id = id;
    }

    /**
     * @param createdDate
     *            the createdDate to set
     */
    private void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @param owner
     *            the owner to set
     */
    private void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @param source
     *            the source to set
     */
    private void setSource(String source) {
        this.source = source;
    }

    /**
     * @param description
     *            the description to set
     */
    private void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param level
     *            the level to set
     */
    private void setLevel(Level level) {
        this.level = level;
    }

    /**
     * @param state
     *            the state to set
     */
    private void setState(State state) {
        this.state = state;
    }

    /**
     * @param properties
     *            the properties to set
     */
    private void setProperties(Set<Property> properties) {
        this.properties = properties;
    }

    /**
     * @param logbooks
     *            the logbooks to set
     */
    private void setLogbooks(Set<Logbook> logbooks) {
        this.logbooks = logbooks;
    }

    /**
     * @param tags
     *            the tags to set
     */
    private void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the createdDate
     */
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * @return the properties
     */
    public Set<Property> getProperties() {
        return properties;
    }

    /**
     * @return the logbooks
     */
    public Set<Logbook> getLogbooks() {
        return logbooks;
    }

    /**
     * @return the tags
     */
    public Set<Tag> getTags() {
        return tags;
    }

    /**
     * 
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
            sb.append(Logbook.toLogger(logbook));
        }
        for (Tag tag : tags) {
            sb.append(Tag.toLogger(tag));
        }
        for (Property property : properties) {
            sb.append(Property.toLogger(property));
        }
        return sb.toString();
    }

    /**
     * 
     * @author Kunal Shroff
     *
     */
    public static class LogBuilder {

        private Long id;
        private Instant createDate;

        private String owner;
        private StringBuilder source = new StringBuilder();
        private StringBuilder description = new StringBuilder();

        private Level level = Level.Info;
        private State state = State.Active;

        private Set<Property> properties = new HashSet<Property>();
        private Set<Logbook> logbooks = new HashSet<Logbook>();
        private Set<Tag> tags = new HashSet<Tag>();

        public LogBuilder() {
        }

        public LogBuilder(Log log) {
            this.id = log.getId();
            this.createDate = log.getCreatedDate();
            this.owner = log.getOwner();
            this.source = new StringBuilder(log.getSource());
            this.description = new StringBuilder(log.getDescription());
            this.level = log.getLevel();
            this.state = log.getState();

            this.properties = log.getProperties();
            this.logbooks = log.getLogbooks();
            this.tags = log.getTags();
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
                this.owner = null;
            return this;
        }

        public LogBuilder description(String description) {
            if (description != null) {
                this.description = new StringBuilder(description);
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

        public LogBuilder level(Level level) {
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

        public Log build() {
            Log log = new Log();
            log.setId(id);
            log.setOwner(owner);
            if (this.createDate != null) {
                log.setCreatedDate(createDate);
            }
            log.setDescription(this.description.toString());
            log.setSource(this.source.toString());
            log.setLevel(level);
            log.setState(state);
            log.setLogbooks(logbooks);
            log.setTags(tags);
            log.setProperties(properties);
            return log;
        }

    }
}
