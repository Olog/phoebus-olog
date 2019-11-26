/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package gov.bnl.olog.entity;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_TYPE;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Log object that can be represented as XML/JSON in payload data.
 */
@Document(indexName = ES_LOG_INDEX, type = ES_LOG_TYPE)
public class Log implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    private String owner;

    private String source;
    private String description;

    private Level level;
    private State state;
    
    @JsonSerialize(using = InstanceSerializer.class)
    @JsonDeserialize(using = InstanceDeserializer.class)
    private Instant createdDate;
    @JsonSerialize(using = InstanceSerializer.class)
    @JsonDeserialize(using = InstanceDeserializer.class)
    private Instant modifyDate;
    
    @Field(type = FieldType.Nested, includeInParent = true)
    private List<Event> events;

    @Field(type = FieldType.Nested, includeInParent = true)
    private Set<Logbook> logbooks = new HashSet<Logbook>();
    @Field(type = FieldType.Nested, includeInParent = true)
    private Set<Tag> tags = new HashSet<Tag>();
    @Field(type = FieldType.Nested, includeInParent = true)
    private Set<Property> properties = new HashSet<Property>();

    @Field(type = FieldType.Nested, includeInParent = true)
    private Set<Attachment> attachments = new HashSet<Attachment>();

    private Log()
    {
    }

    private Log(Long id,
                String version,
                String owner,
                String source,
                String description,
                Level level,
                State state,
                List<Event> events,
                Set<Logbook> logbooks,
                Set<Tag> tags,
                Set<Property> properties)
    {

        super();
        this.id = id;
        this.owner = owner;
        this.source = source;
        this.description = description;
        this.level = level;
        this.state = state;

        this.events = events;

        this.logbooks = logbooks;
        this.tags = tags;
        this.properties = properties;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Level getLevel()
    {
        return level;
    }

    public void setLevel(Level level)
    {
        this.level = level;
    }

    public State getState()
    {
        return state;
    }

    public void setState(State state)
    {
        this.state = state;
    }

    public Instant getCreatedDate()
    {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate)
    {
        this.createdDate = createdDate;
    }

    public Instant getModifyDate()
    {
        return modifyDate;
    }

    public void setModifyDate(Instant modifyDate)
    {
        this.modifyDate = modifyDate;
    }

    public List<Event> getEvents()
    {
        return events;
    }

    public void setEvents(List<Event> events)
    {
        this.events = events;
    }

    public Set<Logbook> getLogbooks()
    {
        return logbooks;
    }

    public void setLogbooks(Set<Logbook> logbooks)
    {
        this.logbooks = logbooks;
    }

    public Set<Tag> getTags()
    {
        return tags;
    }

    public void setTags(Set<Tag> tags)
    {
        this.tags = tags;
    }

    public Set<Property> getProperties()
    {
        return properties;
    }

    public void setProperties(Set<Property> properties)
    {
        this.properties = properties;
    }

    public Set<Attachment> getAttachments()
    {
        return attachments;
    }

    public void setAttachments(Set<Attachment> attachments)
    {
        this.attachments = attachments;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid()
    {
        return serialVersionUID;
    }

    /**
     * 
     * @return A string representation of the log entry
     */
    public String toLogger()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getId());
        sb.append(":");
        sb.append(this.getOwner());
        sb.append(this.getDescription());
        sb.append("(");
        sb.append(this.getLevel());
        sb.append(this.getState());
        sb.append(")");
        for (Logbook logbook : logbooks)
        {
            sb.append(logbook.toLogger());
        }
        for (Tag tag : tags)
        {
            sb.append(tag.toLogger());
        }
        for (Property property : properties)
        {
            sb.append(property.toLogger());
        }
        return sb.toString();
    }

    /**
     * 
     * @author Kunal Shroff
     *
     */
    @JsonIgnoreType
    public static class LogBuilder
    {

        private Long id;
        private Instant createDate;
        private List<Event> events = new ArrayList<Event>();

        private String owner;
        private StringBuilder source = new StringBuilder();
        private StringBuilder description = new StringBuilder();

        private Level level = Level.Info;
        private State state = State.Active;

        private Set<Property> properties = new HashSet<Property>();
        private Set<Logbook> logbooks = new HashSet<Logbook>();
        private Set<Tag> tags = new HashSet<Tag>();
        private Set<Attachment> attachments = new HashSet<Attachment>();

        public LogBuilder()
        {
        }

        public LogBuilder(Log log)
        {
            this.id = log.getId();
            this.createDate = log.getCreatedDate();
            this.owner = log.getOwner();
            this.source = new StringBuilder(log.getSource());
            this.description = new StringBuilder(log.getDescription());
            this.level = log.getLevel();
            this.state = log.getState();

            this.events = log.getEvents();

            this.properties = log.getProperties();
            this.logbooks = log.getLogbooks();
            this.tags = log.getTags();

            this.attachments = log.getAttachments();
        }

        public LogBuilder(String source)
        {
            this.source = new StringBuilder(source);
        }

        public static LogBuilder createLog()
        {
            return new LogBuilder();
        }

        public static LogBuilder createLog(String source)
        {
            return new LogBuilder(source);
        }

        public static LogBuilder createLog(Log log)
        {
            return new LogBuilder(log);
        }

        public LogBuilder owner(String owner)
        {
            if (owner != null)
                this.owner = owner;
            else if (owner == null)
                this.owner = "";
            return this;
        }

        public LogBuilder id(Long id)
        {
            this.id = id;
            return this;
        }

        public LogBuilder createDate(Instant createDate)
        {
            this.createDate = createDate;
            return this;
        }

        public LogBuilder withEvents(List<Event> events)
        {
            this.events = events;
            return this;
        }
       
        public LogBuilder description(String description)
        {
            if (description != null)
            {
                this.description = new StringBuilder(description);
            }
            return this;
        }

        public LogBuilder appendDescription(String description)
        {
            if (this.description.length() > 0)
            {
                this.description.append("\n");
            }
            this.description.append(description);
            return this;
        }

        public LogBuilder source(String source)
        {
            if (source != null)
            {
                this.source = new StringBuilder(source);
            }
            return this;
        }

        public LogBuilder appendSource(String source)
        {
            if (this.source.length() > 0)
            {
                this.source.append("\n");
            }
            this.source.append(source);
            return this;
        }

        public LogBuilder level(Level level)
        {
            this.level = level;
            return this;
        }

        public LogBuilder withLogbook(Logbook logbook)
        {
            this.logbooks.add(logbook);
            return this;
        }

        public LogBuilder withLogbooks(Set<Logbook> logbooks)
        {
            this.logbooks.addAll(logbooks);
            return this;
        }

        public LogBuilder setLogbooks(Set<Logbook> logbooks)
        {
            this.logbooks = logbooks;
            return this;
        }

        public LogBuilder withTag(Tag tag)
        {
            this.tags.add(tag);
            return this;
        }

        public LogBuilder withTags(Set<Tag> tags)
        {
            this.tags.addAll(tags);
            return this;
        }

        public LogBuilder setTags(Set<Tag> tags)
        {
            this.tags = tags;
            return this;
        }

        public LogBuilder withProperty(Property property)
        {
            this.properties.add(property);
            return this;
        }

        public LogBuilder withProperties(Set<Property> properties)
        {
            this.properties.addAll(properties);
            return this;
        }

        public LogBuilder setProperties(Set<Property> properties)
        {
            this.properties = properties;
            return this;
        }

        public LogBuilder withAttachment(Attachment attachment)
        {
            this.attachments.add(attachment);
            return this;
        }

        public LogBuilder setAttachments(Set<Attachment> attachments)
        {
            this.attachments = attachments;
            return this;
        }

        public Log build()
        {
            Log log = new Log();
            log.setId(id);
            log.setOwner(owner);
            if (this.createDate != null)
            {
                log.setCreatedDate(createDate);
            }
            log.setEvents(events);
            log.setDescription(this.description.toString());
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
    public int hashCode()
    {
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
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Log other = (Log) obj;
        if (createdDate == null)
        {
            if (other.createdDate != null)
                return false;
        } else if (!createdDate.equals(other.createdDate))
            return false;
        if (description == null)
        {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (events == null)
        {
            if (other.events != null)
                return false;
        } else if (!events.equals(other.events))
            return false;
        if (id == null)
        {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (level != other.level)
            return false;
        if (logbooks == null)
        {
            if (other.logbooks != null)
                return false;
        } else if (!logbooks.equals(other.logbooks))
            return false;
        if (modifyDate == null)
        {
            if (other.modifyDate != null)
                return false;
        } else if (!modifyDate.equals(other.modifyDate))
            return false;
        if (owner == null)
        {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        if (properties == null)
        {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (state != other.state)
            return false;
        if (tags == null)
        {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        return true;
    }

}
